/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.web.session.mocks;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.jboss.servlet.http.HttpEvent;

/**
 * @author Brian Stansberry
 * 
 */
public class MockValve implements Valve {
    private Request invokedRequest;
    private Response invokedResponse;
    private Valve next;

    /**
     * Create a new MockValve.
     * 
     */
    public MockValve() {
    }

    public void backgroundProcess() {
    }

    public void event(Request request, Response response, HttpEvent event) throws IOException, ServletException {
    }

    public String getInfo() {
        return "MockValve";
    }

    public Valve getNext() {
        return next;
    }

    public void invoke(Request request, Response response) throws IOException, ServletException {
        this.invokedRequest = request;
        this.invokedResponse = response;
    }

    public void setNext(Valve valve) {
        this.next = valve;
    }

    public Request getInvokedRequest() {
        return invokedRequest;
    }

    public Response getInvokedResponse() {
        return invokedResponse;
    }

    public void clear() {
        this.invokedRequest = null;
        this.invokedResponse = null;
    }

}
