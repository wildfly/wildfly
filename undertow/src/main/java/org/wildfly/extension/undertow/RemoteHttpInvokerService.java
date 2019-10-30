/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import io.undertow.server.handlers.CookieImpl;
import io.undertow.server.session.SecureRandomSessionIdGenerator;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import io.undertow.server.handlers.PathHandler;

/**
 * Service that exposes the Wildfly
 *
 * @author Stuart Douglas
 */
public class RemoteHttpInvokerService implements Service<PathHandler> {

    private static final String AFFINITY_PATH = "/common/v1/affinity";

    private final PathHandler pathHandler = new PathHandler();

    @Override
    public void start(StartContext context) throws StartException {
        pathHandler.clearPaths();
        SecureRandomSessionIdGenerator generator = new SecureRandomSessionIdGenerator();

        pathHandler.addPrefixPath(AFFINITY_PATH, exchange -> {
            String resolved = exchange.getResolvedPath();
            int index = resolved.lastIndexOf(AFFINITY_PATH);
            if(index > 0) {
                resolved = resolved.substring(0, index);
            }
            exchange.getResponseCookies().put("JSESSIONID", new CookieImpl("JSESSIONID", generator.createSessionId()).setPath(resolved));
        });
    }

    @Override
    public void stop(StopContext context) {
        pathHandler.clearPaths();
    }

    @Override
    public PathHandler getValue() throws IllegalStateException, IllegalArgumentException {
        return pathHandler;
    }
}
