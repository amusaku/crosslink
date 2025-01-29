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
package org.thingsboard.mqtt.broker.service.stats;

import org.thingsboard.mqtt.broker.actors.msg.MsgType;
import org.thingsboard.mqtt.broker.actors.msg.TbActorMsg;
import org.thingsboard.mqtt.broker.common.stats.ResettableTimer;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StubClientActorStats implements ClientActorStats {
    public static ClientActorStats STUB_CLIENT_ACTOR_STATS = new StubClientActorStats();

    private StubClientActorStats() {
    }

    @Override
    public void logMsgProcessingTime(MsgType msgType, long startTime, TimeUnit unit) {
    }

    @Override
    public void logMsgQueueTime(TbActorMsg msg, TimeUnit unit) {

    }

    @Override
    public Map<String, ResettableTimer> getTimers() {
        return Collections.emptyMap();
    }

    @Override
    public int getMsgCount() {
        return 0;
    }

    @Override
    public double getQueueTimeAvg() {
        return 0;
    }

    @Override
    public double getQueueTimeMax() {
        return 0;
    }


    @Override
    public void reset() {

    }
}
