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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.vertx.core.Vertx;

/**
 * This is a resource used to manage the Vertx instances in the subsystem.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxProxy {

    private static VertxProxy INSTANCE;

    /** Flag that indicates if it is a clustered Vertx instance, it will be used to determine how Vertx instance is constructed. **/
    private final boolean clustered;

    /** The channel name in jgroups subsystem configuration, this is used when creating a clustered Vertx instance **/
    private final String jgroupChannelName;

    /** The alternative jgroups stack file for the jgroups transport **/
    private final String jgroupsStackFile;

    /** Flag that if the forked channel should be used when creating a clustered Vertx instance **/
    private final boolean forkedChannel;

    /** The option name from which the Vert.x instance will be created upon **/
    private final String optionName;

    /** The Vertx reference, this will be set to null when VertxProxyService is stopped. **/
    private final AtomicReference<Vertx> vertx = new AtomicReference<>();

    public VertxProxy(boolean clustered, String jgroupChannelName, boolean forkedChannel, String jgroupsStackFile, String optionName) {
        this.clustered = clustered;
        this.jgroupChannelName = jgroupChannelName;
        this.forkedChannel = forkedChannel;
        this.jgroupsStackFile = jgroupsStackFile;
        this.optionName = optionName;
    }

    void instrument(Vertx vertx) {
        this.vertx.set(Objects.requireNonNull(vertx));
        VertxProxyHolder.instance().instrument(this);
    }

    void release() {
        this.vertx.set(null);
        VertxProxyHolder.instance().release();
    }

    public Vertx getVertx() {
        return this.vertx.get();
    }

    public String getJgroupsStackFile() {
        return jgroupsStackFile;
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

    public String getOptionName() {
        return optionName;
    }
}
