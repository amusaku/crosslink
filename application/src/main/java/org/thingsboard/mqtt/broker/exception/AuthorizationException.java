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
package org.thingsboard.mqtt.broker.exception;

import lombok.Getter;

import java.io.Serial;

@Getter
public class AuthorizationException extends Exception {

    @Serial
    private static final long serialVersionUID = 4536818723254827699L;

    private final String deniedTopic;

    public AuthorizationException(String deniedTopic) {
        this.deniedTopic = deniedTopic;
    }

    public AuthorizationException(String message, String deniedTopic) {
        super(message);
        this.deniedTopic = deniedTopic;
    }

    public AuthorizationException(String message, Throwable cause, String deniedTopic) {
        super(message, cause);
        this.deniedTopic = deniedTopic;
    }

    public AuthorizationException(Throwable cause, String deniedTopic) {
        super(cause);
        this.deniedTopic = deniedTopic;
    }

    public AuthorizationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, String deniedTopic) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.deniedTopic = deniedTopic;
    }

}
