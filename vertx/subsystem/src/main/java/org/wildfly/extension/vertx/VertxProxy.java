/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.vertx;

/**
 * This is a resource used to manage the Vertx instances in the subsystem.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxProxy {

    /** The option name from which the Vert.x instance will be created upon **/
    private final String optionName;
    private final io.vertx.core.Vertx vertx;
    private final io.vertx.mutiny.core.Vertx mutiyVertx;

    public VertxProxy(String optionName, io.vertx.core.Vertx vertx) {
        this.optionName = optionName;
        this.vertx = vertx;
        this.mutiyVertx = new io.vertx.mutiny.core.Vertx(vertx);
    }

    public io.vertx.core.Vertx getVertx() {
        return this.vertx;
    }

    public io.vertx.mutiny.core.Vertx getMutiyVertx() {
        return mutiyVertx;
    }

    public String getOptionName() {
        return optionName;
    }
}