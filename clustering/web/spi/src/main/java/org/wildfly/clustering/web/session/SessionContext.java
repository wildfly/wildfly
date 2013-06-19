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
package org.wildfly.clustering.web.session;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

/**
 * Context exposed to the session.
 * @author Paul Ferraro
 */
public interface SessionContext {

    /**
     * Returns the registered session listeners.
     * @return a non-null collection of session listeners.
     */
    Iterable<HttpSessionListener> getSessionListeners();

    /**
     * Returns the registered session attribute listeners.
     * @return a non-null collection of session listeners.
     */
    Iterable<HttpSessionAttributeListener> getSessionAttributeListeners();

    /**
     * Returns the servlet context of this application.
     * @return the non-null servlet context
     */
    ServletContext getServletContext();
}
