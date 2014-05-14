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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.as.webservices.logging.WSLogger;

/**
 * @author <a href="ropalka@redhat.com">Richard Opalka</a>
 */
abstract class AbstractDeployment {

    private final List<EJBEndpoint> ejbEndpoints = new LinkedList<EJBEndpoint>();
    private final List<EJBEndpoint> unmodifiableEjbEndpoints = Collections.unmodifiableList(ejbEndpoints);
    private final List<POJOEndpoint> pojoEndpoints = new LinkedList<POJOEndpoint>();
    private final List<POJOEndpoint> unmodifiablePojoEndpoints = Collections.unmodifiableList(pojoEndpoints);
    private final Map<String, String> urlPatternToClassMapping = new HashMap<String, String>();

    public List<EJBEndpoint> getEjbEndpoints() {
        return unmodifiableEjbEndpoints;
    }

    public List<POJOEndpoint> getPojoEndpoints() {
        return unmodifiablePojoEndpoints;
    }

    public void addEndpoint(final EJBEndpoint ep) {
        ejbEndpoints.add(ep);
    }

    public void addEndpoint(final POJOEndpoint ep) {
        final String urlPattern = ep.getUrlPattern();
        final String className = ep.getClassName();
        if (urlPatternToClassMapping.keySet().contains((urlPattern))) {
            final String clazz = urlPatternToClassMapping.get(urlPattern);
            throw WSLogger.ROOT_LOGGER.sameUrlPatternRequested(clazz, urlPattern, ep.getClassName());
        } else {
            urlPatternToClassMapping.put(urlPattern, className);
            pojoEndpoints.add(ep);
        }
    }

    public boolean contains(String urlPattern) {
        return urlPatternToClassMapping.keySet().contains((urlPattern));
    }
}
