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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.mqtt.broker.adaptor.NettyMqttConverter;
import org.thingsboard.mqtt.broker.service.mqtt.PublishMsg;
import org.thingsboard.mqtt.broker.util.MqttPropertiesUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetainedMsgProcessorImpl implements RetainedMsgProcessor {

    private final RetainedMsgListenerService retainedMsgListenerService;

    @Override
    public PublishMsg process(PublishMsg publishMsg) {
        if (publishMsg.getByteBuf() != null && publishMsg.getPayload() == null) {
            byte[] payload = NettyMqttConverter.toBytes(publishMsg.getByteBuf());
            publishMsg = publishMsg.toBuilder().payload(payload).build();
        }
        return processRetainedMsg(publishMsg);
    }

    private PublishMsg processRetainedMsg(PublishMsg publishMsg) {
        if (payloadIsEmpty(publishMsg)) {
            retainedMsgListenerService.clearRetainedMsgAndPersist(publishMsg.getTopicName());
        } else {
            retainedMsgListenerService.cacheRetainedMsgAndPersist(publishMsg.getTopicName(), newRetainedMsg(publishMsg));
        }
        return publishMsg;
    }

    private boolean payloadIsEmpty(PublishMsg publishMsg) {
        return publishMsg.getPayload().length == 0;
    }

    private RetainedMsg newRetainedMsg(PublishMsg publishMsg) {
        MqttProperties properties = MqttPropertiesUtil.getMqttProperties(publishMsg);
        return new RetainedMsg(publishMsg.getTopicName(), publishMsg.getPayload(), publishMsg.getQos(), properties);
    }

}
