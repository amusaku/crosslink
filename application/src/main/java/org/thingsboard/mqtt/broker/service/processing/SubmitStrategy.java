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
package org.thingsboard.mqtt.broker.service.processing;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public interface SubmitStrategy {

    void init(Map<UUID, PublishMsgWithId> messagesMap);

    ConcurrentMap<UUID, PublishMsgWithId> getPendingMap();

    void process(Consumer<PublishMsgWithId> msgConsumer);

    void update(Map<UUID, PublishMsgWithId> reprocessMap);
}
