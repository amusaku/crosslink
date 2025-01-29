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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.thingsboard.mqtt.broker.service.subscription.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode
public class SharedSubscriptions {

    private final Set<Subscription> applicationSubscriptions;
    private final Set<Subscription> deviceSubscriptions;

    public static SharedSubscriptions newInstance() {
        return new SharedSubscriptions(ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet());
    }

    public boolean isEmpty() {
        return CollectionUtils.isEmpty(applicationSubscriptions) && CollectionUtils.isEmpty(deviceSubscriptions);
    }

    public List<Subscription> getAllSubscriptions() {
        List<Subscription> result = new ArrayList<>(applicationSubscriptions.size() + deviceSubscriptions.size());
        result.addAll(applicationSubscriptions);
        result.addAll(deviceSubscriptions);
        return result;
    }

}
