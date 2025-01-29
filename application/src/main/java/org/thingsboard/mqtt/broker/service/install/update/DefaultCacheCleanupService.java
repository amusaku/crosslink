/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.mqtt.broker.service.install.update;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.thingsboard.mqtt.broker.cache.CacheConstants.BASIC_CREDENTIALS_PASSWORD_CACHE;
import static org.thingsboard.mqtt.broker.cache.CacheConstants.MQTT_CLIENT_CREDENTIALS_CACHE;
import static org.thingsboard.mqtt.broker.cache.CacheConstants.SSL_REGEX_BASED_CREDENTIALS_CACHE;

@RequiredArgsConstructor
@Service
@Profile("install")
@Slf4j
public class DefaultCacheCleanupService implements CacheCleanupService {

    private final CacheManager cacheManager;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;

    @Value("${cache.cache-prefix:}")
    private String cachePrefix;

    /**
     * Cleanup caches that can not deserialize anymore due to schema upgrade or data update using sql scripts.
     * Refer to SqlDatabaseUpgradeService and /data/upgrade/*.sql
     * to discover what tables were changed
     * */
    @Override
    public void clearCache(String fromVersion) throws Exception {
        switch (fromVersion) {
            case "1.3.0":
                log.info("Clearing cache to upgrade from version 1.3.0 to 1.4.0");
                clearAll();
                break;
            case "2.0.0":
                log.info("Clearing cache to upgrade from version 2.0.0 to 2.0.1");

                Set<String> cacheNamesToFlush = Set.of(
                        cachePrefix + MQTT_CLIENT_CREDENTIALS_CACHE,
                        cachePrefix + BASIC_CREDENTIALS_PASSWORD_CACHE,
                        cachePrefix + SSL_REGEX_BASED_CREDENTIALS_CACHE);

                cacheManager.getCacheNames().forEach(cacheName -> {
                    if (cacheManager.getCache(cacheName) != null && cacheNamesToFlush.contains(cacheName)) {
                        clearCacheByName(cacheName);
                    }
                });

            default:
                //Do nothing since cache cleanup is optional.
        }
    }

    void clearAll() {
        if (redisTemplate.isPresent()) {
            log.info("Flushing all caches");
            redisTemplate.get().execute((RedisCallback<Object>) connection -> {
                connection.serverCommands().flushAll();
                return null;
            });
            return;
        }
        clearAllCaches();
    }

    void clearAllCaches() {
        cacheManager.getCacheNames().forEach(this::clearCacheByName);
    }

    void clearCacheByName(final String cacheName) {
        log.info("Clearing cache [{}]", cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        Objects.requireNonNull(cache, "Cache does not exist for name " + cacheName);
        cache.clear();
    }

}
