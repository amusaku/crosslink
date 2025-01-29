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
package org.thingsboard.mqtt.broker.actors.client.service.subscription;

import org.thingsboard.mqtt.broker.common.data.BasicCallback;
import org.thingsboard.mqtt.broker.common.data.subscription.TopicSubscription;
import org.thingsboard.mqtt.broker.service.subscription.ClientSubscriptionCache;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ClientSubscriptionService extends ClientSubscriptionCache {

    void init(Map<String, Set<TopicSubscription>> clientTopicSubscriptions);

    void subscribeAndPersist(String clientId, Collection<TopicSubscription> topicSubscriptions);

    void subscribeAndPersist(String clientId, Collection<TopicSubscription> topicSubscriptions, BasicCallback callback);

    void subscribeInternally(String clientId, Collection<TopicSubscription> topicSubscriptions);

    void unsubscribeAndPersist(String clientId, Collection<String> topicFilters);

    void unsubscribeAndPersist(String clientId, Collection<String> topicFilters, BasicCallback callback);

    void unsubscribeInternally(String clientId, Collection<String> topicFilters);

    void clearSubscriptionsAndPersist(String clientId, BasicCallback callback);

    void clearSubscriptionsInternally(String clientId);

    int getClientSubscriptionsCount();
}
