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
package org.thingsboard.mqtt.broker.service.auth.enhanced;

import org.thingsboard.mqtt.broker.common.data.ClientType;
import org.thingsboard.mqtt.broker.service.security.authorization.AuthRulePatterns;

import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

public record ScramServerWithCallbackHandler(SaslServer scramServer, ScramAuthCallbackHandler callbackHandler) {

    public byte[] evaluateResponse(byte[] authData) throws SaslException {
        return scramServer.evaluateResponse(authData);
    }

    public boolean isComplete() {
        return scramServer.isComplete();
    }

    public String getUsername() {
        return callbackHandler.getUsername();
    }

    public ClientType getClientType() {
        return callbackHandler.getClientType();
    }

    public AuthRulePatterns getAuthRulePatterns() {
        return callbackHandler.getAuthRulePatterns();
    }

}
