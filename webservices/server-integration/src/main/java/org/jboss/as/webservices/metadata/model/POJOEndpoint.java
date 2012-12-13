/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.metadata.model;

import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class POJOEndpoint extends AbstractEndpoint {

    private final String urlPattern;
    private final boolean isDeclared;

    public POJOEndpoint(final String name, final String className, final ServiceName viewName, final String urlPattern) {
        this(name, className, viewName, urlPattern, true);
    }

    public POJOEndpoint(final String className, final ServiceName viewName, final String urlPattern) {
        this(className, className, viewName, urlPattern, false);
    }

    public POJOEndpoint(final String name, final String className, final ServiceName viewName, final String urlPattern, boolean isDeclared) {
        super(name, className, viewName);
        this.urlPattern = urlPattern;
        this.isDeclared = isDeclared;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public boolean isDeclared() {
        return isDeclared;
    }

}
