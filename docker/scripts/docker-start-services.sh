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

set -e

source scripts/compose-utils.sh

COMPOSE_VERSION=$(composeVersion) || exit $?

ADDITIONAL_CACHE_ARGS=$(additionalComposeCacheArgs) || exit $?

checkVolumes

COMPOSE_ARGS="\
      --env-file ./.env \
      -f docker-compose.yml ${ADDITIONAL_CACHE_ARGS} \
      up -d"

case $COMPOSE_VERSION in
    V2)
        docker compose $COMPOSE_ARGS
    ;;
    V1)
        docker-compose $COMPOSE_ARGS
    ;;
    *)
        # unknown option
    ;;
esac
