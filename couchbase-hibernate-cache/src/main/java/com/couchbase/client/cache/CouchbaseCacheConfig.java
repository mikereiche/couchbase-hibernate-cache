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

import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import java.util.Properties;

/**
 * this class is maybe unnecessary ?
  */

public class CouchbaseCacheConfig<K, V> extends MutableConfiguration<K, V> implements Configuration<K, V> {


  Properties properties;

  public CouchbaseCacheConfig(Class<K> keyClass, Class<V> valueClass, Properties properties){
    setTypes(keyClass, valueClass);
    this.properties = properties;
  }


}
