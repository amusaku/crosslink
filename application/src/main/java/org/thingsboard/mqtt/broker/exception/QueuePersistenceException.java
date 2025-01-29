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

import java.io.Serial;

public class QueuePersistenceException extends Exception {

    @Serial
    private static final long serialVersionUID = 7818039060546289297L;

    public QueuePersistenceException() {
    }

    public QueuePersistenceException(String message) {
        super(message);
    }

    public QueuePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueuePersistenceException(Throwable cause) {
        super(cause);
    }

    public QueuePersistenceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
