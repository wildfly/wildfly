/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
 */
package org.wildfly.clustering.web.catalina.session;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Container;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

/**
 * Valve to be installed in the Host pipeline, that makes the Request available to the Manager.
 * @author Paul Ferraro
 */
public class ThreadLocalRequestValve extends ValveBase {

    private static final ThreadLocal<Request> threadRequest = new ThreadLocal<>();

    public static final Request currentRequest() {
        return threadRequest.get();
    }

    @Override
    public void setContainer(Container container) {
        if (!(container instanceof Host)) {
            throw new IllegalArgumentException(container.getClass().getName());
        }
        this.container = container;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        threadRequest.set(request);
        try {
            this.next.invoke(request, response);
        } finally {
            threadRequest.remove();
        }
    }
}
