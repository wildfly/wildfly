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
    String ELEMENT_VERTX_OPTIONS_FILE = "vertx-options-file";

    String ATTR_DEFAULT_OPTION_NAME = "default-option-name";
    String ATTR_NAME = "name";
    String ATTR_JNDI_NAME = "jndi-name";
    String ATTR_OPTION_NAME = "option-name";
    String ATTR_CLUSTERED = "clustered";
    String ATTR_JGROUPS_CHANNEL = "jgroups-channel";
    String ATTR_FORKED_CHANNEL = "forked-channel";
    String ATTR_ALIAS = "alias";
    String ATTR_PATH = "path";


    // Followings are cluster manager related settings
    String DEFAULT_CACHE_NAME = "distributed-cache";
    String SUBS_CACHE_NAME = "__vertx.subs";
    String HA_INFO_CACHE_NAME = "__vertx.haInfo";
    String NODE_INFO_CACHE_NAME = "__vertx.nodeInfo";
    String CACHE_CONFIGURATION = "__vertx.distributed.cache.configuration";

}
