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

import io.vertx.core.Vertx;

import java.util.Collections;
import java.util.List;

/**
 * This is a resource used to manage the Vertx instances in the subsystem.
 *
 * Each {@link VertxProxy} instance will be registered to {@link VertxRegistry} on creation and be removed on stop.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxProxy {

    /** The name of the Vertx instance, it is the last element value of the PathAddress **/
    private final String name;

    /** The JNDI name of the Vertx instance so that it can be retrieved using naming subsystem **/
    private final String jndiName;

    /** The Alias of the Vertx instance, which will be used in injection point via the member name **/
    private List<String> aliases;

    /** Flag that indicates if it is a clustered Vertx instance, it will be used to determine how Vertx instance is constructed. **/
    private final boolean clustered;

    /** The channel name in jgroups subsystem configuration, this is used when creating a clustered Vertx instance **/
    private final String jgroupChannelName;

    /** Flag that if the forked channel should be used when creating a clustered Vertx instance **/
    private final boolean forkedChannel;

    /** The Vertx reference, this will be set to null when VertxProxyService is stopped. **/
    private Vertx vertx;

    public VertxProxy(String name, String jndiName, boolean clustered, String jgroupChannelName, boolean forkedChannel) {
        this.name = name;
        this.jndiName = jndiName;
        this.clustered = clustered;
        this.jgroupChannelName = jgroupChannelName;
        this.forkedChannel = forkedChannel;
    }

    public String getName() {
        return name;
    }

    public String getJndiName() {
        return jndiName;
    }

    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    public Vertx getVertx() {
        return this.vertx;
    }

    /**
     * Returns aliases of this VertxProxy
     *
     * @return the aliases of this VertxProxy, not null.
     */
    public List<String> getAliases() {
        return aliases == null ? Collections.emptyList() : aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public boolean isClustered() {
        return clustered;
    }

    public String getJgroupChannelName() {
        return jgroupChannelName;
    }

    public boolean isForkedChannel() {
        return forkedChannel;
    }

}
