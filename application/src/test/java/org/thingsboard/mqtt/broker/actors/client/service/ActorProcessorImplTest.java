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
package org.thingsboard.mqtt.broker.actors.client.service;

import com.google.common.util.concurrent.Futures;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttReasonCodes;
import io.netty.handler.codec.mqtt.MqttVersion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.mqtt.broker.actors.client.messages.EnhancedAuthInitMsg;
import org.thingsboard.mqtt.broker.actors.client.messages.SessionInitMsg;
import org.thingsboard.mqtt.broker.actors.client.messages.mqtt.MqttAuthMsg;
import org.thingsboard.mqtt.broker.actors.client.messages.mqtt.MqttConnectMsg;
import org.thingsboard.mqtt.broker.actors.client.messages.mqtt.MqttDisconnectMsg;
import org.thingsboard.mqtt.broker.actors.client.service.disconnect.DisconnectService;
import org.thingsboard.mqtt.broker.actors.client.state.ClientActorState;
import org.thingsboard.mqtt.broker.actors.client.state.DefaultClientActorState;
import org.thingsboard.mqtt.broker.actors.client.state.SessionState;
import org.thingsboard.mqtt.broker.common.data.ClientType;
import org.thingsboard.mqtt.broker.common.data.UnauthorizedClient;
import org.thingsboard.mqtt.broker.common.data.client.credentials.ScramAlgorithm;
import org.thingsboard.mqtt.broker.dao.client.unauthorized.UnauthorizedClientService;
import org.thingsboard.mqtt.broker.exception.AuthenticationException;
import org.thingsboard.mqtt.broker.service.auth.AuthenticationService;
import org.thingsboard.mqtt.broker.service.auth.EnhancedAuthenticationService;
import org.thingsboard.mqtt.broker.service.auth.enhanced.EnhancedAuthContext;
import org.thingsboard.mqtt.broker.service.auth.enhanced.EnhancedAuthContinueResponse;
import org.thingsboard.mqtt.broker.service.auth.enhanced.EnhancedAuthFinalResponse;
import org.thingsboard.mqtt.broker.service.auth.providers.AuthResponse;
import org.thingsboard.mqtt.broker.service.mqtt.MqttMessageGenerator;
import org.thingsboard.mqtt.broker.service.security.authorization.AuthRulePatterns;
import org.thingsboard.mqtt.broker.session.ClientMqttActorManager;
import org.thingsboard.mqtt.broker.session.ClientSessionCtx;
import org.thingsboard.mqtt.broker.session.DisconnectReason;
import org.thingsboard.mqtt.broker.session.DisconnectReasonType;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_BAD_AUTHENTICATION_METHOD;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED_5;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_UNSPECIFIED_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.thingsboard.mqtt.broker.service.auth.enhanced.EnhancedAuthFailure.AUTH_CHALLENGE_FAILED;
import static org.thingsboard.mqtt.broker.service.auth.enhanced.EnhancedAuthFailure.AUTH_METHOD_MISMATCH;
import static org.thingsboard.mqtt.broker.service.auth.enhanced.EnhancedAuthFailure.CLIENT_FINAL_MESSAGE_EVALUATION_ERROR;
import static org.thingsboard.mqtt.broker.service.auth.enhanced.EnhancedAuthFailure.CLIENT_FIRST_MESSAGE_EVALUATION_ERROR;
import static org.thingsboard.mqtt.broker.service.auth.enhanced.EnhancedAuthFailure.CLIENT_RE_AUTH_MESSAGE_EVALUATION_ERROR;

@RunWith(MockitoJUnitRunner.class)
public class ActorProcessorImplTest {

    ActorProcessorImpl actorProcessor;

    DisconnectService disconnectService;
    AuthenticationService authenticationService;
    EnhancedAuthenticationService enhancedAuthenticationService;
    MqttMessageGenerator mqttMessageGenerator;
    ClientMqttActorManager clientMqttActorManager;
    UnauthorizedClientService unauthorizedClientService;

    ClientActorState clientActorState;

    @Before
    public void setUp() {
        disconnectService = mock(DisconnectService.class);
        authenticationService = mock(AuthenticationService.class);
        mqttMessageGenerator = spy(MqttMessageGenerator.class);
        enhancedAuthenticationService = mock(EnhancedAuthenticationService.class);
        clientMqttActorManager = mock(ClientMqttActorManager.class);
        unauthorizedClientService = mock(UnauthorizedClientService.class);
        actorProcessor = spy(new ActorProcessorImpl(disconnectService, authenticationService, enhancedAuthenticationService,
                mqttMessageGenerator, clientMqttActorManager, unauthorizedClientService));

        clientActorState = new DefaultClientActorState("clientId", false, 0);
    }

    @Test
    public void givenConnectedSession_whenOnDisconnect_thenOk() {
        updateSessionState(SessionState.CONNECTED);

        doNothing().when(disconnectService).disconnect(any(), any());
        actorProcessor.onDisconnect(clientActorState, getDisconnectMsg());

        assertEquals(SessionState.DISCONNECTED, clientActorState.getCurrentSessionState());
        verify(disconnectService, times(1)).disconnect(any(), any());
    }

    @Test
    public void givenDisconnectedSession_whenOnDisconnect_thenOk() {
        updateSessionState(SessionState.DISCONNECTED);

        actorProcessor.onDisconnect(clientActorState, getDisconnectMsg());

        assertEquals(SessionState.DISCONNECTED, clientActorState.getCurrentSessionState());
        verify(disconnectService, never()).disconnect(any(), any());
    }

    @Test
    public void givenDisconnectingSession_whenOnDisconnect_thenOk() {
        updateSessionState(SessionState.DISCONNECTING);

        actorProcessor.onDisconnect(clientActorState, getDisconnectMsg());

        assertEquals(SessionState.DISCONNECTING, clientActorState.getCurrentSessionState());
        verify(disconnectService, never()).disconnect(any(), any());
    }

    @Test
    public void givenDisconnectedSession_whenOnInit_thenOk() throws AuthenticationException {
        updateSessionState(SessionState.DISCONNECTED);

        AuthResponse authResponse = getAuthResponse(true);
        doReturn(authResponse).when(authenticationService).authenticate(any());
        when(unauthorizedClientService.remove(any())).thenReturn(Futures.immediateFuture(null));

        SessionInitMsg sessionInitMsg = getSessionInitMsg(getClientSessionCtx());
        actorProcessor.onInit(clientActorState, sessionInitMsg);

        assertEquals(SessionState.INITIALIZED, clientActorState.getCurrentSessionState());
        assertEquals(sessionInitMsg.getClientSessionCtx(), clientActorState.getCurrentSessionCtx());
        assertEquals(1, clientActorState.getCurrentSessionCtx().getAuthRulePatterns().size());
        assertEquals(1, clientActorState.getCurrentSessionCtx().getAuthRulePatterns().get(0).getPubPatterns().size());
        assertEquals(1, clientActorState.getCurrentSessionCtx().getAuthRulePatterns().get(0).getSubPatterns().size());
        assertEquals("test", clientActorState.getCurrentSessionCtx().getAuthRulePatterns().get(0).getPubPatterns().get(0).pattern());
        assertEquals("test", clientActorState.getCurrentSessionCtx().getAuthRulePatterns().get(0).getSubPatterns().get(0).pattern());
        assertEquals(ClientType.APPLICATION, clientActorState.getCurrentSessionCtx().getClientType());
    }

    @Test
    public void givenSameSession_whenOnInit_thenDisconnect() throws AuthenticationException {
        updateSessionState(SessionState.CONNECTED);

        ClientSessionCtx clientSessionCtx = getClientSessionCtx();
        clientActorState.setClientSessionCtx(clientSessionCtx);

        SessionInitMsg sessionInitMsg = getSessionInitMsg(clientSessionCtx);
        actorProcessor.onInit(clientActorState, sessionInitMsg);

        assertEquals(SessionState.DISCONNECTING, clientActorState.getCurrentSessionState());
        verify(disconnectService, times(1)).disconnect(any(), any());
        verify(authenticationService, never()).authenticate(any());
    }

    @Test
    public void givenDisconnectedSession_whenOnInitAndAuthenticateFailed_thenClose() throws AuthenticationException {
        updateSessionState(SessionState.DISCONNECTED);

        AuthResponse authResponse = getAuthResponse(false);
        doReturn(authResponse).when(authenticationService).authenticate(any());
        when(unauthorizedClientService.save(any())).thenReturn(Futures.immediateFuture(null));

        doNothing().when(actorProcessor).sendConnectionRefusedNotAuthorizedMsgAndCloseChannel(any());

        SessionInitMsg sessionInitMsg = getSessionInitMsg(getClientSessionCtx());
        actorProcessor.onInit(clientActorState, sessionInitMsg);

        assertEquals(SessionState.DISCONNECTED, clientActorState.getCurrentSessionState());
        verify(actorProcessor, never()).updateClientActorState(any(), eq(SessionState.INITIALIZED), any());
        verify(actorProcessor, times(1)).sendConnectionRefusedNotAuthorizedMsgAndCloseChannel(any());
    }

    @Test
    public void givenDisconnectedSession_whenOnEnhancedAuthInitAndChallengeStarted_thenUpdateStateToEnhancedAuthStarted() {
        updateSessionState(SessionState.DISCONNECTED);

        var clientSessionCtxMock = mock(ClientSessionCtx.class);
        var channelHandlerCtxMock = mock(ChannelHandlerContext.class);
        var mqttMessageMock = mock(MqttMessage.class);

        when(clientSessionCtxMock.getSessionId()).thenReturn(UUID.fromString("93db435c-8059-43ae-b41f-22776404039e"));
        when(clientSessionCtxMock.getChannel()).thenReturn(channelHandlerCtxMock);

        doReturn(mqttMessageMock).when(mqttMessageGenerator).createMqttAuthMsg(anyString(), any(), any(MqttReasonCodes.Auth.class));

        var success = EnhancedAuthContinueResponse.success("username", "server-first-data".getBytes(StandardCharsets.UTF_8));
        doReturn(success).when(enhancedAuthenticationService)
                .onClientConnectMsg(any(ClientSessionCtx.class), any(EnhancedAuthContext.class));

        EnhancedAuthInitMsg enhancedAuthInitMsg = getEnhancedAuthInitMsg(clientSessionCtxMock);
        actorProcessor.onEnhancedAuthInit(clientActorState, enhancedAuthInitMsg);

        verify(mqttMessageGenerator).createMqttAuthMsg(anyString(), any(), eq(MqttReasonCodes.Auth.CONTINUE_AUTHENTICATION));
        verify(clientSessionCtxMock).getChannel();
        verify(channelHandlerCtxMock).writeAndFlush(mqttMessageMock);

        assertThat(clientActorState.getCurrentSessionState()).isEqualTo(SessionState.ENHANCED_AUTH_STARTED);

    }

    @Test
    public void givenSameSessionId_whenOnEnhancedAuthInit_thenTryDisconnectSameSessionCalled() {
        updateSessionState(SessionState.CONNECTED);

        ClientSessionCtx clientSessionCtx = getClientSessionCtx();
        clientActorState.setClientSessionCtx(clientSessionCtx);

        EnhancedAuthInitMsg enhancedAuthInitMsg = getEnhancedAuthInitMsg(clientSessionCtx);
        actorProcessor.onEnhancedAuthInit(clientActorState, enhancedAuthInitMsg);

        assertThat(clientActorState.getCurrentSessionState()).isEqualTo(SessionState.DISCONNECTING);

        var mqttDisconnectMsgCaptor = ArgumentCaptor.forClass(MqttDisconnectMsg.class);
        verify(disconnectService).disconnect(eq(clientActorState), mqttDisconnectMsgCaptor.capture());

        MqttDisconnectMsg mqttDisconnectMsg = mqttDisconnectMsgCaptor.getValue();
        assertThat(mqttDisconnectMsg.getReason().getType()).isEqualTo(DisconnectReasonType.ON_CONFLICTING_SESSIONS);
        assertThat(mqttDisconnectMsg.getReason().getMessage()).isEqualTo("Trying to init the same active session");

        verify(enhancedAuthenticationService, never()).onClientConnectMsg(any(ClientSessionCtx.class), any(EnhancedAuthContext.class));
    }

    @Test
    public void givenDisconnectedSession_whenOnEnhancedAuthInitAndChallengeFailed_thenSendConnectionRefusedNotAuthorizedErrorAndCloseChannel() {
        updateSessionState(SessionState.DISCONNECTED);

        ClientSessionCtx sessionCtxMock = mock(ClientSessionCtx.class);
        ChannelHandlerContext ctxMock = mock(ChannelHandlerContext.class);
        when(sessionCtxMock.getSessionId()).thenReturn(UUID.fromString("c1bb7594-6009-48ad-bdbd-b2b2f98a08b5"));
        when(sessionCtxMock.getChannel()).thenReturn(ctxMock);
        when(sessionCtxMock.getMqttVersion()).thenReturn(MqttVersion.MQTT_5);

        var enhancedAuthContinueResponse = EnhancedAuthContinueResponse.failure(
                CLIENT_FIRST_MESSAGE_EVALUATION_ERROR);

        doReturn(enhancedAuthContinueResponse).when(enhancedAuthenticationService)
                .onClientConnectMsg(any(ClientSessionCtx.class), any(EnhancedAuthContext.class));

        when(unauthorizedClientService.save(any(UnauthorizedClient.class))).thenReturn(Futures.immediateFuture(null));

        EnhancedAuthInitMsg enhancedAuthInitMsg = getEnhancedAuthInitMsg(sessionCtxMock);
        actorProcessor.onEnhancedAuthInit(clientActorState, enhancedAuthInitMsg);

        verify(sessionCtxMock).getSessionId();
        verify(sessionCtxMock).getAddressBytes();
        verify(sessionCtxMock).getSslHandler();
        verify(sessionCtxMock).getChannel();
        verify(sessionCtxMock).closeChannel();
        verify(sessionCtxMock).getMqttVersion();
        verifyNoMoreInteractions(sessionCtxMock);

        verify(mqttMessageGenerator).createMqttConnAckMsg(eq(CONNECTION_REFUSED_NOT_AUTHORIZED_5));
        verify(ctxMock).writeAndFlush(any());
        verify(unauthorizedClientService).save(any(UnauthorizedClient.class));
        assertThat(clientActorState.getCurrentSessionState()).isEqualTo(SessionState.DISCONNECTED);
    }

    @Test
    public void givenDisconnectedSession_whenOnEnhancedAuthInitAndChallengeStartedAndCurrentSessionStateIsNotDisconnected_thenDisconnectCurrentSession() {
        updateSessionState(SessionState.CONNECTED);

        ClientSessionCtx currentClientSessionCtx = getClientSessionCtx();
        UUID currentSessionId = currentClientSessionCtx.getSessionId();
        clientActorState.setClientSessionCtx(currentClientSessionCtx);

        var mqttMessageMock = mock(MqttMessage.class);
        doReturn(mqttMessageMock).when(mqttMessageGenerator).createMqttAuthMsg(anyString(), any(), any(MqttReasonCodes.Auth.class));

        var enhancedAuthContinueResponse = EnhancedAuthContinueResponse.success("username", "server-first-data".getBytes(StandardCharsets.UTF_8));
        doReturn(enhancedAuthContinueResponse).when(enhancedAuthenticationService)
                .onClientConnectMsg(any(ClientSessionCtx.class), any(EnhancedAuthContext.class));

        ChannelHandlerContext channelHandlerCtxMock = mock(ChannelHandlerContext.class);
        ClientSessionCtx ctxFromEnhancedMsgMock = mock(ClientSessionCtx.class);
        when(ctxFromEnhancedMsgMock.getSessionId()).thenReturn(UUID.fromString("520d28e7-20b5-4ca3-ae7d-8103409ba1b8"));
        when(ctxFromEnhancedMsgMock.getChannel()).thenReturn(channelHandlerCtxMock);
        EnhancedAuthInitMsg enhancedAuthInitMsg = getEnhancedAuthInitMsg(ctxFromEnhancedMsgMock);
        actorProcessor.onEnhancedAuthInit(clientActorState, enhancedAuthInitMsg);

        verify(mqttMessageGenerator).createMqttAuthMsg(anyString(), any(), eq(MqttReasonCodes.Auth.CONTINUE_AUTHENTICATION));
        verify(channelHandlerCtxMock).writeAndFlush(mqttMessageMock);

        var mqttDisconnectMsgCaptor = ArgumentCaptor.forClass(MqttDisconnectMsg.class);
        verify(disconnectService).disconnect(eq(clientActorState), mqttDisconnectMsgCaptor.capture());

        MqttDisconnectMsg mqttDisconnectMsg = mqttDisconnectMsgCaptor.getValue();
        assertThat(mqttDisconnectMsg.getSessionId()).isEqualTo(currentSessionId);
        assertThat(mqttDisconnectMsg.getReason().getType()).isEqualTo(DisconnectReasonType.ON_CONFLICTING_SESSIONS);
        assertThat(mqttDisconnectMsg.getReason().getMessage()).isNull();

        assertThat(clientActorState.getCurrentSessionState()).isEqualTo(SessionState.ENHANCED_AUTH_STARTED);
        assertThat(clientActorState.getCurrentSessionCtx()).isEqualTo(ctxFromEnhancedMsgMock);
    }

    @Test
    public void givenConnectedSession_whenOnEnhancedAuthContinueAndReAuthSucceeds_thenFinishSessionAndClearScramServer() {
        updateSessionState(SessionState.CONNECTED);

        ChannelHandlerContext channelHandlerCtxMock = mock(ChannelHandlerContext.class);
        ClientSessionCtx sessionCtxMock = mock(ClientSessionCtx.class);
        UUID sessionId = UUID.fromString("31e11192-762d-4604-bf60-ac5f1aafedc7");

        when(sessionCtxMock.getChannel()).thenReturn(channelHandlerCtxMock);

        clientActorState.setClientSessionCtx(sessionCtxMock);

        var mqttMessageMock = mock(MqttMessage.class);
        doReturn(mqttMessageMock).when(mqttMessageGenerator).createMqttAuthMsg(anyString(), any(), any(MqttReasonCodes.Auth.class));

        MqttAuthMsg authMsg = getMqttAuthMsg(sessionId);
        EnhancedAuthFinalResponse authResponse = mock(EnhancedAuthFinalResponse.class);
        List<AuthRulePatterns> authorizationRules = getAuthorizationRules();
        when(authResponse.authRulePatterns()).thenReturn(authorizationRules);
        when(authResponse.clientType()).thenReturn(ClientType.DEVICE);
        when(authResponse.success()).thenReturn(true);

        doReturn(authResponse).when(enhancedAuthenticationService)
                .onReAuthContinue(any(ClientSessionCtx.class), any(EnhancedAuthContext.class));

        actorProcessor.onEnhancedAuthContinue(clientActorState, authMsg);

        verify(sessionCtxMock).setAuthRulePatterns(authorizationRules);
        verify(sessionCtxMock).setClientType(ClientType.DEVICE);
        verify(sessionCtxMock).clearScramServer();
        verify(sessionCtxMock).getChannel();
        verifyNoMoreInteractions(sessionCtxMock);
        verifyNoInteractions(clientMqttActorManager);

        verify(mqttMessageGenerator).createMqttAuthMsg(anyString(), any(), eq(MqttReasonCodes.Auth.SUCCESS));
        verify(channelHandlerCtxMock).writeAndFlush(mqttMessageMock);
    }

    @Test
    public void givenConnectedSession_whenOnEnhancedAuthContinueAndReAuthFails_thenDisconnectNotAuthorized() {
        updateSessionState(SessionState.CONNECTED);

        ClientSessionCtx sessionCtxMock = mock(ClientSessionCtx.class);
        UUID sessionId = UUID.fromString("9aa96492-5a3f-4d2a-973b-e08cd0c0f355");
        when(sessionCtxMock.getSessionId()).thenReturn(sessionId);

        clientActorState.setClientSessionCtx(sessionCtxMock);

        MqttAuthMsg authMsg = getMqttAuthMsg(sessionId);
        EnhancedAuthFinalResponse authResponse = mock(EnhancedAuthFinalResponse.class);
        when(authResponse.success()).thenReturn(false);
        when(authResponse.enhancedAuthFailure()).thenReturn(CLIENT_FINAL_MESSAGE_EVALUATION_ERROR);

        when(unauthorizedClientService.save(any(UnauthorizedClient.class))).thenReturn(Futures.immediateFuture(null));

        doReturn(authResponse).when(enhancedAuthenticationService)
                .onReAuthContinue(any(ClientSessionCtx.class), any(EnhancedAuthContext.class));

        actorProcessor.onEnhancedAuthContinue(clientActorState, authMsg);

        var mqttDisconnectMsgCaptor = ArgumentCaptor.forClass(MqttDisconnectMsg.class);
        verify(clientMqttActorManager).disconnect(eq("clientId"), mqttDisconnectMsgCaptor.capture());
        var mqttDisconnectMsg = mqttDisconnectMsgCaptor.getValue();
        assertThat(mqttDisconnectMsg.getSessionId()).isEqualTo(sessionId);
        assertThat(mqttDisconnectMsg.getReason().getType()).isEqualTo(DisconnectReasonType.NOT_AUTHORIZED);
        verify(sessionCtxMock).getSessionId();
        verify(sessionCtxMock).getAddressBytes();
        verify(sessionCtxMock).getSslHandler();
        verifyNoMoreInteractions(sessionCtxMock);
    }

    @Test
    public void givenEnhancedAuthStartedSession_whenOnEnhancedAuthContinueAndAuthSucceeds_thenFinishSessionAndConnect() {
        updateSessionState(SessionState.ENHANCED_AUTH_STARTED);

        var mqttConnectPayloadMock = mock(MqttConnectPayload.class);
        var mqttConnectVariableHeaderMock = mock(MqttConnectVariableHeader.class);
        var mqttConnectMsgFromCtxMock = mock(MqttConnectMessage.class);

        when(mqttConnectMsgFromCtxMock.payload()).thenReturn(mqttConnectPayloadMock);
        when(mqttConnectMsgFromCtxMock.variableHeader()).thenReturn(mqttConnectVariableHeaderMock);

        ClientSessionCtx sessionCtxMock = mock(ClientSessionCtx.class);
        UUID sessionId = UUID.fromString("32177be8-b247-411a-bd57-94bd92ac71f7");
        when(sessionCtxMock.getSessionId()).thenReturn(sessionId);
        when(sessionCtxMock.getConnectMsgFromEnhancedAuth()).thenReturn(mqttConnectMsgFromCtxMock);

        clientActorState.setClientSessionCtx(sessionCtxMock);

        MqttAuthMsg authMsg = getMqttAuthMsg(sessionId);
        EnhancedAuthFinalResponse authResponse = mock(EnhancedAuthFinalResponse.class);
        List<AuthRulePatterns> authorizationRules = getAuthorizationRules();
        when(authResponse.authRulePatterns()).thenReturn(authorizationRules);
        when(authResponse.clientType()).thenReturn(ClientType.DEVICE);
        when(authResponse.success()).thenReturn(true);


        doNothing().when(clientMqttActorManager).connect(any(), any(MqttConnectMsg.class));

        doReturn(authResponse).when(enhancedAuthenticationService)
                .onAuthContinue(any(ClientSessionCtx.class), any(EnhancedAuthContext.class));

        when(unauthorizedClientService.remove(any())).thenReturn(Futures.immediateFuture(null));

        actorProcessor.onEnhancedAuthContinue(clientActorState, authMsg);

        verify(sessionCtxMock).setAuthRulePatterns(authorizationRules);
        verify(sessionCtxMock).setClientType(ClientType.DEVICE);
        verify(sessionCtxMock).getSessionId();
        verify(sessionCtxMock).getConnectMsgFromEnhancedAuth();
        verify(sessionCtxMock).clearScramServer();
        verify(sessionCtxMock).clearConnectMsg();
        verifyNoMoreInteractions(sessionCtxMock);

        verify(clientMqttActorManager).connect(eq("clientId"), any(MqttConnectMsg.class));
        verifyNoMoreInteractions(clientMqttActorManager);
        verify(unauthorizedClientService).remove(UnauthorizedClient.builder().clientId("clientId").build());

        assertThat(clientActorState.getCurrentSessionState()).isEqualTo(SessionState.INITIALIZED);
    }

    @Test
    public void givenEnhancedAuthStartedSession_whenOnEnhancedAuthContinueAndAuthFailsWithMethodMismatch_thenSendBadAuthenticationMethod() {
        updateSessionState(SessionState.ENHANCED_AUTH_STARTED);

        ClientSessionCtx sessionCtxMock = mock(ClientSessionCtx.class);
        ChannelHandlerContext ctxMock = mock(ChannelHandlerContext.class);
        UUID sessionId = UUID.fromString("f71b459f-3011-4ae3-a2c5-e77e3c378be7");
        when(sessionCtxMock.getChannel()).thenReturn(ctxMock);

        clientActorState.setClientSessionCtx(sessionCtxMock);

        MqttAuthMsg authMsg = getMqttAuthMsg(sessionId);
        EnhancedAuthFinalResponse authResponse = mock(EnhancedAuthFinalResponse.class);
        when(authResponse.success()).thenReturn(false);
        when(authResponse.enhancedAuthFailure()).thenReturn(AUTH_METHOD_MISMATCH);

        doReturn(authResponse).when(enhancedAuthenticationService)
                .onAuthContinue(any(ClientSessionCtx.class), any(EnhancedAuthContext.class));

        when(unauthorizedClientService.save(any(UnauthorizedClient.class))).thenReturn(Futures.immediateFuture(null));

        actorProcessor.onEnhancedAuthContinue(clientActorState, authMsg);

        verify(sessionCtxMock).getAddressBytes();
        verify(sessionCtxMock).getSslHandler();
        verify(sessionCtxMock).getChannel();
        verify(sessionCtxMock).closeChannel();
        verifyNoMoreInteractions(sessionCtxMock);

        verify(mqttMessageGenerator).createMqttConnAckMsg(eq(CONNECTION_REFUSED_BAD_AUTHENTICATION_METHOD));
        verify(ctxMock).writeAndFlush(any());
        assertThat(clientActorState.getCurrentSessionState()).isEqualTo(SessionState.DISCONNECTED);
    }

    @Test
    public void givenEnhancedAuthStartedSession_whenOnEnhancedAuthContinueAndAuthFails_thenSendUnspecifiedError() {
        updateSessionState(SessionState.ENHANCED_AUTH_STARTED);

        ClientSessionCtx sessionCtxMock = mock(ClientSessionCtx.class);
        ChannelHandlerContext ctxMock = mock(ChannelHandlerContext.class);
        UUID sessionId = UUID.fromString("f71b459f-3011-4ae3-a2c5-e77e3c378be7");
        when(sessionCtxMock.getChannel()).thenReturn(ctxMock);

        clientActorState.setClientSessionCtx(sessionCtxMock);

        when(unauthorizedClientService.save(any())).thenReturn(Futures.immediateFuture(null));

        MqttAuthMsg authMsg = getMqttAuthMsg(sessionId);
        EnhancedAuthFinalResponse authResponse = mock(EnhancedAuthFinalResponse.class);
        when(authResponse.success()).thenReturn(false);
        when(authResponse.enhancedAuthFailure()).thenReturn(AUTH_CHALLENGE_FAILED);

        doReturn(authResponse).when(enhancedAuthenticationService)
                .onAuthContinue(any(ClientSessionCtx.class), any(EnhancedAuthContext.class));

        actorProcessor.onEnhancedAuthContinue(clientActorState, authMsg);

        verify(sessionCtxMock).getAddressBytes();
        verify(sessionCtxMock).getSslHandler();
        verify(sessionCtxMock).getChannel();
        verify(sessionCtxMock).closeChannel();
        verifyNoMoreInteractions(sessionCtxMock);

        verify(mqttMessageGenerator).createMqttConnAckMsg(eq(CONNECTION_REFUSED_UNSPECIFIED_ERROR));
        verify(ctxMock).writeAndFlush(any());
        verify(unauthorizedClientService).save(any(UnauthorizedClient.class));
        assertThat(clientActorState.getCurrentSessionState()).isEqualTo(SessionState.DISCONNECTED);
    }

    @Test
    public void givenInvalidSessionState_whenOnEnhancedAuthContinue_thenDisconnect() {
        updateSessionState(SessionState.INITIALIZED);

        ClientSessionCtx sessionCtxMock = mock(ClientSessionCtx.class);
        ChannelHandlerContext ctxMock = mock(ChannelHandlerContext.class);
        when(sessionCtxMock.getChannel()).thenReturn(ctxMock);
        when(sessionCtxMock.getMqttVersion()).thenReturn(MqttVersion.MQTT_5);

        clientActorState.setClientSessionCtx(sessionCtxMock);

        when(unauthorizedClientService.save(any())).thenReturn(Futures.immediateFuture(null));

        actorProcessor.onEnhancedAuthContinue(clientActorState, mock(MqttAuthMsg.class));

        verify(sessionCtxMock).getAddressBytes();
        verify(sessionCtxMock).getSslHandler();
        verify(sessionCtxMock).getChannel();
        verify(sessionCtxMock).closeChannel();
        verify(sessionCtxMock).getMqttVersion();
        verifyNoMoreInteractions(sessionCtxMock);

        verify(mqttMessageGenerator).createMqttConnAckMsg(eq(CONNECTION_REFUSED_NOT_AUTHORIZED_5));
        verify(ctxMock).writeAndFlush(any());
        assertThat(clientActorState.getCurrentSessionState()).isEqualTo(SessionState.DISCONNECTED);
    }

    @Test
    public void givenConnectedSession_whenOnEnhancedReAuthAndReAuthSucceeds_thenNoDisconnect() {
        updateSessionState(SessionState.CONNECTED);

        var mqttAuthMsgMock = mock(MqttAuthMsg.class);
        when(mqttAuthMsgMock.getAuthMethod()).thenReturn(ScramAlgorithm.SHA_256.getMqttAlgorithmName());

        var mqttMessageMock = mock(MqttMessage.class);
        doReturn(mqttMessageMock).when(mqttMessageGenerator).createMqttAuthMsg(anyString(), any(), any(MqttReasonCodes.Auth.class));

        var channelHandlerCtxMock = mock(ChannelHandlerContext.class);
        var clientSessionCtxMock = mock(ClientSessionCtx.class);
        when(clientSessionCtxMock.getChannel()).thenReturn(channelHandlerCtxMock);

        clientActorState.setClientSessionCtx(clientSessionCtxMock);

        byte[] expectedAuthData = "server-first-data".getBytes(StandardCharsets.UTF_8);
        when(enhancedAuthenticationService.onReAuth(any(ClientSessionCtx.class), any(EnhancedAuthContext.class)))
                .thenReturn(EnhancedAuthContinueResponse.success("username", expectedAuthData));

        actorProcessor.onEnhancedReAuth(clientActorState, mqttAuthMsgMock);

        verify(enhancedAuthenticationService).onReAuth(eq(clientSessionCtxMock), any(EnhancedAuthContext.class));
        verifyNoInteractions(clientMqttActorManager);
        verify(clientSessionCtxMock).getChannel();

        verify(mqttMessageGenerator).createMqttAuthMsg(ScramAlgorithm.SHA_256.getMqttAlgorithmName(),
                expectedAuthData, MqttReasonCodes.Auth.CONTINUE_AUTHENTICATION);
        verify(channelHandlerCtxMock).writeAndFlush(mqttMessageMock);
    }

    @Test
    public void givenConnectedSession_whenOnEnhancedReAuthAndReAuthFails_thenDisconnectWithNotAuthorizedReason() {
        updateSessionState(SessionState.CONNECTED);

        ClientSessionCtx sessionCtxMock = mock(ClientSessionCtx.class);
        UUID sessionId = UUID.fromString("9aa96492-5a3f-4d2a-973b-e08cd0c0f355");
        when(sessionCtxMock.getSessionId()).thenReturn(sessionId);

        clientActorState.setClientSessionCtx(sessionCtxMock);

        when(unauthorizedClientService.save(any())).thenReturn(Futures.immediateFuture(null));

        var enhancedAuthContinueResponse = EnhancedAuthContinueResponse.failure(CLIENT_RE_AUTH_MESSAGE_EVALUATION_ERROR);
        when(enhancedAuthenticationService.onReAuth(any(ClientSessionCtx.class), any(EnhancedAuthContext.class)))
                .thenReturn(enhancedAuthContinueResponse);

        actorProcessor.onEnhancedReAuth(clientActorState, mock(MqttAuthMsg.class));

        var mqttDisconnectMsgCaptor = ArgumentCaptor.forClass(MqttDisconnectMsg.class);
        verify(clientMqttActorManager).disconnect(eq("clientId"), mqttDisconnectMsgCaptor.capture());
        var mqttDisconnectMsg = mqttDisconnectMsgCaptor.getValue();
        assertThat(mqttDisconnectMsg.getSessionId()).isEqualTo(sessionId);
        assertThat(mqttDisconnectMsg.getReason().getType()).isEqualTo(DisconnectReasonType.NOT_AUTHORIZED);
        assertThat(mqttDisconnectMsg.getReason().getMessage()).isEqualTo(CLIENT_RE_AUTH_MESSAGE_EVALUATION_ERROR.getReasonLog());
        verifyNoMoreInteractions(clientMqttActorManager);
        verify(unauthorizedClientService).save(any(UnauthorizedClient.class));
    }

    @Test
    public void givenNonConnectedSession_whenOnEnhancedReAuth_thenDisconnectWithProtocolError() {
        // Arrange
        updateSessionState(SessionState.CONNECTING);

        ClientSessionCtx sessionCtxMock = mock(ClientSessionCtx.class);
        UUID sessionId = UUID.fromString("0b2c909a-a54a-4906-a210-09e4a5742037");
        when(sessionCtxMock.getSessionId()).thenReturn(sessionId);

        clientActorState.setClientSessionCtx(sessionCtxMock);

        actorProcessor.onEnhancedReAuth(clientActorState, mock(MqttAuthMsg.class));

        var mqttDisconnectMsgCaptor = ArgumentCaptor.forClass(MqttDisconnectMsg.class);
        verify(clientMqttActorManager).disconnect(eq("clientId"), mqttDisconnectMsgCaptor.capture());
        var mqttDisconnectMsg = mqttDisconnectMsgCaptor.getValue();
        assertThat(mqttDisconnectMsg.getSessionId()).isEqualTo(sessionId);
        assertThat(mqttDisconnectMsg.getReason().getType()).isEqualTo(DisconnectReasonType.ON_PROTOCOL_ERROR);
        verifyNoMoreInteractions(clientMqttActorManager);
        verifyNoInteractions(enhancedAuthenticationService);
    }

    private AuthResponse getAuthResponse(boolean success) {
        return new AuthResponse(success, ClientType.APPLICATION, getAuthorizationRules());
    }

    private List<AuthRulePatterns> getAuthorizationRules() {
        return List.of(AuthRulePatterns.newInstance(List.of(Pattern.compile("test"))));
    }

    private MqttDisconnectMsg getDisconnectMsg() {
        return new MqttDisconnectMsg(UUID.randomUUID(), new DisconnectReason(DisconnectReasonType.ON_DISCONNECT_MSG));
    }

    private SessionInitMsg getSessionInitMsg(ClientSessionCtx clientSessionCtx) {
        return new SessionInitMsg(clientSessionCtx, "userName", "password".getBytes(StandardCharsets.UTF_8));
    }

    private EnhancedAuthInitMsg getEnhancedAuthInitMsg(ClientSessionCtx clientSessionCtx) {
        return new EnhancedAuthInitMsg(clientSessionCtx, ScramAlgorithm.SHA_256.getMqttAlgorithmName(), "client-initial-data".getBytes(StandardCharsets.UTF_8));
    }

    private MqttAuthMsg getMqttAuthMsg(UUID sessionId) {
        return new MqttAuthMsg(sessionId, ScramAlgorithm.SHA_512.getMqttAlgorithmName(),
                "client-final-data".getBytes(StandardCharsets.UTF_8), MqttReasonCodes.Auth.CONTINUE_AUTHENTICATION);
    }

    private ClientSessionCtx getClientSessionCtx() {
        ClientSessionCtx clientSessionCtx = new ClientSessionCtx();
        clientSessionCtx.setAddress(new InetSocketAddress("localhost", 1883));
        return clientSessionCtx;
    }

    private void updateSessionState(SessionState state) {
        clientActorState.updateSessionState(state);
    }
}