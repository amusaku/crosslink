#!/bin/bash
#
# Copyright © 2016-2025 The Thingsboard Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

CONF_FOLDER="${pkg.installFolder}/conf"
jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
configfile=${pkg.name}.conf
firstlaunch=${DATA_FOLDER}/.firstlaunch

logbackfile="/config/logback.xml"
if [ ! -f ${logbackfile} ]; then
  logbackfile=${CONF_FOLDER}/logback.xml
fi

source "${CONF_FOLDER}/${configfile}"

if [ ! -f ${firstlaunch} ]; then
    install-tbmq.sh && touch ${firstlaunch}
fi

if [ -f ${firstlaunch} ]; then
    echo "Starting TBMQ ..."

    exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.mqtt.broker.ThingsboardMqttBrokerApplication \
                        -Dlogging.config=${logbackfile} \
                        org.springframework.boot.loader.launch.PropertiesLauncher
else
    echo "ERROR: TBMQ is not installed"
fi
