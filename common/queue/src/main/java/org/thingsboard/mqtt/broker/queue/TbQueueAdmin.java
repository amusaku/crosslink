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
package org.thingsboard.mqtt.broker.queue;

import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.thingsboard.mqtt.broker.common.data.BasicCallback;
import org.thingsboard.mqtt.broker.common.data.page.PageData;
import org.thingsboard.mqtt.broker.common.data.page.PageLink;
import org.thingsboard.mqtt.broker.common.data.queue.KafkaBroker;
import org.thingsboard.mqtt.broker.common.data.queue.KafkaConsumerGroup;
import org.thingsboard.mqtt.broker.common.data.queue.KafkaTopic;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TbQueueAdmin {

    void createTopicIfNotExists(String topic, Map<String, String> topicConfigs);

    void createTopic(String topic, Map<String, String> topicConfigs);

    void deleteTopic(String topic, BasicCallback callback);

    void deleteConsumerGroups(Collection<String> consumerGroups);

    void deleteConsumerGroup(String groupId);

    int getNumberOfPartitions(String topic);

    PageData<KafkaBroker> getClusterInfo();

    PageData<KafkaTopic> getTopics(PageLink pageLink);

    List<String> getBrokerServiceIds();

    PageData<KafkaConsumerGroup> getConsumerGroups(PageLink pageLink);

    void deleteOldConsumerGroups(String consumerGroupPrefix, String serviceId, long currentCgSuffix);

    ListConsumerGroupOffsetsResult listConsumerGroupOffsets(String groupId);
}
