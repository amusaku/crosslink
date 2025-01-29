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

import org.thingsboard.mqtt.broker.common.stats.StatsCounter;
import org.thingsboard.mqtt.broker.service.processing.PackProcessingResult;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface PublishMsgConsumerStats {
    String getConsumerId();

    void log(int totalMessagesCount, PackProcessingResult packProcessingResult, boolean finalIterationForPack);

    void logMsgProcessingTime(long amount, TimeUnit unit);

    void logPackProcessingTime(int packSize, long amount, TimeUnit unit);

    List<StatsCounter> getStatsCounters();

    double getAvgMsgProcessingTime();

    double getAvgPackProcessingTime();

    double getAvgPackSize();

    void reset();
}
