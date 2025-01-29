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
package org.thingsboard.mqtt.broker.actors.client.service.handlers;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttReasonCodes;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thingsboard.mqtt.broker.actors.client.messages.mqtt.MqttUnsubscribeMsg;
import org.thingsboard.mqtt.broker.actors.client.service.subscription.ClientSubscriptionService;
import org.thingsboard.mqtt.broker.adaptor.NettyMqttConverter;
import org.thingsboard.mqtt.broker.common.data.ClientType;
import org.thingsboard.mqtt.broker.common.data.util.CallbackUtil;
import org.thingsboard.mqtt.broker.service.mqtt.MqttMessageGenerator;
import org.thingsboard.mqtt.broker.service.mqtt.persistence.application.ApplicationPersistenceProcessor;
import org.thingsboard.mqtt.broker.service.subscription.shared.TopicSharedSubscription;
import org.thingsboard.mqtt.broker.session.ClientSessionCtx;
import org.thingsboard.mqtt.broker.util.MqttReasonCodeResolver;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class MqttUnsubscribeHandler {

    private final MqttMessageGenerator mqttMessageGenerator;
    private final ClientSubscriptionService clientSubscriptionService;
    private final ApplicationPersistenceProcessor applicationPersistenceProcessor;

    public void process(ClientSessionCtx ctx, MqttUnsubscribeMsg msg) {
        UUID sessionId = ctx.getSessionId();
        String clientId = ctx.getClientId();
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] Processing unsubscribe, messageId - {}, topic filters - {}", clientId, sessionId, msg.getMessageId(), msg.getTopics());
        }

        List<MqttReasonCodes.UnsubAck> codes = msg.getTopics().stream().map(s -> MqttReasonCodeResolver.unsubAckSuccess(ctx)).collect(Collectors.toList());
        MqttMessage unSubAckMessage = mqttMessageGenerator.createUnSubAckMessage(msg.getMessageId(), codes);
        clientSubscriptionService.unsubscribeAndPersist(clientId, msg.getTopics(),
                CallbackUtil.createCallback(
                        () -> ctx.getChannel().writeAndFlush(unSubAckMessage),
                        t -> log.warn("[{}][{}] Failed to process client unsubscription", clientId, sessionId, t)
                ));

        stopProcessingApplicationSharedSubscriptions(ctx, msg.getTopics());
    }

    private void stopProcessingApplicationSharedSubscriptions(ClientSessionCtx ctx, List<String> topics) {
        if (ClientType.APPLICATION == ctx.getSessionInfo().getClientInfo().getType()) {
            Set<TopicSharedSubscription> subscriptionTopicFilters = collectUniqueSharedSubscriptions(topics);
            if (CollectionUtils.isEmpty(subscriptionTopicFilters)) {
                return;
            }
            applicationPersistenceProcessor.stopProcessingSharedSubscriptions(ctx, subscriptionTopicFilters);
        }
    }

    Set<TopicSharedSubscription> collectUniqueSharedSubscriptions(List<String> topics) {
        return topics
                .stream()
                .filter(NettyMqttConverter::isSharedTopic)
                .map(topic -> new TopicSharedSubscription(
                        NettyMqttConverter.getTopicFilter(topic),
                        NettyMqttConverter.getShareName(topic)))
                .collect(Collectors.toSet());
    }

}
