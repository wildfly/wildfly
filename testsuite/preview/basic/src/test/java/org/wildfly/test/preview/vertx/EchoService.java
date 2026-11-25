/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.preview.vertx;

import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Stateless
public class EchoService {

    @Any
    @Inject
    private Instance<Vertx> vertxInstance;

    private Vertx getVertx(boolean exist) {
        if (exist) {
            return vertxInstance.select(Identifier.Literal.of("vertx")).get();
        }
        return vertxInstance.select(Identifier.Literal.of("vertx-not-exist")).get();
    }

    @Asynchronous
    public Future<String> echo(String message) {
        Vertx vertx = getVertx(true);
        vertx.eventBus()
                .<String>localConsumer("echo")
                .handler(msg -> msg.reply(msg.body()));
        return (CompletableFuture<String>)vertx.eventBus().request("echo", message).map(msg -> msg.body().toString()).toCompletionStage();
    }

    @Asynchronous
    public Future<String> echoFail(String message) throws Exception {
        getVertx(false);
        return CompletableFuture.completedFuture("wrong");
    }
}
