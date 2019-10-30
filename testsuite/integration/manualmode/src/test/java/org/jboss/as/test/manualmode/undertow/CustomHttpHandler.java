/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
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
