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
package org.thingsboard.mqtt.broker.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.mqtt.broker.dao.timeseries.TimeseriesService;
import org.thingsboard.mqtt.broker.queue.TbQueueAdmin;
import org.thingsboard.mqtt.broker.queue.cluster.ServiceInfoProvider;
import org.thingsboard.mqtt.broker.service.provider.AbstractServiceProvider;

@Slf4j
@Service
public class TimeseriesCleanUpService extends AbstractServiceProvider {

    @Value("${sql.ttl.ts.ts_key_value_ttl}")
    protected long systemTtl;
    @Value("${sql.ttl.ts.enabled}")
    private boolean ttlTaskExecutionEnabled;

    private final TimeseriesService timeseriesService;

    public TimeseriesCleanUpService(TbQueueAdmin tbQueueAdmin,
                                    ServiceInfoProvider serviceInfoProvider,
                                    TimeseriesService timeseriesService) {
        super(tbQueueAdmin, serviceInfoProvider);
        this.timeseriesService = timeseriesService;
    }

    @Scheduled(initialDelayString = "${sql.ttl.ts.execution_interval_ms}", fixedDelayString = "${sql.ttl.ts.execution_interval_ms}")
    public void cleanUp() {
        if (ttlTaskExecutionEnabled && isCurrentNodeShouldCleanUp()) {
            log.info("Starting timeseries clean up!");
            timeseriesService.cleanUp(systemTtl);
        }
    }

}
