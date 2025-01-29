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
package org.thingsboard.mqtt.broker.queue.publish;

import com.google.protobuf.GeneratedMessageV3;
import org.thingsboard.mqtt.broker.queue.TbQueueCallback;
import org.thingsboard.mqtt.broker.queue.common.TbProtoQueueMsg;

public interface TbPublishService<PROTO extends GeneratedMessageV3> {

    void init();

    void destroy();

    void send(TbProtoQueueMsg<PROTO> msg, TbQueueCallback callback);

    void send(TbProtoQueueMsg<PROTO> msg, TbQueueCallback callback, String topic);
}
