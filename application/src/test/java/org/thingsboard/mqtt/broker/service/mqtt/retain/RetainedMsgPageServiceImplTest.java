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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.mqtt.broker.common.data.mqtt.retained.RetainedMsgQuery;
import org.thingsboard.mqtt.broker.common.data.page.PageData;
import org.thingsboard.mqtt.broker.common.data.page.PageLink;
import org.thingsboard.mqtt.broker.common.data.page.SortOrder;
import org.thingsboard.mqtt.broker.common.data.page.TimePageLink;
import org.thingsboard.mqtt.broker.dto.RetainedMsgDto;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class RetainedMsgPageServiceImplTest {

    RetainedMsgListenerService retainedMsgListenerService;
    RetainedMsgPageServiceImpl retainedMsgPageService;

    @Before
    public void setUp() {
        retainedMsgListenerService = mock(RetainedMsgListenerService.class);
        retainedMsgPageService = spy(new RetainedMsgPageServiceImpl(retainedMsgListenerService));

        List<RetainedMsg> retainedMsgs = getAllRetainedMessages();
        doReturn(retainedMsgs).when(retainedMsgListenerService).getRetainedMessages();
    }

    private List<RetainedMsg> getAllRetainedMessages() {
        return List.of(
                getRetainedMsg("topic/test1"),
                getRetainedMsg("topic/test2"),
                getRetainedMsg("topic/test3"),
                getRetainedMsg("my/topic"),
                getRetainedMsg("home/temp")
        );
    }

    @Test
    public void testGetRetainedMessagesWithPageLink() {
        PageData<RetainedMsgDto> retainedMessages = retainedMsgPageService.getRetainedMessages(
                new PageLink(3, 0));
        List<RetainedMsgDto> data = retainedMessages.getData();

        assertEquals(3, data.size());
        assertEquals(5, retainedMessages.getTotalElements());
        assertTrue(retainedMessages.hasNext());

        retainedMessages = retainedMsgPageService.getRetainedMessages(
                new PageLink(3, 1));
        data = retainedMessages.getData();

        assertEquals(2, data.size());
        assertEquals(5, retainedMessages.getTotalElements());
        assertEquals(2, retainedMessages.getTotalPages());
        assertFalse(retainedMessages.hasNext());

        retainedMessages = retainedMsgPageService.getRetainedMessages(
                new PageLink(3, 2));
        data = retainedMessages.getData();

        assertEquals(0, data.size());
        assertEquals(5, retainedMessages.getTotalElements());
        assertEquals(2, retainedMessages.getTotalPages());
        assertFalse(retainedMessages.hasNext());
    }

    @Test
    public void testGetRetainedMessagesWithPageLinkAndTextSearch() {
        PageData<RetainedMsgDto> retainedMessages = retainedMsgPageService.getRetainedMessages(
                new PageLink(10, 0, "topic"));
        List<RetainedMsgDto> data = retainedMessages.getData();

        assertEquals(4, data.size());
        assertEquals(4, retainedMessages.getTotalElements());
        assertFalse(retainedMessages.hasNext());
    }

    @Test
    public void testGetRetainedMessagesWithPageLinkAndSorting() {
        PageData<RetainedMsgDto> retainedMessages = retainedMsgPageService.getRetainedMessages(
                new PageLink(10, 0, null, new SortOrder("topic", SortOrder.Direction.ASC)));
        List<RetainedMsgDto> data = retainedMessages.getData();

        assertEquals(5, data.size());
        assertEquals(5, retainedMessages.getTotalElements());
        assertFalse(retainedMessages.hasNext());

        assertEquals("home/temp", data.get(0).getTopic());
        assertEquals("topic/test3", data.get(4).getTopic());
    }

    @Test
    public void testGetRetainedMessagesByQueryWithTopicName() {
        TimePageLink pageLink = new TimePageLink(10);
        PageData<RetainedMsgDto> retainedMessages = retainedMsgPageService.getRetainedMessages(new RetainedMsgQuery(pageLink, "topic/", Set.of(), null));

        assertEquals(3, retainedMessages.getData().size());
    }

    @Test
    public void testGetRetainedMessagesByQueryWithPayload() {
        TimePageLink pageLink = new TimePageLink(10);
        PageData<RetainedMsgDto> retainedMessages = retainedMsgPageService.getRetainedMessages(new RetainedMsgQuery(pageLink, null, Set.of(), "ylo"));

        assertEquals(5, retainedMessages.getData().size());
    }

    @Test
    public void testGetRetainedMessagesByQueryWithWrongPayload() {
        TimePageLink pageLink = new TimePageLink(10);
        PageData<RetainedMsgDto> retainedMessages = retainedMsgPageService.getRetainedMessages(new RetainedMsgQuery(pageLink, null, Set.of(), "aaaayloddddd"));

        assertEquals(0, retainedMessages.getData().size());
    }

    @Test
    public void testGetRetainedMessagesByQueryWithWrongQosSet() {
        TimePageLink pageLink = new TimePageLink(10);
        PageData<RetainedMsgDto> retainedMessages = retainedMsgPageService.getRetainedMessages(new RetainedMsgQuery(pageLink, null, Set.of(0, 2), null));

        assertEquals(0, retainedMessages.getData().size());
    }

    @Test
    public void testGetRetainedMessagesByQueryWithQosSet() {
        TimePageLink pageLink = new TimePageLink(10);
        PageData<RetainedMsgDto> retainedMessages = retainedMsgPageService.getRetainedMessages(new RetainedMsgQuery(pageLink, null, Set.of(1), null));

        assertEquals(5, retainedMessages.getData().size());
    }

    private static RetainedMsg getRetainedMsg(String topic) {
        return new RetainedMsg(topic, "payload".getBytes(StandardCharsets.UTF_8), 1);
    }
}
