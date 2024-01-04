/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
