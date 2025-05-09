/*
 * Copyright (c) 2018 Couchbase, Inc.
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

package com.couchbase.client.cache.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import javax.cache.spi.CachingProvider;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.couchbase.client.cache.CouchbaseCacheConfig;
import com.couchbase.client.cache.CouchbaseCachingProvider;

/**
 * Basic test of Couchbase implementation of java.
 */
class CacheTest {


	@BeforeAll
	static void beforeAll() {}

	@AfterAll
	static void afterAll() {}

	@Test
	void insertAndGet() throws URISyntaxException {
		Properties props = new Properties();
		// username/password/bucket. These are removed before properties is fed into builder.load(props).
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
