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
package org.thingsboard.mqtt.broker.common.data.subscription;

import io.netty.handler.codec.mqtt.MqttSubscriptionOption;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.thingsboard.mqtt.broker.common.data.dto.SubscriptionOptionsDto;

import java.util.function.Function;

@ToString
@EqualsAndHashCode
@Getter
@RequiredArgsConstructor
public final class SubscriptionOptions {

    public enum RetainHandlingPolicy {
        SEND_AT_SUBSCRIBE(0),
        SEND_AT_SUBSCRIBE_IF_NOT_YET_EXISTS(1),
        DONT_SEND_AT_SUBSCRIBE(2);

        private final int value;

        RetainHandlingPolicy(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static RetainHandlingPolicy valueOf(int value) {
            return switch (value) {
                case 0 -> SEND_AT_SUBSCRIBE;
                case 1 -> SEND_AT_SUBSCRIBE_IF_NOT_YET_EXISTS;
                case 2 -> DONT_SEND_AT_SUBSCRIBE;
                default -> throw new IllegalArgumentException("invalid RetainedHandlingPolicy: " + value);
            };
        }
    }

    private final boolean noLocal;
    private final boolean retainAsPublish;
    private final RetainHandlingPolicy retainHandling;

    public SubscriptionOptions() {
        this.noLocal = false;
        this.retainAsPublish = false;
        this.retainHandling = RetainHandlingPolicy.SEND_AT_SUBSCRIBE;
    }

    public static SubscriptionOptions newInstance(MqttSubscriptionOption option) {
        return new SubscriptionOptions(
                option.isNoLocal(),
                option.isRetainAsPublished(),
                RetainHandlingPolicy.valueOf(option.retainHandling().value())
        );
    }

    public static SubscriptionOptions newInstance() {
        return new SubscriptionOptions(false, false, RetainHandlingPolicy.SEND_AT_SUBSCRIBE);
    }

    public static SubscriptionOptions fromSubscriptionOptionsDto(SubscriptionOptionsDto options) {
        return new SubscriptionOptions(options.isNoLocal(), options.isRetainAsPublish(), RetainHandlingPolicy.valueOf(options.getRetainHandling()));
    }

    public boolean isNoLocalOptionMet(String receiverClientId, String senderClientId) {
        return receiverClientId.equals(senderClientId) && noLocal;
    }

    public boolean isRetain(boolean retain) {
        if (!retainAsPublish) {
            return false;
        }
        return retain;
    }

    public boolean needSendRetainedForTopicSubscription(Function<TopicSubscription, Boolean> subscriptionPresentFunction,
                                                        TopicSubscription topicSubscription) {
        return switch (retainHandling) {
            case SEND_AT_SUBSCRIBE -> true;
            case SEND_AT_SUBSCRIBE_IF_NOT_YET_EXISTS -> subscriptionPresentFunction.apply(topicSubscription);
            case DONT_SEND_AT_SUBSCRIBE -> false;
        };
    }
}
