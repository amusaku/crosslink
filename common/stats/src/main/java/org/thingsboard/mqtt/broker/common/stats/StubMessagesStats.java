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
package org.thingsboard.mqtt.broker.common.stats;

import java.util.function.Supplier;

public class StubMessagesStats implements MessagesStats {

    public static MessagesStats STUB_MESSAGE_STATS = new StubMessagesStats();

    private StubMessagesStats(){}

    @Override
    public String getName() {
        return "STUB_STATS";
    }

    @Override
    public void incrementTotal(int amount) {}

    @Override
    public void incrementSuccessful(int amount) {}

    @Override
    public void incrementFailed(int amount) {}

    @Override
    public int getTotal() {
        return 0;
    }

    @Override
    public int getSuccessful() {
         return 0;
    }

    @Override
    public int getFailed() {
        return 0;
    }

    @Override
    public void reset() {}

    @Override
    public void updateQueueSize(Supplier<Integer> queueSizeSupplier) {

    }

    @Override
    public int getCurrentQueueSize() {
        return 0;
    }
}
