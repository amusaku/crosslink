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
package org.thingsboard.mqtt.broker.dao.client.application;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.mqtt.broker.common.data.ApplicationSharedSubscription;
import org.thingsboard.mqtt.broker.common.data.page.PageData;
import org.thingsboard.mqtt.broker.common.data.page.PageLink;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationSharedSubscriptionService {

    ApplicationSharedSubscription saveSharedSubscription(ApplicationSharedSubscription subscription);

    boolean deleteSharedSubscription(UUID id);

    ApplicationSharedSubscription findSharedSubscriptionByTopic(String topic);

    ListenableFuture<ApplicationSharedSubscription> findSharedSubscriptionByTopicAsync(String topic);

    PageData<ApplicationSharedSubscription> getSharedSubscriptions(PageLink pageLink);

    Optional<ApplicationSharedSubscription> getSharedSubscriptionById(UUID id);
}
