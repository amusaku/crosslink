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

import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression("('${redis.connection.type:null}' == 'standalone' || '${redis.connection.type:null}' == 'sentinel')")
public class DefaultLettuceConnectionManager extends AbstractLettuceConnectionManager implements LettuceConnectionManager {

    private final LettuceConnectionFactory lettuceConnectionFactory;

    private StatefulRedisConnection<byte[], byte[]> connection;

    @SuppressWarnings({"unchecked", "deprecation"})
    @PostConstruct
    public void init() {
        RedisConnection redisConnection = lettuceConnectionFactory.getConnection();
        RedisAsyncCommands<byte[], byte[]> asyncCommands =
                (RedisAsyncCommands<byte[], byte[]>) redisConnection.getNativeConnection();
        connection = asyncCommands.getStatefulConnection();
        connection.setAutoFlushCommands(autoFlush);
    }

    @Override
    @SneakyThrows
    public String scriptLoad(String script) {
        RedisFuture<String> scriptLoadFuture = connection.async().scriptLoad(script);
        forceFlush();
        return scriptLoadFuture.get(10, TimeUnit.SECONDS);
    }

    @Override
    public RedisFuture<String> scriptLoadAsync(String script) {
        RedisFuture<String> scriptLoadFuture = connection.async().scriptLoad(script);
        flushIfNeeded();
        return scriptLoadFuture;
    }

    @Override
    public <T> RedisFuture<T> evalShaAsync(String sha, ScriptOutputType outputType, byte[][] keys, byte[]... values) {
        RedisFuture<T> evalshaFuture = connection.async().evalsha(sha, outputType, keys, values);
        flushIfNeeded();
        return evalshaFuture;
    }

    @Override
    public <T> RedisFuture<T> evalShaAsync(String sha, ScriptOutputType outputType, byte[]... keys) {
        RedisFuture<T> evalshaFuture = connection.async().evalsha(sha, outputType, keys);
        flushIfNeeded();
        return evalshaFuture;
    }

    @Override
    public <T> RedisFuture<T> evalAsync(String script, ScriptOutputType outputType, byte[][] keys, byte[]... values) {
        RedisFuture<T> evalFuture = connection.async().eval(script, outputType, keys, values);
        flushIfNeeded();
        return evalFuture;
    }

    @Override
    public <T> RedisFuture<T> evalAsync(String script, ScriptOutputType outputType, byte[]... keys) {
        RedisFuture<T> evalFuture = connection.async().eval(script, outputType, keys);
        flushIfNeeded();
        return evalFuture;
    }

    @Override
    public RedisFuture<byte[]> getAsync(byte[] key) {
        RedisFuture<byte[]> getFuture = connection.async().get(key);
        flushIfNeeded();
        return getFuture;
    }

    @Override
    public RedisFuture<Long> delAsync(byte[] key) {
        RedisFuture<Long> delFuture = connection.async().del(key);
        flushIfNeeded();
        return delFuture;
    }

    @Override
    public RedisFuture<String> setAsync(byte[] key, byte[] value) {
        RedisFuture<String> setFuture = connection.async().set(key, value);
        flushIfNeeded();
        return setFuture;
    }

    @Override
    protected void flushCommands() {
        connection.flushCommands();
        bufferedCmdCount.set(0);
    }

    @Scheduled(fixedRateString = "${lettuce.flush-interval-ms}")
    public void flushOnTimeThreshold() {
        doFlushOnTimeThreshold();
    }

}
