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
package org.thingsboard.mqtt.broker.actors.client.service.handlers;

import io.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.mqtt.broker.service.mqtt.MqttMessageGenerator;
import org.thingsboard.mqtt.broker.session.AwaitingPubRelPacketsCtx;
import org.thingsboard.mqtt.broker.session.ClientSessionCtx;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MqttPubRelHandlerTest {

    MqttMessageGenerator mqttMessageGenerator;
    MqttPubRelHandler mqttPubRelHandler;
    ClientSessionCtx ctx;

    @Before
    public void setUp() {
        mqttMessageGenerator = mock(MqttMessageGenerator.class);
        mqttPubRelHandler = spy(new MqttPubRelHandler(mqttMessageGenerator));

        ctx = mock(ClientSessionCtx.class);
        ChannelHandlerContext handlerContext = mock(ChannelHandlerContext.class);
        when(ctx.getChannel()).thenReturn(handlerContext);
        when(ctx.getAwaitingPubRelPacketsCtx()).thenReturn(new AwaitingPubRelPacketsCtx());
    }

    @Test
    public void testProcess() {
        mqttPubRelHandler.process(ctx, 1);
        verify(mqttMessageGenerator, times(1)).createPubCompMsg(eq(1), eq(null));
        verify(mqttPubRelHandler, times(1)).completePubRel(eq(ctx), eq(1));
    }
}