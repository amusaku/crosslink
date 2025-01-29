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
package org.thingsboard.mqtt.broker.dao.messages;

import org.thingsboard.mqtt.broker.common.data.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public record ClientIdMessagesCacheKey(String clientId, String cachePrefix) implements Serializable {

    @Serial
    private static final long serialVersionUID = 65684921903757140L;

    @Override
    public String toString() {
        String keyBase = "{" + clientId + "}_messages";
        return StringUtils.isBlank(cachePrefix) ? keyBase : cachePrefix + keyBase;
    }

    private static ClientIdMessagesCacheKey newInstance(String clientId, String cachePrefix) {
        return new ClientIdMessagesCacheKey(clientId, cachePrefix);
    }

    public static String toStringKey(String clientId, String cachePrefix) {
        return newInstance(clientId, cachePrefix).toString();
    }

    public static byte[] toBytesKey(String clientId, String cachePrefix) {
        return toStringKey(clientId, cachePrefix).getBytes(StandardCharsets.UTF_8);
    }

}
