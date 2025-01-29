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

import java.util.concurrent.TimeUnit;

public class StubClientSessionEventConsumerStats implements ClientSessionEventConsumerStats {

    public static StubClientSessionEventConsumerStats STUB_CLIENT_SESSION_EVENT_CONSUMER_STATS = new StubClientSessionEventConsumerStats();

    private StubClientSessionEventConsumerStats(){}

    @Override
    public String getConsumerId() {
        return "STUB_CONSUMER_ID";
    }

    @Override
    public void logPackProcessingTime(int packSize, long amount, TimeUnit unit) {
    }

    @Override
    public double getAvgPackProcessingTime() {
        return 0;
    }

    @Override
    public double getAvgPackSize() {
        return 0;
    }

    @Override
    public void reset() {
    }
}
