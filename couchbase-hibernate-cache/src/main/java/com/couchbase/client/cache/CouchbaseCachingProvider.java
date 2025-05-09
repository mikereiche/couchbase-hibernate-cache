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

import com.couchbase.client.java.json.JsonObject;

import javax.cache.CacheManager;
import javax.cache.configuration.OptionalFeature;
import javax.cache.spi.CachingProvider;
import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class CouchbaseCachingProvider implements javax.cache.spi.CachingProvider {

  private final URI uri;
  private final ClassLoader classLoader;
  private final Properties properties;
  private final List<CacheManager> managers = new LinkedList<>();

   public CouchbaseCachingProvider(URI uri, ClassLoader classLoader, Properties properties) {
     this.uri = uri != null ? uri : defaultURI();
     this.classLoader = classLoader != null ? classLoader : CouchbaseCachingProvider.class.getClassLoader();
     this.properties = properties != null ? properties : defaultProps();
   }

  public CouchbaseCachingProvider() {
     this(null,null,null);
  }

  static URI defaultURI() {
    try {
      return new URI("couchbases://localhost");
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  static Properties defaultProps(){
    Properties props = new Properties();
    // username/password.  These are removed before properties is fed into builder.load(props).
    props.put("username", "Administrator");
    props.put("password", "password");
    props.put("bucket", "cache");
    // remainder are fed into BuilderPropertySetter
    props.put("security.enableTls", "true");
    props.put("io.numKvConnections", "1");
    props.put("timeout.kvTimeout", "2.5s");
    return props;
  }

  @Override
  public CacheManager getCacheManager(URI uri, ClassLoader classLoader, Properties properties) {

    Class<?> managerClass = null;
    try {
      managerClass = (classLoader != null ? classLoader : this.classLoader).loadClass("com.couchbase.client.cache.CouchbaseCacheManager");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    Constructor<?> constructor = null;
    try {
      constructor = managerClass.getConstructor(URI.class, ClassLoader.class, Properties.class);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }

    // is it necessary to call constructor by introspection?
    CacheManager manager = null;
    try {
      manager = (CacheManager) constructor.newInstance(
        uri != null ? uri :  this.uri,
        classLoader != null ? classLoader : this.classLoader,
        properties != null ? properties : this.properties);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
    // is it necessary to call manager.provider(provider) by introspection?
    // why not pass as an argument to the constructor
    Class<?> managerImpl = manager.getClass();
    try {
      Method setProvider = managerImpl.getMethod("provider", CachingProvider.class);
      setProvider.invoke(manager,this);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      // TODO Warning throw new RuntimeException(e);
    }


    managers.add(manager);
    return manager;
  }

  @Override
  public ClassLoader getDefaultClassLoader() {
    return classLoader;
  }

  @Override
  public URI getDefaultURI() {
    return uri;
  }

  @Override
  public Properties getDefaultProperties() {
    return properties;
  }

  @Override
  public CacheManager getCacheManager(URI uri, ClassLoader classLoader) {
    return getCacheManager(uri, classLoader, null);
  }

  @Override
  public CacheManager getCacheManager() {
    return getCacheManager(null, null, null);
  }

  @Override
  public void close() {
     for(CacheManager manager:managers){
       manager.close();
     }
  }

  @Override
  public void close(ClassLoader classLoader) {
    close(); // TODO
  }

  @Override
  public void close(URI uri, ClassLoader classLoader) {
    close(); // TODO
  }

  @Override
  public boolean isSupported(OptionalFeature optionalFeature) {
    return false; // TODO
  }


  public JsonObject toJson(){
    JsonObject jo = JsonObject.jo();
    jo.put( this.getClass().getSimpleName(),
      JsonObject.jo()
        .put("uri",uri.toString())
        .put("classLoader",classLoader.toString())
        .put("properties",properties));
    return jo;
  }

  @Override
  public String toString(){
    return toJson().toString();
  }
}
