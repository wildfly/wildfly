/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

/**
 * A custom {@link HttpHandler} which sets the response headers {@link #RESPONSE_HEADER_ONE_NAME} and
 * {@link #RESPONSE_HEADER_TWO_NAME} with the value {@code true} if this handler was constructed using
 * the "right" TCCL in the static and object initialization, respectively. The headers' value is set
 * to {@code false} otherwise
 *
 * @author Jaikiran Pai
 */
public class CustomHttpHandler implements HttpHandler {

    static final String RESPONSE_HEADER_ONE_NAME = "correct-tccl-during-handler-static-init";
    static final String RESPONSE_HEADER_TWO_NAME = "correct-tccl-during-handler-init";

    private final HttpHandler next;
    private static final boolean correctTcclInStaticInit;
    private final boolean correctTcclInInstanceConstruction;

    static {
        correctTcclInStaticInit = tryLoadClassInTCCL();
    }

    public CustomHttpHandler(final HttpHandler next) {
        this.next = next;
        this.correctTcclInInstanceConstruction = tryLoadClassInTCCL();
    }

    private static boolean tryLoadClassInTCCL() {
        try {
            Class.forName("org.jboss.as.test.manualmode.undertow.SomeClassInSameModuleAsCustomHttpHandler", true, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(new HttpString(RESPONSE_HEADER_ONE_NAME), String.valueOf(correctTcclInStaticInit));
        exchange.getResponseHeaders().put(new HttpString(RESPONSE_HEADER_TWO_NAME), String.valueOf(this.correctTcclInInstanceConstruction));
        next.handleRequest(exchange);
    }
}
