/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.logging.perdeploy;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.logging.LoggingExtension;
import org.jboss.logmanager.ClassLoaderLogContextSelector;
import org.jboss.logmanager.ThreadLocalLogContextSelector;

/**
 * <strong>NOTE:</strong> This uses a reflection hack to determine the size of the {@link
 * org.jboss.logmanager.LogContext log context} map from a {@link org.jboss.as.logging.logmanager.WildFlyLogContextSelector
 * log context selector}. This is rather fragile with a change to the {@link org.jboss.as.logging.LoggingExtension}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@WebServlet(LogContextHackServlet.SERVLET_URL)
public class LogContextHackServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public static final String SERVLET_URL = "/logContext";

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    private void processRequest(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        try {
            final ClassLoaderLogContextSelector selector = getFieldValue(LoggingExtension.class, "CONTEXT_SELECTOR", ClassLoaderLogContextSelector.class);
            final ConcurrentMap<?, ?> map = getFieldValue(selector, ConcurrentMap.class, "contextMap");
            response.getWriter().print(map.size());
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private static <T> T getFieldValue(final Class<?> staticType, final String fieldName, final Class<T> type) throws NoSuchFieldException, IllegalAccessException {
        final Field field = staticType.getDeclaredField(fieldName);
        if (Modifier.isStatic(field.getModifiers()) && type.isAssignableFrom(field.getType())) {
            field.setAccessible(true);
            return type.cast(field.get(null));
        }
        return null;
    }

    private static <T> T getFieldValue( final Object ref, final Class<T> type, final String fieldName) throws NoSuchFieldException, IllegalAccessException {
        final Field field = ref.getClass().getDeclaredField(fieldName);
        if (type.isAssignableFrom(field.getType())) {
            field.setAccessible(true);
            return type.cast(field.get(ref));
        }
        return null;
    }
}
