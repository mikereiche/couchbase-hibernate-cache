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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.CacheManager;
import javax.cache.configuration.OptionalFeature;
import javax.cache.spi.CachingProvider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static com.couchbase.client.core.util.Validators.notNull;

public class CouchbaseCachingProvider implements javax.cache.spi.CachingProvider {

  private final URI uri;
  private final ClassLoader classLoader;
  private final Properties properties;
  private final List<CacheManager> managers = new LinkedList<>();
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	public final static String URI_PROP = "uri";
	public final static String USERNAME_PROP = "username";
	public final static String PASSWORD_PROP = "password";
	public final static String KEYSPACE_PROP = "keyspace";

   public CouchbaseCachingProvider(URI uri, ClassLoader classLoader, Properties properties) {

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

	public static JsonObject loadJsonFromUri(URI uri) throws IOException {
		URL url = uri.toURL();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
			StringBuilder response = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
			return JsonObject.fromJson(response.toString());
		}
	}

  public CouchbaseCachingProvider() {
     this(null,null,null);
  }


  static Properties defaultProps(){
    Properties props = new Properties();
    // username/password.  These are removed before properties is fed into builder.load(props).
	props.put(URI_PROP, "couchbase://localhost");
	props.put(USERNAME_PROP, "Administrator");
	props.put(PASSWORD_PROP, "password");
	props.put(KEYSPACE_PROP, "cache");
    // remainder are fed into BuilderPropertySetter
	// props.put("security.enableTls", "false");
	// props.put("io.numKvConnections", "1");
	// props.put("timeout.kvTimeout", "2.5s");
    return props;
  }

	public static boolean isTls(URI uri) {
		if (uri.getScheme().toLowerCase().equals("couchbases") || uri.getScheme().toLowerCase().equals("couchbase2s")
				|| uri.getScheme().toLowerCase().equals("https")) {
			return true;
		} else {
			return false;
		}
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
