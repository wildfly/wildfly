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
 */

package org.jboss.as.capedwarf.services;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Execute servlet from an async invocation.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ServletExecutor {

    private static Map<String, ServletContext> contexts = new ConcurrentHashMap<String, ServletContext>();
    private static ServletResponse NOOP = new NoopServletResponse();

    public static ServletContext registerContext(final String appId, final ServletContext context) {
        return contexts.put(appId, context);
    }

    public static ServletContext unregisterContext(final String appId) {
        return contexts.remove(appId);
    }

    /**
     * Dispatch custom request.
     *
     * @param appId the appId
     * @param path the dispatcher path
     * @param request the custom request
     * @throws IOException for any I/O exception
     * @throws ServletException for any servlet exception
     */
    static void dispatch(final String appId, final String path, final ServletRequest request) throws IOException, ServletException {
        if (appId == null)
            throw new IllegalArgumentException("Null appId");
        if (path == null)
            throw new IllegalArgumentException("Null path");
        if (request == null)
            throw new IllegalArgumentException("Null request");

        final ServletContext context = contexts.get(appId);
        if (context == null)
            throw new IllegalArgumentException("No context registered for appId: " + appId);

        final RequestDispatcher dispatcher = context.getRequestDispatcher(path);
        if (dispatcher == null)
            throw new IllegalArgumentException("No dispatcher for path: " + path);

        dispatcher.forward(request, NOOP);
    }
}
