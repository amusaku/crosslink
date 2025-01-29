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
package org.thingsboard.mqtt.broker.service.mqtt.client.event;

import org.springframework.stereotype.Service;
import org.thingsboard.mqtt.broker.adaptor.ProtoConverter;
import org.thingsboard.mqtt.broker.common.data.BrokerConstants;
import org.thingsboard.mqtt.broker.common.data.ClientInfo;
import org.thingsboard.mqtt.broker.common.data.ClientSessionInfo;
import org.thingsboard.mqtt.broker.common.data.SessionInfo;
import org.thingsboard.mqtt.broker.gen.queue.ClientSessionEventProto;

import java.util.UUID;

@Service
public class ClientSessionEventFactoryImpl implements ClientSessionEventFactory {

    @Override
    public ClientSessionEventProto createConnectionRequestEventProto(SessionInfo sessionInfo) {
        return ClientSessionEventProto.newBuilder()
                .setSessionInfo(ProtoConverter.convertToSessionInfoProto(sessionInfo))
                .setEventType(ClientSessionEventType.CONNECTION_REQUEST.toString())
                .build();
    }

    @Override
    public ClientSessionEventProto createDisconnectedEventProto(ClientInfo clientInfo, UUID sessionId,
                                                                            int sessionExpiryInterval) {
        SessionInfo disconnectSessionInfo = SessionInfo.builder()
                .sessionId(sessionId)
                .clientInfo(clientInfo)
                .serviceId(BrokerConstants.EMPTY_STR)
                .sessionExpiryInterval(sessionExpiryInterval)
                .build();
        return ClientSessionEventProto.newBuilder()
                .setSessionInfo(ProtoConverter.convertToSessionInfoProto(disconnectSessionInfo))
                .setEventType(ClientSessionEventType.DISCONNECTION_REQUEST.toString())
                .build();
    }

    @Override
    public ClientSessionEventProto createClearSessionRequestEventProto(SessionInfo sessionInfo) {
        return ClientSessionEventProto.newBuilder()
                .setSessionInfo(ProtoConverter.convertToSessionInfoProto(sessionInfo))
                .setEventType(ClientSessionEventType.CLEAR_SESSION_REQUEST.toString())
                .build();
    }

    @Override
    public ClientSessionEventProto createClearSessionRequestEventProto(ClientSessionInfo clientSessionInfo) {
        return ClientSessionEventProto.newBuilder()
                .setSessionInfo(ProtoConverter.convertToSessionInfoProto(clientSessionInfo))
                .setEventType(ClientSessionEventType.CLEAR_SESSION_REQUEST.toString())
                .build();
    }

    @Override
    public ClientSessionEventProto createApplicationTopicRemoveRequestEventProto(String clientId) {
        return ClientSessionEventProto.newBuilder()
                .setEventType(ClientSessionEventType.REMOVE_APPLICATION_TOPIC_REQUEST.toString())
                .build();
    }

}
