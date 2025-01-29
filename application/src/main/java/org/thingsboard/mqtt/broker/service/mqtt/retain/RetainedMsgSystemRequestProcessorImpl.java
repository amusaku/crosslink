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
package org.thingsboard.mqtt.broker.service.mqtt.retain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.mqtt.broker.service.historical.stats.TbMessageStatsReportClient;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class RetainedMsgSystemRequestProcessorImpl implements RetainedMsgSystemRequestProcessor {

    private final TbMessageStatsReportClient tbMessageStatsReportClient;

    @Override
    public void processClientSessionStatsCleanup(RetainedMsg retainedMsg) {
        log.trace("[{}] Executing processClientSessionStatsCleanup", retainedMsg);
        String clientId = new String(retainedMsg.getPayload(), StandardCharsets.UTF_8);
        tbMessageStatsReportClient.removeClient(clientId);
    }
}
