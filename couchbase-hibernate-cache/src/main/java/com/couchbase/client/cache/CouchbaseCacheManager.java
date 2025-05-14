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
import com.couchbase.client.java.codec.RawBinaryTranscoder;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.couchbase.client.cache.CouchbaseCachingProvider.KEYSPACE_PROP;
import static com.couchbase.client.cache.CouchbaseCachingProvider.PASSWORD_PROP;
import static com.couchbase.client.cache.CouchbaseCachingProvider.URI_PROP;
import static com.couchbase.client.cache.CouchbaseCachingProvider.USERNAME_PROP;
import static com.couchbase.client.cache.CouchbaseCachingProvider.defaultProps;
import static com.couchbase.client.cache.CouchbaseCachingProvider.isTls;
import static com.couchbase.client.core.util.Validators.notNull;

public class CouchbaseCacheManager implements javax.cache.CacheManager {

	private final URI uri;
	private final ClassLoader classLoader;
	private final Properties properties;
	private final Map<Object, Collection> collections = new HashMap<>();
	private final Map<String, Cache> caches = new HashMap<>();
	private CachingProvider provider;
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	public CouchbaseCacheManager(URI uri, ClassLoader classLoader, Properties properties) {

		properties = defaultProps();
		if (uri != null) {
			try {
				URL url = uri.toURL();
				InputStream inputStream = url.openStream();
				properties.load(inputStream);
				notNull(properties.get(URI_PROP), URI_PROP);
				notNull(properties.get(USERNAME_PROP), USERNAME_PROP);
				notNull(properties.get(PASSWORD_PROP), PASSWORD_PROP);
				notNull(properties.get(KEYSPACE_PROP), KEYSPACE_PROP);
				/*
				props.put("security.enableTls", String.valueOf(isTls(uri)));
				props.put("io.numKvConnections", "1");
				props.put("timeout.kvTimeout", "2.5s");
				 */

			} catch (IOException e) {
				System.err.println("Error loading properties: " + e.getMessage());
			}
		}

		this.uri = uri;
		this.classLoader = classLoader != null ? classLoader : CouchbaseCachingProvider.class.getClassLoader();
		this.properties = properties;

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
	public <KEY_CLASS, VALUE_CLASS, CONFIGURATION extends Configuration<KEY_CLASS, VALUE_CLASS>> Cache<KEY_CLASS, VALUE_CLASS> createCache(
			String name, CONFIGURATION configuration) throws IllegalArgumentException {
		Collection collection = collections.get(configuration);
		if (collection == null) {
			logger.debug("CREATING CACHE: " + uri + " keytype: " + configuration.getKeyType() + " valueType: "
					+ configuration.getValueType());

			Properties p = new Properties();
			p.putAll(properties);
			String connectString = p.getProperty(URI_PROP);
			p.remove(URI_PROP);
			String username = p.getProperty(USERNAME_PROP);
			p.remove(USERNAME_PROP);
			char[] password = p.getProperty(PASSWORD_PROP).toCharArray();
			p.remove(PASSWORD_PROP);
			String bucket = p.getProperty(KEYSPACE_PROP);
			p.remove(KEYSPACE_PROP);
			// BuilderPropertySetter
			PropertyLoader loader = PropertyLoader.fromMap((Map) p);
			ClusterEnvironment.Builder builder = ClusterEnvironment.builder().load(loader);
      builder.transcoder(RawBinaryTranscoder.INSTANCE);
			ClusterEnvironment env = builder.build();
			if (env.securityConfig().tlsEnabled() && !isTls(uri)) {
				logger.debug("property tlsEnabled did not match url, disabling TLS");
				env = builder.securityConfig(sc -> sc.enableCertificateVerification(false)).build();
			}

			Cluster cluster = Cluster.connect(connectString,
					ClusterOptions.clusterOptions(username, String.valueOf(password)).environment(env));
			collection = cluster.bucket(bucket).defaultCollection();
		}
		return new CouchbaseCache<>(this, name, collection,
				/*(CouchbaseCacheConfig)*/ (MutableConfiguration) configuration);
	}

	@Override
	public <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyClass, Class<V> valueClass) {
		Cache cache = getCache(cacheName);
		Configuration config = cache.getConfiguration(Configuration.class);
		logger.debug("getCache -> " + cache);
		if (config.getKeyType() == keyClass && config.getValueType() == valueClass) {
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
		// TODO
		return null;
	}

	public void provider(CachingProvider provider) {
		this.provider = provider;
	}

	@Override
	public String toString() {
		JsonObject jo = JsonObject.jo();
		jo.put(this.getClass().getSimpleName(),
				JsonObject.jo().put("uri", uri.toString()).put("classLoader", classLoader.toString())
						.put("properties", properties)
						.put(CouchbaseCachingProvider.class.getSimpleName(), ((CouchbaseCachingProvider) provider)
								.toJson().get(CouchbaseCachingProvider.class.getSimpleName())));
		return jo.toString();
	}

}
