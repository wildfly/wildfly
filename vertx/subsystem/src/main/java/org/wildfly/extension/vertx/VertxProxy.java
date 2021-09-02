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

import io.vertx.core.VertxOptions;

public class VertxProxy {
    private String name;
    private String jndiName;
    private VertxOptions vertxOptions;
    private boolean clustered;
    private String jgroupChannelName;

    public VertxOptions getVertxOptions() {
        return vertxOptions;
    }

    public void setVertxOptions(VertxOptions vertxOptions) {
        this.vertxOptions = vertxOptions;
    }

    public boolean isClustered() {
        return clustered;
    }

    public void setClustered(boolean clustered) {
        this.clustered = clustered;
    }

    public String getJgroupChannelName() {
        return jgroupChannelName;
    }

    public void setJgroupChannelName(String jgroupChannelName) {
        this.jgroupChannelName = jgroupChannelName;
    }

    public String getName() {
        return name;
    }

    public VertxProxy setName(String name) {
        this.name = name;
        return this;
    }

    public String getJndiName() {
        return jndiName;
    }

    public VertxProxy setJndiName(String jndiName) {
        this.jndiName = jndiName;
        return this;
    }


}
