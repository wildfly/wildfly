/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
