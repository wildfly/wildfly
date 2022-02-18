/*
 * Copyright (C) 2020 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.vertx;

public interface VertxConstants {

    String DEFAULT_VERTX_OPTION_NAME = "__DEFAULT__";
    String DEFAULT_JNDI_PREFIX = "java:/vertx/";

    String ELEMENT_VERTXES = "vertxes";
    String ELEMENT_VERTX = "vertx";

    String ELEMENT_VERTX_OPTIONS = "vertx-options";
    String ELEMENT_VERTX_OPTIONS_FILE = "vertx-option-file";
    String ELEMENT_VERTX_OPTION = "vertx-option";

    String ATTR_NAME = "name";
    String ATTR_JNDI_NAME = "jndi-name";
    String ATTR_OPTION_NAME = "option-name";
    String ATTR_CLUSTERED = "clustered";
    String ATTR_JGROUPS_CHANNEL = "jgroups-channel";
    String ATTR_FORKED_CHANNEL = "forked-channel";
    String ATTR_ALIAS = "alias";
    String ATTR_PATH = "path";

    // basic vertx-option attributes
    String ATTR_EVENTLOOP_POOL_SIZE = "event-loop-pool-size";
    String ATTR_WORKER_POOL_SIZE = "worker-pool-size";
    String ATTR_INTERNAL_BLOCKING_POOL_SIZE = "internal-blocking-pool-size";
    String ATTR_HA_ENABLED = "ha-enabled";
    String ATTR_HA_GROUP = "ha-group";
    String ATTR_QUORUM_SIZE = "quorum-size";
    String ATTR_PREFER_NATIVE_TRANSPORT = "prefer-native-transport";
    String ATTR_DISABLE_TCCL = "disable-tccl";
    String ATTR_BLOCKED_THREAD_CHECK_INTERVAL = "blocked-thread-check-interval";
    String ATTR_BLOCKED_THREAD_CHECK_INTERVAL_UNIT = "blocked-thread-check-interval-unit";
    String ATTR_MAX_EVENTLOOP_EXECUTE_TIME = "max-eventloop-execute-time";
    String ATTR_MAX_EVENTLOOP_EXECUTE_TIME_UNIT = "max-eventloop-execute-time-unit";
    String ATTR_MAX_WORKER_EXECUTE_TIME = "max-worker-execute-time";
    String ATTR_MAX_WORKER_EXECUTE_TIME_UNIT = "max-worker-execute-time-unit";
    String ATTR_WARNING_EXECUTION_TIME = "warning-exception-time";
    String ATTR_WARNING_EXECUTION_TIME_UNIT = "warning-exception-time-unit";

    // file system options
    String ATTR_FS_CLASS_PATH_RESOLVING_ENABLED = "classpath-resolving-enabled";
    String ATTR_FS_FILE_CACHE_ENABLED = "file-cache-enabled";

    // Followings are cluster manager related settings
    String DEFAULT_CACHE_NAME = "distributed-cache";
    String SUBS_CACHE_NAME = "__vertx.subs";
    String HA_INFO_CACHE_NAME = "__vertx.haInfo";
    String NODE_INFO_CACHE_NAME = "__vertx.nodeInfo";
    String CACHE_CONFIGURATION = "__vertx.distributed.cache.configuration";

}
