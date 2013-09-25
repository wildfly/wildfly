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

package org.wildfly.extension.undertow;

import org.jboss.as.web.host.CommonWebServer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Stuart Douglas
 */
class WebServerService implements CommonWebServer, Service<WebServerService> {


    private volatile int httpPort = -1;
    private volatile int httpsPort = -1;

    @Override
    public int getPort(final String protocol, final boolean secure) {
        // TODO: Is relying on "secure" enough of should the protocol be considered too? For example, how would this behave with AJP (or does it not matter)
        if (secure) {
            if (httpsPort == -1) {
                // throw error
                throw UndertowMessages.MESSAGES.noPortListeningForProtocol(protocol);
            }
            return httpsPort;
        }
        if (httpPort == -1) {
            // throw error
            throw UndertowMessages.MESSAGES.noPortListeningForProtocol(protocol);
        }
        return httpPort;
    }

    @Override
    public void start(final StartContext context) throws StartException {

    }

    @Override
    public void stop(final StopContext context) {
        httpPort = -1;
        httpsPort = -1;
    }

    @Override
    public WebServerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    void setHttpPort(final int port) {
        this.httpPort = port;
    }

    void setHttpsPort(final int port) {
        this.httpsPort = port;
    }

}
