package com.couchbase.client.cache.test;

import com.couchbase.client.cache.CouchbaseCacheConfig;
import com.couchbase.client.cache.CouchbaseCachingProvider;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class Test {

  public static void main(String[] args) throws URISyntaxException {
    Properties props = new Properties();
    // username/password/bucket.  These are removed before properties is fed into builder.load(props).
    // default scope and collection are used
    props.put("username", "Administrator");
    props.put("password", "password");
    props.put("bucket", "cache");
    // remainder are fed into BuilderPropertySetter to build ClusterEnvironment...
    props.put("timeout.kvTimeout", "2.5s");
    CachingProvider cp = new CouchbaseCachingProvider(new URI("couchbase://localhost"), null, props);
    CacheManager manager = cp.getCacheManager();
    Configuration config = new CouchbaseCacheConfig(String.class, User.class, new Properties());
    Cache<String, Object> cache = manager.createCache("test", config);
    User u = new User("Walt", "Whitman");
    cache.put(u.id, u);
    System.out.println(cache.get(u.id));
  }
}
