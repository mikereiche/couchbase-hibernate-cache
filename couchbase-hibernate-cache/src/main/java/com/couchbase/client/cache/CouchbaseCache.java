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

/**
 */
import com.couchbase.client.core.error.DocumentExistsException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;

import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class CouchbaseCache<K, V> implements javax.cache.Cache<K, V> {

	private final String name;
	private Collection collection;
	private final CouchbaseCacheConfig config;
	private boolean isClosed = false;
	private final CouchbaseCacheManager manager;

	public CouchbaseCache(CouchbaseCacheManager manager, String name, Collection collection, CouchbaseCacheConfig config) {
		this.manager = manager;
		this.name = name;
		this.collection = collection;
		this.config = config;
	}

	@Override
	public V get(K k) {
		GetResult result = collection.get(k.toString());
		return (V) result.contentAs(config.getValueType());
	}

	@Override
	public Map<K, V> getAll(Set<? extends K> set) {
		return null;
	}

	@Override
	public boolean containsKey(K k) {
		return collection.exists(k.toString()).exists();
	}

	@Override
	public void loadAll(Set<? extends K> set, boolean b, CompletionListener completionListener) {

	}

	@Override
	public void put(K k, V v) {
		collection.upsert(k.toString(), v);
	}

	@Override
	public V getAndPut(K k, V v) {
		return null;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {

	}

	@Override
	public boolean putIfAbsent(K k, V v) {
		try {
			collection.insert(k.toString(), v);
			return true;
		} catch (DocumentExistsException de) {
			return false;
		}
	}

	@Override
	public boolean remove(K k) {
		try {
			collection.remove(k.toString());
		} catch (DocumentNotFoundException dnf) {
			return false;
		}
		return true;
	}

	@Override
	public boolean remove(K k, V v) {
		return false;
	}

	@Override
	public V getAndRemove(K k) {
		return null;
	}

	@Override
	public boolean replace(K k, V v, V v1) {
		return false;
	}

	@Override
	public boolean replace(K k, V v) {
		return false;
	}

	@Override
	public V getAndReplace(K k, V v) {
		return null;
	}

	@Override
	public void removeAll(Set<? extends K> set) {

	}

	@Override
	public void removeAll() {

	}

	@Override
	public void clear() {

	}

	@Override
	public <C extends Configuration<K, V>> C getConfiguration(Class<C> aClass) {
		return null;
	}

	@Override
	public <T> T invoke(K k, EntryProcessor<K, V, T> entryProcessor, Object... objects) throws EntryProcessorException {
		return null;
	}

	@Override
	public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> set, EntryProcessor<K, V, T> entryProcessor,
			Object... objects) {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public CacheManager getCacheManager() {
		return manager;
	}

	@Override
	public void close() {
		// TODO
		isClosed = true;
	}

	@Override
	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public <T> T unwrap(Class<T> aClass) {
		//  TODO
		return null;
	}

	@Override
	public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
		// TODO
	}

	@Override
	public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
		// TODO
	}

	@Override
	public Iterator<Entry<K, V>> iterator() {
		return null;
	}
}
