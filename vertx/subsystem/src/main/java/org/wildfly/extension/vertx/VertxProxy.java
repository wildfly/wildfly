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

/**
 * This is a resource used to manage the Vertx instances in the subsystem.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxProxy {

    /** The option name from which the Vert.x instance will be created upon **/
    private final String optionName;
    private final Vertx vertx;

    public VertxProxy(String optionName, Vertx vertx) {
        this.optionName = optionName;
        this.vertx = vertx;
    }

    public Vertx getVertx() {
        return this.vertx;
    }

    public String getOptionName() {
        return optionName;
    }
}