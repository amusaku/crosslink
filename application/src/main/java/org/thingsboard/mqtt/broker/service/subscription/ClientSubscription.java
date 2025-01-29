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
package org.thingsboard.mqtt.broker.service.subscription;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.mqtt.broker.common.data.subscription.SubscriptionOptions;
import org.thingsboard.mqtt.broker.service.subscription.data.EntitySubscriptionType;

import java.util.Objects;

@AllArgsConstructor
@Getter
@ToString
public class ClientSubscription implements EntitySubscription {

    private final String clientId;
    private final int qos;
    private final String shareName;
    private final SubscriptionOptions options;
    private final int subscriptionId;

    public ClientSubscription(String clientId, int qos, String shareName, SubscriptionOptions options) {
        this(clientId, qos, shareName, options, -1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientSubscription that = (ClientSubscription) o;
        return clientId.equals(that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId);
    }

    @Override
    public EntitySubscriptionType getType() {
        return EntitySubscriptionType.DEFAULT;
    }
}
