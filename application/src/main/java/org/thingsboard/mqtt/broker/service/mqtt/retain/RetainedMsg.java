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
package org.thingsboard.mqtt.broker.service.mqtt.retain;

import io.netty.handler.codec.mqtt.MqttProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class RetainedMsg {

    private final String topic;
    private final byte[] payload;
    private final int qos;
    private final MqttProperties properties;
    private final long createdTime;

    public RetainedMsg(String topic, byte[] payload) {
        this(topic, payload, 0);
    }

    public RetainedMsg(String topic, byte[] payload, int qos) {
        this(topic, payload, qos, MqttProperties.NO_PROPERTIES, System.currentTimeMillis());
    }

    public RetainedMsg(String topic, byte[] payload, int qos, MqttProperties properties) {
        this(topic, payload, qos, properties, System.currentTimeMillis());
    }

    public RetainedMsg withQosAndProps(int qos, MqttProperties properties) {
        return new RetainedMsg(this.topic, this.payload, qos, properties, this.createdTime);
    }
}
