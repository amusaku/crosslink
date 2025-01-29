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
package org.thingsboard.mqtt.broker.service.subscription.shared;

import org.thingsboard.mqtt.broker.common.data.subscription.TopicSubscription;
import org.thingsboard.mqtt.broker.service.subscription.EntitySubscription;
import org.thingsboard.mqtt.broker.service.subscription.ValueWithTopicFilter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SharedSubscriptionCacheService {

    void put(String clientId, Collection<TopicSubscription> topicSubscriptions);

    void remove(String clientId, TopicSubscription topicSubscription);

    SharedSubscriptions get(Set<TopicSharedSubscription> topicSharedSubscriptions);

    boolean isAnyOtherDeviceClientConnected(String clientId, TopicSharedSubscription topicSharedSubscription);

    boolean sharedSubscriptionsInitialized();

    Map<TopicSharedSubscription, SharedSubscriptions> getAllSharedSubscriptions();

    CompositeSubscriptions getSubscriptions(List<ValueWithTopicFilter<EntitySubscription>> clientSubscriptions);

}
