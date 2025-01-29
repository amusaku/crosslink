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
package org.thingsboard.mqtt.broker.cache;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RedisCacheDefaultConfigurationTest.class, loader = SpringBootContextLoader.class)
@ComponentScan({"org.thingsboard.mqtt.broker.cache"})
@EnableConfigurationProperties
@Slf4j
public class RedisCacheDefaultConfigurationTest {

    @MockBean
    private LettuceConnectionFactory lettuceConnectionFactory;

    @MockBean
    private LettuceConnectionManager lettuceConnectionManager;

    @Autowired
    TBRedisCacheConfiguration redisCacheConfiguration;

    @Test
    public void verifyTransactionAwareCacheManagerProxy() {
        assertThat(redisCacheConfiguration.getCacheSpecsMap().getSpecs()).as("specs without prefix").isNotNull();
        assertThat(redisCacheConfiguration.getCacheSpecsMap().getCacheSpecs()).as("specs").isNotNull();
        redisCacheConfiguration.getCacheSpecsMap().getCacheSpecs().forEach((name, cacheSpecs) -> {
            assertThat(name).as("cacheSpec name").isNotNull();
            assertThat(name).as("cacheSpec name has prefix").startsWith(redisCacheConfiguration.getCacheSpecsMap().getCachePrefix());
            assertThat(cacheSpecs).as("cache %s specs", name).isNotNull();
        });

        SoftAssertions softly = new SoftAssertions();
        redisCacheConfiguration.getCacheSpecsMap().getCacheSpecs().forEach((name, cacheSpecs) -> {
            softly.assertThat(name).as("cache name").isNotEmpty();
            softly.assertThat(cacheSpecs.getTimeToLiveInMinutes()).as("cache %s time to live", name).isGreaterThanOrEqualTo(0);
        });
        softly.assertAll();
    }

}
