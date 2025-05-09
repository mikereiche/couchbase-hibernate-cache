/*
 * Copyright (c) 2025 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.client.cache;

import com.couchbase.client.core.env.PropertyLoader;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.json.JsonObject;

import javax.cache.Cache;
import javax.cache.configuration.Configuration;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CouchbaseCacheManager implements javax.cache.CacheManager {

  private final URI uri;
  private final ClassLoader classLoader;
  private final Properties properties;
  private final Map<Object,Collection> collections = new HashMap<>();
  private final Map<String, Cache> caches = new HashMap<>();
  private CachingProvider provider;

  public CouchbaseCacheManager(URI uri, ClassLoader classLoader, Properties properties) {
    this.uri = uri != null ? uri : CouchbaseCachingProvider.defaultURI();
    this.classLoader = classLoader != null ? classLoader : CouchbaseCachingProvider.class.getClassLoader();
    this.properties = properties != null ? properties : CouchbaseCachingProvider.defaultProps();

  }

  @Override
  public CachingProvider getCachingProvider() {
    return provider;
  }

  @Override
  public URI getURI() {
    return uri;
  }

  @Override
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  @Override
  public Properties getProperties() {
    return properties;
  }

  @Override
  public <KEY_CLASS, VALUE_CLASS, CONFIGURATION  extends Configuration<KEY_CLASS, VALUE_CLASS>>
  Cache<KEY_CLASS, VALUE_CLASS> createCache(String name, CONFIGURATION configuration) throws IllegalArgumentException {
    Collection collection = collections.get(configuration);
    if(collection == null){
      ClusterEnvironment.Builder builder = ClusterEnvironment.builder();
      Properties p = new Properties();
      p.putAll(properties);
      String username = p.getProperty("username");
      p.remove("username");
      char[] password = p.getProperty("password").toCharArray();
      p.remove("password");
      String bucket = p.getProperty("bucket");
      p.remove("bucket");
      // BuilderPropertySetter
      PropertyLoader loader = PropertyLoader.fromMap((Map)p);
      ClusterEnvironment env = builder.load(loader).build();
      Cluster cluster = Cluster.connect(uri.toString(),  ClusterOptions.clusterOptions(username, String.valueOf(password)).environment(env));
      collection = cluster.bucket(bucket).defaultCollection();
    }
    return new CouchbaseCache<>(this, name, collection , (CouchbaseCacheConfig)configuration);
  }

  @Override
  public <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyClass, Class<V> valueClass) {
    Cache cache = getCache(cacheName);
    Configuration config = cache.getConfiguration(Configuration.class);
    if(config.getKeyType() == keyClass && config.getValueType() == valueClass) {
      return cache;
    } else {
      return null; // or make a new cache??
    }
  }

  @Override
  public <K, V> Cache<K, V> getCache(String cacheName) {
    return caches.get(cacheName);
  }

  @Override
  public Iterable<String> getCacheNames() {
    return caches.keySet();
  }

  @Override
  public void destroyCache(String cacheName) {
    // TODO
  }

  @Override
  public void enableManagement(String s, boolean b) {
    // TODO
  }

  @Override
  public void enableStatistics(String s, boolean b) {
    // TODO
  }

  @Override
  public void close() {
    // TODO
  }

  @Override
  public boolean isClosed() {
    // TODO
    return false;
  }

  @Override
  public <T> T unwrap(Class<T> aClass) {
    //  TODO
    return null;
  }

  public void provider(CachingProvider provider) {
    this.provider = provider;
  }

  @Override
  public String toString() {
    JsonObject jo = JsonObject.jo();
    jo.put(this.getClass().getSimpleName(),
      JsonObject.jo()
        .put("uri", uri.toString())
        .put("classLoader", classLoader.toString())
        .put("properties", properties)
        .put(CouchbaseCachingProvider.class.getSimpleName(), ((CouchbaseCachingProvider)provider).toJson().get(CouchbaseCachingProvider.class.getSimpleName())));
    return jo.toString();
  }

}
