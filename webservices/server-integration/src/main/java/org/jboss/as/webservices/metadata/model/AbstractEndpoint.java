/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.metadata.model;

import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="ropalka@redhat.com">Richard Opalka</a>
 */
public abstract class AbstractEndpoint {

    public static final String COMPONENT_VIEW_NAME = AbstractEndpoint.class.getPackage().getName() + "ComponentViewName";
    public static final String WELD_DEPLOYMENT = AbstractEndpoint.class.getPackage().getName() + ".WeldDeployment";
    private final String name;
    private final String className;
    private final ServiceName viewName;

    protected AbstractEndpoint(final String name, final String className, final ServiceName viewName) {
        this.name = name;
        this.className = className;
        this.viewName = viewName;
    }

    public final String getName() {
        return name;
    }

    public final String getClassName() {
        return className;
    }

    public ServiceName getComponentViewName() {
        return viewName;
    }

}
