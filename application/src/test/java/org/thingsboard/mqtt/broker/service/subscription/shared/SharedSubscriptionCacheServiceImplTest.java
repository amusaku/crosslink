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
package org.thingsboard.mqtt.broker.service.subscription.shared;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.mqtt.broker.common.data.ClientSessionInfo;
import org.thingsboard.mqtt.broker.common.data.subscription.ClientTopicSubscription;
import org.thingsboard.mqtt.broker.common.data.subscription.SubscriptionOptions;
import org.thingsboard.mqtt.broker.service.mqtt.client.session.ClientSessionCache;
import org.thingsboard.mqtt.broker.service.subscription.Subscription;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SharedSubscriptionCacheServiceImplTest {

    private static final String CLIENT_ID_1 = "clientId1";
    private static final String CLIENT_ID_2 = "clientId2";
    private static final String CLIENT_ID_3 = "clientId3";

    ClientSessionCache clientSessionCache;
    SharedSubscriptionCacheServiceImpl sharedSubscriptionCache;

    ClientSessionInfo clientSessionInfo1;
    ClientSessionInfo clientSessionInfo2;

    @Before
    public void setUp() {
        clientSessionCache = mock(ClientSessionCache.class);
        sharedSubscriptionCache = spy(new SharedSubscriptionCacheServiceImpl(clientSessionCache));

        clientSessionInfo1 = mock(ClientSessionInfo.class);
        clientSessionInfo2 = mock(ClientSessionInfo.class);

        when(clientSessionCache.getClientSessionInfo(CLIENT_ID_1)).thenReturn(clientSessionInfo1);
        when(clientSessionCache.getClientSessionInfo(CLIENT_ID_2)).thenReturn(clientSessionInfo2);

        when(clientSessionInfo1.getClientId()).thenReturn(CLIENT_ID_1);
        when(clientSessionInfo2.getClientId()).thenReturn(CLIENT_ID_2);

        when(clientSessionInfo1.isAppClient()).thenReturn(true);
        when(clientSessionInfo2.isAppClient()).thenReturn(true);

        when(clientSessionInfo1.isConnected()).thenReturn(true);
        when(clientSessionInfo2.isConnected()).thenReturn(false);
    }

    @After
    public void destroy() {
        sharedSubscriptionCache.getSharedSubscriptionsMap().clear();
    }

    @Test
    public void givenNonSharedSubscriptions_whenPutSubscriptions_thenNothingAdded() {
        sharedSubscriptionCache.put(CLIENT_ID_1, List.of(
                new ClientTopicSubscription("/test/topic/1", 1),
                new ClientTopicSubscription("/test/topic/2", 1),
                new ClientTopicSubscription("/test/topic/3", 1)
        ));

        sharedSubscriptionCache.put(CLIENT_ID_2, List.of(
                new ClientTopicSubscription("/test/topic/4", 2),
                new ClientTopicSubscription("/test/topic/5", 2),
                new ClientTopicSubscription("/test/topic/6", 2)
        ));

        assertEquals(0, sharedSubscriptionCache.getSharedSubscriptionsMap().size());
    }

    @Test
    public void givenDifferentClientsAndSubscriptions_whenPutSubscriptions_thenAddedSuccessfully() {
        sharedSubscriptionCache.put(CLIENT_ID_1, List.of(
                new ClientTopicSubscription("/test/topic/1", 1, "g1"),
                new ClientTopicSubscription("/test/topic/2", 1, "g2"),
                new ClientTopicSubscription("/test/topic/3", 1)
        ));

        assertEquals(2, sharedSubscriptionCache.getSharedSubscriptionsMap().size());

        SharedSubscriptions sharedSubscriptions = sharedSubscriptionCache.getSharedSubscriptionsMap()
                .get(new TopicSharedSubscription("/test/topic/1", "g1"));
        assertTrue(sharedSubscriptions.getDeviceSubscriptions().isEmpty());
        assertEquals(1, sharedSubscriptions.getApplicationSubscriptions().size());

        sharedSubscriptions = sharedSubscriptionCache.getSharedSubscriptionsMap()
                .get(new TopicSharedSubscription("/test/topic/2", "g2"));
        assertTrue(sharedSubscriptions.getDeviceSubscriptions().isEmpty());
        assertEquals(1, sharedSubscriptions.getApplicationSubscriptions().size());

        sharedSubscriptionCache.put(CLIENT_ID_2, List.of(
                new ClientTopicSubscription("/test/topic/1", 2, "g1"),
                new ClientTopicSubscription("/test/topic/2", 0, "g2"),
                new ClientTopicSubscription("/test/topic/3", 1)
        ));

        assertEquals(2, sharedSubscriptionCache.getSharedSubscriptionsMap().size());

        sharedSubscriptions = sharedSubscriptionCache.getSharedSubscriptionsMap()
                .get(new TopicSharedSubscription("/test/topic/2", "g2"));
        assertTrue(sharedSubscriptions.getDeviceSubscriptions().isEmpty());
        assertEquals(2, sharedSubscriptions.getApplicationSubscriptions().size());
    }

    @Test
    public void givenSameClientAndItsSubscriptions_whenExecutePut_thenAddedSuccessfully() {
        when(clientSessionInfo1.isAppClient()).thenReturn(false);

        sharedSubscriptionCache.put(CLIENT_ID_1, List.of(
                new ClientTopicSubscription("/test/topic/1", 1, "g1")
        ));

        assertEquals(1, sharedSubscriptionCache.getSharedSubscriptionsMap().size());

        SharedSubscriptions sharedSubscriptions = sharedSubscriptionCache.getSharedSubscriptionsMap()
                .get(new TopicSharedSubscription("/test/topic/1", "g1"));
        assertTrue(sharedSubscriptions.getApplicationSubscriptions().isEmpty());
        assertEquals(1, sharedSubscriptions.getDeviceSubscriptions().size());
        assertEquals(1, sharedSubscriptions.getDeviceSubscriptions().stream().toList().get(0).getQos());

        sharedSubscriptionCache.put(CLIENT_ID_1, List.of(
                new ClientTopicSubscription("/test/topic/1", 2, "g1")
        ));

        assertEquals(1, sharedSubscriptionCache.getSharedSubscriptionsMap().size());

        sharedSubscriptions = sharedSubscriptionCache.getSharedSubscriptionsMap()
                .get(new TopicSharedSubscription("/test/topic/1", "g1"));
        assertTrue(sharedSubscriptions.getApplicationSubscriptions().isEmpty());
        assertEquals(1, sharedSubscriptions.getDeviceSubscriptions().size());
        assertEquals(2, sharedSubscriptions.getDeviceSubscriptions().stream().toList().get(0).getQos());
    }

    @Test
    public void givenSubscriptions_whenExecuteRemove_thenUpdatedSuccessfully() {
        when(clientSessionInfo1.isAppClient()).thenReturn(false);

        sharedSubscriptionCache.put(CLIENT_ID_1, List.of(
                new ClientTopicSubscription("/test/topic/1", 2, "g1"),
                new ClientTopicSubscription("#", 0, "g2"),
                new ClientTopicSubscription("/test/topic/+", 1, "g3")
        ));
        sharedSubscriptionCache.put(CLIENT_ID_2, List.of(
                new ClientTopicSubscription("/test/topic/1", 1, "g1"),
                new ClientTopicSubscription("/test/topic/+", 2, "g3")
        ));

        assertEquals(3, sharedSubscriptionCache.getSharedSubscriptionsMap().size());
        SharedSubscriptions sharedSubscriptions = sharedSubscriptionCache.getSharedSubscriptionsMap()
                .get(new TopicSharedSubscription("/test/topic/1", "g1"));
        assertEquals(1, sharedSubscriptions.getApplicationSubscriptions().size());
        assertEquals(1, sharedSubscriptions.getDeviceSubscriptions().size());
        sharedSubscriptions = sharedSubscriptionCache.getSharedSubscriptionsMap()
                .get(new TopicSharedSubscription("/test/topic/+", "g2"));
        assertNull(sharedSubscriptions);

        sharedSubscriptionCache.remove(CLIENT_ID_1, new ClientTopicSubscription("#", 0, "g2"));
        assertEquals(2, sharedSubscriptionCache.getSharedSubscriptionsMap().size());

        when(clientSessionCache.getClientSessionInfo(CLIENT_ID_2)).thenReturn(null);
        sharedSubscriptionCache.remove(CLIENT_ID_2, new ClientTopicSubscription("/test/topic/1", 0, "g1"));

        assertEquals(2, sharedSubscriptionCache.getSharedSubscriptionsMap().size());
        sharedSubscriptions = sharedSubscriptionCache.getSharedSubscriptionsMap()
                .get(new TopicSharedSubscription("/test/topic/1", "g1"));
        assertEquals(1, sharedSubscriptions.getDeviceSubscriptions().size());
        sharedSubscriptions = sharedSubscriptionCache.getSharedSubscriptionsMap()
                .get(new TopicSharedSubscription("/test/topic/+", "g3"));
        assertEquals(1, sharedSubscriptions.getDeviceSubscriptions().size());
    }

    @Test
    public void givenNullOrEmpty_whenGetSubscriptions_thenNothingReturned() {
        assertNull(sharedSubscriptionCache.get(null));
        assertNull(sharedSubscriptionCache.get(Set.of()));
    }

    @Test
    public void givenSubscriptions_whenGetSubscriptions_thenReturnedCorrectResult() {
        when(clientSessionInfo1.isAppClient()).thenReturn(false);
        when(clientSessionInfo2.isAppClient()).thenReturn(false);

        sharedSubscriptionCache.put(CLIENT_ID_1, List.of(
                new ClientTopicSubscription("/test/topic/1", 2, "g1"),
                new ClientTopicSubscription("#", 0, "g2"),
                new ClientTopicSubscription("/test/topic/+", 1, "g3")
        ));

        sharedSubscriptionCache.put(CLIENT_ID_2, List.of(
                new ClientTopicSubscription("/test/topic/1", 1, "g1")
        ));

        SharedSubscriptions sharedSubscriptions = sharedSubscriptionCache.get(Set.of(
                new TopicSharedSubscription("/test/topic/1", "g1"),
                new TopicSharedSubscription("#", "g2"),
                new TopicSharedSubscription("/test/topic/+", "g3")
        ));

        assertEquals(2, sharedSubscriptions.getDeviceSubscriptions().size());
        for (Subscription subscription : sharedSubscriptions.getDeviceSubscriptions()) {
            assertEquals("/test/topic/1", subscription.getTopicFilter());
            if (subscription.getClientSessionInfo().getClientId().equals(CLIENT_ID_1)) {
                assertEquals(2, subscription.getQos());
            } else {
                assertEquals(1, subscription.getQos());
            }
        }
    }

    @Test(expected = RuntimeException.class)
    public void givenWrongSubscription_whenCheckIsAnyOtherDeviceClientConnected_thenThrowsException() {
        TopicSharedSubscription topicSharedSubscription = new TopicSharedSubscription("something", "g1");
        sharedSubscriptionCache.isAnyOtherDeviceClientConnected(CLIENT_ID_3, topicSharedSubscription);
    }

    @Test
    public void givenSameClient_whenCheckIsAnyOtherDeviceClientConnected_thenFalse() {
        sharedSubscriptionCache.put(CLIENT_ID_1, List.of(
                new ClientTopicSubscription("something", 2, "g1"),
                new ClientTopicSubscription("#", 0, "g2")
        ));

        TopicSharedSubscription topicSharedSubscription = new TopicSharedSubscription("something", "g1");
        boolean anyDeviceClientConnected = sharedSubscriptionCache.isAnyOtherDeviceClientConnected(CLIENT_ID_1, topicSharedSubscription);
        assertFalse(anyDeviceClientConnected);
    }

    @Test
    public void givenDisconnectedClients_whenCheckIsAnyOtherDeviceClientConnected_thenFalse() {
        when(clientSessionInfo1.isAppClient()).thenReturn(false);
        when(clientSessionInfo2.isAppClient()).thenReturn(false);
        when(clientSessionInfo1.isConnected()).thenReturn(false);

        sharedSubscriptionCache.put(CLIENT_ID_1, List.of(
                new ClientTopicSubscription("something", 2, "g1"),
                new ClientTopicSubscription("#", 0, "g2")
        ));
        sharedSubscriptionCache.put(CLIENT_ID_2, List.of(
                new ClientTopicSubscription("something", 2, "g1")
        ));

        TopicSharedSubscription topicSharedSubscription = new TopicSharedSubscription("something", "g1");
        boolean anyDeviceClientConnected = sharedSubscriptionCache.isAnyOtherDeviceClientConnected(CLIENT_ID_3, topicSharedSubscription);
        assertFalse(anyDeviceClientConnected);
    }

    @Test
    public void givenConnectedClient_whenCheckIsAnyOtherDeviceClientConnected_thenTrue() {
        when(clientSessionInfo1.isAppClient()).thenReturn(false);
        when(clientSessionInfo2.isAppClient()).thenReturn(false);

        sharedSubscriptionCache.put(CLIENT_ID_1, List.of(
                new ClientTopicSubscription("something", 2, "g1"),
                new ClientTopicSubscription("#", 0, "g2")
        ));
        sharedSubscriptionCache.put(CLIENT_ID_2, List.of(
                new ClientTopicSubscription("something", 2, "g1")
        ));

        TopicSharedSubscription topicSharedSubscription = new TopicSharedSubscription("something", "g1");
        boolean anyDeviceClientConnected = sharedSubscriptionCache.isAnyOtherDeviceClientConnected(CLIENT_ID_3, topicSharedSubscription);
        assertTrue(anyDeviceClientConnected);
    }

    @Test
    public void givenEmptySubscriptionsSet_whenFilterSubscriptions_thenReturnNothing() {
        Collection<Subscription> subscriptions = sharedSubscriptionCache.filterSubscriptions(Set.of());
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void givenSubscriptionsSetAndMissingClientSession_whenFilterSubscriptions_thenReturnNothing() {
        when(clientSessionCache.getClientSessionInfo(CLIENT_ID_1)).thenReturn(null);

        Collection<Subscription> subscriptions = sharedSubscriptionCache.filterSubscriptions(
                Set.of(getSubscription("#", 1, clientSessionInfo1, List.of(1, 2, 3)))
        );
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void givenSubscriptionsSet_whenFilterSubscriptions_thenReturnCorrectSubscriptions() {
        when(clientSessionCache.getClientSessionInfo(anyString())).thenReturn(clientSessionInfo1);

        Collection<Subscription> subscriptions = sharedSubscriptionCache.filterSubscriptions(
                Set.of(
                        getSubscription("#", 1, clientSessionInfo1, Lists.newArrayList(1, 2, 3)),
                        getSubscription("+", 2, clientSessionInfo1, Lists.newArrayList(3, 4, 5))
                )
        );
        assertEquals(1, subscriptions.size());
        Subscription subscription = subscriptions.stream().toList().get(0);
        assertEquals("+", subscription.getTopicFilter());
        assertEquals(2, subscription.getQos());
        assertEquals(6, subscription.getSubscriptionIds().size());
        assertEquals(List.of(3, 4, 5, 1, 2, 3), subscription.getSubscriptionIds());
    }

    private Subscription getSubscription(String tf, int qos, ClientSessionInfo clientSessionInfo,
                                         List<Integer> subscriptionIds) {
        return new Subscription(tf, qos, clientSessionInfo, null, SubscriptionOptions.newInstance(), subscriptionIds);
    }

}
