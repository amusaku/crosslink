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
package org.thingsboard.mqtt.broker.adaptor;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import io.netty.handler.codec.mqtt.MqttProperties;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.mqtt.broker.common.data.BrokerConstants;
import org.thingsboard.mqtt.broker.common.data.ClientInfo;
import org.thingsboard.mqtt.broker.common.data.ClientSessionInfo;
import org.thingsboard.mqtt.broker.common.data.ClientType;
import org.thingsboard.mqtt.broker.common.data.ConnectionInfo;
import org.thingsboard.mqtt.broker.common.data.DevicePublishMsg;
import org.thingsboard.mqtt.broker.common.data.PersistedPacketType;
import org.thingsboard.mqtt.broker.common.data.SessionInfo;
import org.thingsboard.mqtt.broker.common.data.subscription.ClientTopicSubscription;
import org.thingsboard.mqtt.broker.common.data.subscription.SubscriptionOptions;
import org.thingsboard.mqtt.broker.common.data.subscription.TopicSubscription;
import org.thingsboard.mqtt.broker.gen.queue.ClientSubscriptionsProto;
import org.thingsboard.mqtt.broker.gen.queue.DevicePublishMsgProto;
import org.thingsboard.mqtt.broker.gen.queue.MqttPropertiesProto;
import org.thingsboard.mqtt.broker.gen.queue.PublishMsgProto;
import org.thingsboard.mqtt.broker.gen.queue.RetainHandling;
import org.thingsboard.mqtt.broker.gen.queue.RetainedMsgProto;
import org.thingsboard.mqtt.broker.gen.queue.SessionInfoProto;
import org.thingsboard.mqtt.broker.gen.queue.TopicSubscriptionProto;
import org.thingsboard.mqtt.broker.service.mqtt.PublishMsg;
import org.thingsboard.mqtt.broker.service.mqtt.retain.RetainedMsg;
import org.thingsboard.mqtt.broker.service.subscription.Subscription;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ProtoConverterTest {

    @Test
    public void givenSessionInfo_whenCheckForPersistence_thenOk() {
        SessionInfo sessionInfo = newSessionInfo(true, 5);
        Assert.assertTrue(sessionInfo.isPersistent());
        sessionInfo = newSessionInfo(false, 5);
        Assert.assertTrue(sessionInfo.isPersistent());
        sessionInfo = newSessionInfo(true, 0);
        Assert.assertFalse(sessionInfo.isPersistent());
        sessionInfo = newSessionInfo(false, 0);
        Assert.assertTrue(sessionInfo.isPersistent());
        sessionInfo = newSessionInfo(true, -1);
        Assert.assertFalse(sessionInfo.isPersistent());
        sessionInfo = newSessionInfo(false, -1);
        Assert.assertTrue(sessionInfo.isPersistent());

        sessionInfo = newSessionInfo(true, 0);
        Assert.assertTrue(sessionInfo.isCleanSession());
        sessionInfo = newSessionInfo(true, -1);
        Assert.assertTrue(sessionInfo.isCleanSession());
        sessionInfo = newSessionInfo(true, 5);
        Assert.assertFalse(sessionInfo.isCleanSession());

        sessionInfo = newSessionInfo(false, 0);
        Assert.assertTrue(sessionInfo.isNotCleanSession());
        sessionInfo = newSessionInfo(false, -1);
        Assert.assertTrue(sessionInfo.isNotCleanSession());
        sessionInfo = newSessionInfo(false, 5);
        Assert.assertFalse(sessionInfo.isNotCleanSession());
    }

    @Test
    public void givenSessionInfo_whenConvertToProtoAndBackWithSessionExpiryIntervalNull_thenOk() {
        SessionInfo sessionInfoConverted = convertSessionInfo(-1);
        Assert.assertEquals(-1, sessionInfoConverted.getSessionExpiryInterval());
    }

    @Test
    public void givenSessionInfo_whenConvertToProtoAndBackWithSessionExpiryIntervalNotNull_thenOk() {
        SessionInfo sessionInfoConverted = convertSessionInfo(5);
        Assert.assertEquals(5, sessionInfoConverted.getSessionExpiryInterval());
    }

    private static SessionInfo convertSessionInfo(int sessionExpiryInterval) {
        SessionInfo sessionInfo = newSessionInfo(true, sessionExpiryInterval);

        SessionInfoProto sessionInfoProto = ProtoConverter.convertToSessionInfoProto(sessionInfo);
        SessionInfo sessionInfoConverted = ProtoConverter.convertToSessionInfo(sessionInfoProto);

        Assert.assertEquals(sessionInfo, sessionInfoConverted);
        return sessionInfoConverted;
    }

    private static SessionInfo newSessionInfo(boolean cleanStart, int sessionExpiryInterval) {
        return SessionInfo.builder()
                .serviceId("serviceId")
                .sessionId(UUID.randomUUID())
                .cleanStart(cleanStart)
                .sessionExpiryInterval(sessionExpiryInterval)
                .clientInfo(ClientInfo.builder()
                        .clientId("clientId")
                        .type(ClientType.DEVICE)
                        .clientIpAdr(BrokerConstants.LOCAL_ADR)
                        .build())
                .connectionInfo(ConnectionInfo.builder()
                        .connectedAt(10)
                        .disconnectedAt(20)
                        .keepAlive(100)
                        .build())
                .build();
    }

    @Test
    public void givenTopicSubscriptions_whenConvertToProtoAndBack_thenOk() {
        Set<TopicSubscription> input = Set.of(
                new ClientTopicSubscription("topic1", 0, "name1"),
                new ClientTopicSubscription("topic2", 1),
                new ClientTopicSubscription(
                        "topic3",
                        2,
                        null,
                        new SubscriptionOptions(
                                true,
                                true,
                                SubscriptionOptions.RetainHandlingPolicy.DONT_SEND_AT_SUBSCRIBE),
                        1)
        );

        ClientSubscriptionsProto clientSubscriptionsProto = ProtoConverter.convertToClientSubscriptionsProto(input);

        List<TopicSubscriptionProto> sortedList = clientSubscriptionsProto.getSubscriptionsList().stream().sorted(Comparator.comparing(TopicSubscriptionProto::getTopic)).toList();

        TopicSubscriptionProto subscription0 = sortedList.get(0);
        assertEquals("topic1", subscription0.getTopic());
        assertEquals(0, subscription0.getQos());
        assertEquals("name1", subscription0.getShareName());
        assertFalse(subscription0.hasSubscriptionId());
        assertFalse(subscription0.getOptions().getNoLocal());
        assertFalse(subscription0.getOptions().getRetainAsPublish());
        assertEquals(RetainHandling.SEND, subscription0.getOptions().getRetainHandling());

        TopicSubscriptionProto subscription2 = sortedList.get(2);
        assertEquals("topic3", subscription2.getTopic());
        assertEquals(2, subscription2.getQos());
        assertFalse(subscription2.hasShareName());
        assertTrue(subscription2.hasSubscriptionId());
        assertEquals(1, subscription2.getSubscriptionId());
        assertTrue(subscription2.getOptions().getNoLocal());
        assertTrue(subscription2.getOptions().getRetainAsPublish());
        assertEquals(RetainHandling.DONT_SEND, subscription2.getOptions().getRetainHandling());

        TopicSubscriptionProto topicSubscriptionProto = clientSubscriptionsProto.getSubscriptionsList()
                .stream()
                .filter(tsp -> tsp.getShareName().isEmpty())
                .findFirst()
                .orElse(null);
        assertNotNull(topicSubscriptionProto);

        Set<TopicSubscription> output = ProtoConverter.convertProtoToClientSubscriptions(clientSubscriptionsProto);

        TopicSubscription topicSubscription = output
                .stream()
                .filter(ts -> ts.getShareName() == null)
                .findFirst()
                .orElse(null);
        assertNotNull(topicSubscription);

        assertEquals(input, output);
        assertEquals(input.size(), output.size());
        assertEquals(3, input.size());

        List<TopicSubscription> topicSubscriptionList = output.stream().sorted(Comparator.comparing(TopicSubscription::getTopicFilter)).toList();

        TopicSubscription topicSubscription0 = topicSubscriptionList.get(0);
        assertEquals("topic1", topicSubscription0.getTopicFilter());
        assertEquals(-1, topicSubscription0.getSubscriptionId());

        TopicSubscription topicSubscription2 = topicSubscriptionList.get(2);
        assertEquals("topic3", topicSubscription2.getTopicFilter());
        assertEquals(1, topicSubscription2.getSubscriptionId());
        assertTrue(topicSubscription2.getOptions().isNoLocal());
        assertTrue(topicSubscription2.getOptions().isRetainAsPublish());
        assertEquals(SubscriptionOptions.RetainHandlingPolicy.DONT_SEND_AT_SUBSCRIBE, topicSubscription2.getOptions().getRetainHandling());
    }

    @Test
    public void givenMqttPropertiesProtoAndEmptyProps_whenAddFromProtoToMqttProperties_thenPropsNotEmpty() {
        MqttPropertiesProto build = MqttPropertiesProto
                .newBuilder()
                .setContentType("testCT")
                .setPayloadFormatIndicator(1)
                .setCorrelationData(ByteString.copyFromUtf8("test"))
                .setResponseTopic("test/")
                .build();
        MqttProperties properties = new MqttProperties();

        assertNull(properties.getProperty(BrokerConstants.PAYLOAD_FORMAT_INDICATOR_PROP_ID));
        assertNull(properties.getProperty(BrokerConstants.CONTENT_TYPE_PROP_ID));
        assertNull(properties.getProperty(BrokerConstants.RESPONSE_TOPIC_PROP_ID));
        assertNull(properties.getProperty(BrokerConstants.CORRELATION_DATA_PROP_ID));

        ProtoConverter.addFromProtoToMqttProperties(build, properties);

        assertNotNull(properties.getProperty(BrokerConstants.PAYLOAD_FORMAT_INDICATOR_PROP_ID));
        assertNotNull(properties.getProperty(BrokerConstants.CONTENT_TYPE_PROP_ID));
        assertNotNull(properties.getProperty(BrokerConstants.RESPONSE_TOPIC_PROP_ID));
        assertNotNull(properties.getProperty(BrokerConstants.CORRELATION_DATA_PROP_ID));
    }

    @Test
    public void givenMqttPropertiesWithPayloadFormatIndicatorAndContentType_whenGetMqttPropsProtoBuilder_thenGetExpectedResult() {
        MqttProperties properties = new MqttProperties();
        properties.add(new MqttProperties.IntegerProperty(BrokerConstants.PAYLOAD_FORMAT_INDICATOR_PROP_ID, 1));
        properties.add(new MqttProperties.StringProperty(BrokerConstants.CONTENT_TYPE_PROP_ID, "test"));

        MqttPropertiesProto.Builder mqttPropsProtoBuilder = ProtoConverter.getMqttPropsProtoBuilder(properties);

        assertNotNull(mqttPropsProtoBuilder);
        MqttPropertiesProto proto = mqttPropsProtoBuilder.build();
        assertEquals("test", proto.getContentType());
        assertEquals(1, proto.getPayloadFormatIndicator());
    }

    @Test
    public void givenMqttPropertiesWithResponseTopicAndCorrelationData_whenGetMqttPropsProtoBuilder_thenGetExpectedResult() {
        MqttProperties properties = new MqttProperties();
        properties.add(new MqttProperties.BinaryProperty(BrokerConstants.CORRELATION_DATA_PROP_ID, BrokerConstants.DUMMY_PAYLOAD));
        properties.add(new MqttProperties.StringProperty(BrokerConstants.RESPONSE_TOPIC_PROP_ID, "test/"));

        MqttPropertiesProto.Builder mqttPropsProtoBuilder = ProtoConverter.getMqttPropsProtoBuilder(properties);

        assertNotNull(mqttPropsProtoBuilder);
        MqttPropertiesProto proto = mqttPropsProtoBuilder.build();
        assertEquals("test/", proto.getResponseTopic());
        assertEquals("test", proto.getCorrelationData().toString(StandardCharsets.UTF_8));
    }

    @Test
    public void givenMqttPropertiesWithPayloadFormatIndicatorAndNoContentType_whenGetMqttPropsProtoBuilder_thenGetExpectedResult() {
        MqttProperties properties = new MqttProperties();
        properties.add(new MqttProperties.IntegerProperty(BrokerConstants.PAYLOAD_FORMAT_INDICATOR_PROP_ID, 1));

        MqttPropertiesProto.Builder mqttPropsProtoBuilder = ProtoConverter.getMqttPropsProtoBuilder(properties);

        assertNotNull(mqttPropsProtoBuilder);
        MqttPropertiesProto proto = mqttPropsProtoBuilder.build();
        assertFalse(proto.hasContentType());
        assertEquals(1, proto.getPayloadFormatIndicator());
    }

    @Test
    public void givenMqttPropertiesWithNoPayloadFormatIndicatorAndNoContentType_whenGetMqttPropsProtoBuilder_thenGetExpectedResult() {
        MqttPropertiesProto.Builder mqttPropsProtoBuilder = ProtoConverter.getMqttPropsProtoBuilder(new MqttProperties());
        assertNull(mqttPropsProtoBuilder);
    }

    @Test
    public void givenRetainedMsg_whenConvertToRetainedMsgProto_thenGetExpectedRetainedMsgProto() {
        MqttProperties properties = new MqttProperties();
        RetainedMsg retainedMsg = new RetainedMsg("t", "p".getBytes(StandardCharsets.UTF_8), 1, properties);

        RetainedMsgProto retainedMsgProto = ProtoConverter.convertToRetainedMsgProto(retainedMsg);
        assertEquals(1, retainedMsgProto.getQos());
        assertEquals("t", retainedMsgProto.getTopic());
        assertFalse(retainedMsgProto.hasMqttProperties());
        assertEquals(0, retainedMsgProto.getUserPropertiesCount());
    }

    @Test
    public void givenRetainedMsgWithPresentMqttProperties_whenConvertToRetainedMsgProto_thenGetExpectedRetainedMsgProto() {
        MqttProperties properties = new MqttProperties();
        properties.add(new MqttProperties.IntegerProperty(BrokerConstants.PAYLOAD_FORMAT_INDICATOR_PROP_ID, 0));
        properties.add(new MqttProperties.UserProperty("key", "value"));
        RetainedMsg retainedMsg = new RetainedMsg("t", "p".getBytes(StandardCharsets.UTF_8), 1, properties);

        RetainedMsgProto retainedMsgProto = ProtoConverter.convertToRetainedMsgProto(retainedMsg);
        assertEquals(1, retainedMsgProto.getQos());
        assertEquals("t", retainedMsgProto.getTopic());
        assertTrue(retainedMsgProto.hasMqttProperties());
        assertEquals(1, retainedMsgProto.getUserPropertiesCount());
    }

    @Test
    public void givenRetainedMsgProto_whenConvertProtoToRetainedMsg_thenGetExpectedRetainedMsg() {
        RetainedMsgProto proto = RetainedMsgProto
                .newBuilder()
                .setMqttProperties(MqttPropertiesProto.newBuilder().setPayloadFormatIndicator(1).build())
                .setQos(1)
                .setPayload(ByteString.copyFromUtf8("payload"))
                .setCreatedTime(System.currentTimeMillis())
                .setTopic("topic")
                .build();

        RetainedMsg retainedMsg = ProtoConverter.convertProtoToRetainedMsg(proto);
        assertEquals("topic", retainedMsg.getTopic());
        assertEquals(1, retainedMsg.getQos());
        assertNotNull(retainedMsg.getProperties().getProperty(BrokerConstants.PAYLOAD_FORMAT_INDICATOR_PROP_ID));
        assertNull(retainedMsg.getProperties().getProperty(BrokerConstants.CONTENT_TYPE_PROP_ID));
    }

    @Test
    public void givenDevicePubMsgProto_whenExecuteProtoToDevicePublishMsg_thenGetExpectedDevicePubMsg() {
        DevicePublishMsgProto proto = DevicePublishMsgProto
                .newBuilder()
                .setMqttProperties(MqttPropertiesProto.newBuilder().setContentType("testCT").build())
                .setQos(1)
                .setPacketType("PUBLISH")
                .setPayload(ByteString.copyFromUtf8("payload"))
                .setClientId("clientId")
                .setTopicName("topic")
                .build();

        DevicePublishMsg devicePublishMsg = ProtoConverter.protoToDevicePublishMsg(proto);
        assertEquals("topic", devicePublishMsg.getTopicName());
        assertEquals(1, devicePublishMsg.getQos());
        assertEquals(PersistedPacketType.PUBLISH, devicePublishMsg.getPacketType());
        assertNull(devicePublishMsg.getProperties().getProperty(BrokerConstants.PAYLOAD_FORMAT_INDICATOR_PROP_ID));
        assertNotNull(devicePublishMsg.getProperties().getProperty(BrokerConstants.CONTENT_TYPE_PROP_ID));
    }

    @Test
    public void givenDevicePubMsg_whenExecuteToDevicePublishMsgProto_thenGetExpectedDevicePubMsgProto() {
        MqttProperties properties = new MqttProperties();
        properties.add(new MqttProperties.IntegerProperty(BrokerConstants.PAYLOAD_FORMAT_INDICATOR_PROP_ID, 0));
        properties.add(new MqttProperties.UserProperty("key", "value"));

        DevicePublishMsg devicePublishMsg = DevicePublishMsg.builder()
                .properties(properties)
                .qos(0)
                .time(123213L)
                .payload("p".getBytes(StandardCharsets.UTF_8))
                .packetType(PersistedPacketType.PUBREL)
                .clientId("cli")
                .topicName("topic")
                .packetId(124)
                .build();

        DevicePublishMsgProto devicePublishMsgProto = ProtoConverter.toDevicePublishMsgProto(devicePublishMsg);
        assertEquals(0, devicePublishMsgProto.getQos());
        assertEquals("topic", devicePublishMsgProto.getTopicName());
        assertTrue(devicePublishMsgProto.hasMqttProperties());
        assertEquals(0, devicePublishMsgProto.getMqttProperties().getPayloadFormatIndicator());
        assertEquals(1, devicePublishMsgProto.getUserPropertiesCount());
    }

    @Test
    public void givenPubMsg_whenExecuteConvertToPublishMsgProto_thenGetExpectedPubMsgProto() {
        MqttProperties properties = new MqttProperties();
        properties.add(new MqttProperties.IntegerProperty(BrokerConstants.PAYLOAD_FORMAT_INDICATOR_PROP_ID, 0));
        properties.add(new MqttProperties.UserProperty("key", "value"));
        properties.add(new MqttProperties.UserProperty("foo", "bar"));

        SessionInfo sessionInfo = SessionInfo.builder().clientInfo(ClientInfo.builder().clientId("cli").build()).build();
        PublishMsg publishMsg = PublishMsg.builder()
                .topicName("topic")
                .packetId(1)
                .qos(2)
                .properties(properties)
                .payload("p".getBytes(StandardCharsets.UTF_8))
                .build();
        PublishMsgProto publishMsgProto = ProtoConverter.convertToPublishMsgProto(sessionInfo, publishMsg);

        assertEquals(2, publishMsgProto.getQos());
        assertEquals("topic", publishMsgProto.getTopicName());
        assertTrue(publishMsgProto.hasMqttProperties());
        assertEquals(0, publishMsgProto.getMqttProperties().getPayloadFormatIndicator());
        assertEquals(2, publishMsgProto.getUserPropertiesCount());
    }

    @Test
    public void givenPubMsgProto_whenExecuteConvertToPublishMsg_thenGetExpectedPubMsg() {
        PublishMsgProto proto = PublishMsgProto.newBuilder()
                .setTopicName("t")
                .setPayload(ByteString.copyFromUtf8("p"))
                .setMqttProperties(MqttPropertiesProto.newBuilder().setPayloadFormatIndicator(1).build())
                .build();
        PublishMsg publishMsg = ProtoConverter.convertToPublishMsg(proto, 1, 1, false, -1);

        assertEquals("t", publishMsg.getTopicName());
        assertEquals(1, publishMsg.getPacketId());
        assertEquals(1, publishMsg.getQos());
        assertNotNull(publishMsg.getProperties().getProperty(BrokerConstants.PAYLOAD_FORMAT_INDICATOR_PROP_ID));
        assertNull(publishMsg.getProperties().getProperty(BrokerConstants.CONTENT_TYPE_PROP_ID));
        assertNull(publishMsg.getProperties().getProperty(BrokerConstants.SUBSCRIPTION_IDENTIFIER_PROP_ID));
    }

    @Test
    public void givenPubMsgAndSubscriptionWithSameQosAndFalseRetainAsPublish_whenProcessUpdatePublishMsg_thenReturnSameMsg() {
        Subscription subscription = new Subscription("test/topic", 1, ClientSessionInfo.builder().build());
        PublishMsgProto beforePublishMsgProto = PublishMsgProto.newBuilder().setQos(1).setRetain(false).build();

        PublishMsgProto afterPublishMsgProto = ProtoConverter.updatePublishMsg(subscription, beforePublishMsgProto);

        Assert.assertEquals(beforePublishMsgProto, afterPublishMsgProto);
    }

    @Test
    public void givenPubMsgAndSubscriptionWithDifferentQos_whenProcessUpdatePublishMsg_thenReturnUpdatedMsgWithMinQos() {
        Subscription subscription = new Subscription("test/topic", 1, ClientSessionInfo.builder().build());
        PublishMsgProto beforePublishMsgProto = PublishMsgProto.newBuilder().setQos(2).setRetain(true).build();

        PublishMsgProto afterPublishMsgProto = ProtoConverter.updatePublishMsg(subscription, beforePublishMsgProto);

        Assert.assertNotEquals(beforePublishMsgProto, afterPublishMsgProto);
        Assert.assertEquals(1, afterPublishMsgProto.getQos());
        Assert.assertFalse(afterPublishMsgProto.getRetain());
    }

    @Test
    public void givenPubMsgAndSubscriptionWithSameQosAndRetainAsPublish_whenProcessUpdatePublishMsg_thenReturnSameMsg() {
        Subscription subscription = new Subscription(
                "test/topic",
                2,
                ClientSessionInfo.builder().build(),
                null,
                new SubscriptionOptions(
                        false,
                        true,
                        SubscriptionOptions.RetainHandlingPolicy.SEND_AT_SUBSCRIBE));

        PublishMsgProto beforePublishMsgProto = PublishMsgProto.newBuilder().setQos(2).setRetain(true).build();

        PublishMsgProto afterPublishMsgProto = ProtoConverter.updatePublishMsg(subscription, beforePublishMsgProto);

        Assert.assertEquals(beforePublishMsgProto, afterPublishMsgProto);
        Assert.assertEquals(2, afterPublishMsgProto.getQos());
        Assert.assertTrue(afterPublishMsgProto.getRetain());
    }

    @Test
    public void givenPubMsgAndSubsWithIds_whenProcessUpdatePublishMsg_thenReturnUpdatedMsgWithIds() {
        Subscription subscription = new Subscription("test/topic", 1, Lists.newArrayList(1, 2, 3));
        PublishMsgProto beforePublishMsgProto = PublishMsgProto.newBuilder().setQos(2).setRetain(false).build();

        PublishMsgProto afterPublishMsgProto = ProtoConverter.updatePublishMsg(subscription, beforePublishMsgProto);

        Assert.assertEquals(1, afterPublishMsgProto.getQos());
        Assert.assertEquals(List.of(1, 2, 3), afterPublishMsgProto.getMqttProperties().getSubscriptionIdsList());
    }

    @Test
    public void givenPubMsgWithPropsAndSubsWithIds_whenProcessUpdatePublishMsg_thenReturnUpdatedMsgWithIds() {
        Subscription subscription = new Subscription("test/topic", 1, Lists.newArrayList(1, 2, 3));
        MqttPropertiesProto mqttPropertiesProto = MqttPropertiesProto.newBuilder().setContentType("test").build();
        PublishMsgProto beforePublishMsgProto = PublishMsgProto.newBuilder().setQos(2).setRetain(false).setMqttProperties(mqttPropertiesProto).build();

        PublishMsgProto afterPublishMsgProto = ProtoConverter.updatePublishMsg(subscription, beforePublishMsgProto);

        Assert.assertEquals(1, afterPublishMsgProto.getQos());
        Assert.assertEquals(List.of(1, 2, 3), afterPublishMsgProto.getMqttProperties().getSubscriptionIdsList());
        Assert.assertEquals("test", afterPublishMsgProto.getMqttProperties().getContentType());
    }

    @Test
    public void givenPubMsgWithPropsAndSubsWithIds_whenProcessCreateReceiverPublishMsg_thenReturnUpdatedMsgWithIds() {
        Subscription subscription = new Subscription("test/topic", 1, Lists.newArrayList(1, 2, 3));
        MqttPropertiesProto mqttPropertiesProto = MqttPropertiesProto.newBuilder().setContentType("test").build();
        PublishMsgProto beforePublishMsgProto = PublishMsgProto.newBuilder().setQos(2).setPacketId(1).setRetain(false).setMqttProperties(mqttPropertiesProto).build();

        PublishMsgProto afterPublishMsgProto = ProtoConverter.createReceiverPublishMsg(subscription, beforePublishMsgProto);

        Assert.assertEquals(1, afterPublishMsgProto.getQos());
        Assert.assertEquals(0, afterPublishMsgProto.getPacketId());
        Assert.assertEquals(List.of(1, 2, 3), afterPublishMsgProto.getMqttProperties().getSubscriptionIdsList());
        Assert.assertEquals("test", afterPublishMsgProto.getMqttProperties().getContentType());
    }
}
