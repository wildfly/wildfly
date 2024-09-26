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

import org.jboss.as.version.Stability;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public interface VertxConstants {

    Stability EXTENSION_STABILITY = Stability.PREVIEW;

    String[] TIME_UNITS = Arrays.stream(TimeUnit.values()).map(Enum::toString).collect(Collectors.toList()).toArray(new String[0]);

    String CDI_NAMED_QUALIFIER = "vertx";
    String VERTX_SERVICE = "vertx";
    String ELEMENT_VERTX = "vertx";

    String ELEMENT_VERTX_OPTIONS = "vertx-options";
    String ELEMENT_VERTX_OPTIONS_FILE = "vertx-option-file";
    String ELEMENT_VERTX_OPTION = "vertx-option";
    String ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER = "address-resolver-option";
    String ATTR_OPTION_NAME = "option-name";
    String ATTR_PATH = "path";

    // basic vertx-option attributes
    String ATTR_EVENTLOOP_POOL_SIZE = "event-loop-pool-size";
    String ATTR_WORKER_POOL_SIZE = "worker-pool-size";
    String ATTR_INTERNAL_BLOCKING_POOL_SIZE = "internal-blocking-pool-size";
    String ATTR_PREFER_NATIVE_TRANSPORT = "prefer-native-transport";
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

    // address resolver options
    String ATTR_HOSTS_PATH = "hosts-path";
    String ATTR_HOSTS_VALUE = "hosts-value";
    String ATTR_SERVERS = "servers";
    String ATTR_OPT_RES_ENABLED = "opt-resource-enabled";
    String ATTR_CACHE_MIN_TTL = "cache-min-time-to-live";
    String ATTR_MAX_TTL = "cache-max-time-to-live";
    String ATTR_NEGATIVE_TTL = "cache-negative-time-to-live";
    String ATTR_QUERY_TIMEOUT = "query-time-out";
    String ATTR_MAX_QUERIES = "max-queries";
    String ATTR_RD_FLAG = "rd-flag";
    String ATTR_SEARCH_DOMAIN = "search-domains";
    String ATTR_N_DOTS = "n-dots";
    String ATTR_ROTATE_SERVERS = "rotate-servers";
    String ATTR_ROUND_ROBIN_INET_ADDRESS = "round-robin-inet-address";

}
