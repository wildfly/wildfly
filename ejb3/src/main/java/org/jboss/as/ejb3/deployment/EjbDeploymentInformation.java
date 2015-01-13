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

package org.jboss.as.ejb3.deployment;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.iiop.EjbIIOPService;
import org.jboss.msc.value.InjectedValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Runtime information about an EJB in a module
 *
 * @author Stuart Douglas
 */
public class EjbDeploymentInformation {

    private final String ejbName;

    private final ClassLoader deploymentClassLoader;

    private final InjectedValue<EJBComponent> ejbComponent;

    private final Map<String, InjectedValue<ComponentView>> componentViews;

    private final InjectedValue<EjbIIOPService> iorFactory;
    private final Set<String> remoteViewClassNames = new HashSet<String>();

    /**
     * @param ejbName               The EJB name
     * @param ejbComponent          The EJB component
     * @param componentViews        The views exposed by the EJB component
     * @param deploymentClassLoader The deployment classloader of the EJB component
     * @param iorFactory            The {@link EjbIIOPService}
     * @deprecated since 7.1.1.Final - Use {@link #EjbDeploymentInformation(String, org.jboss.msc.value.InjectedValue, java.util.Map, java.util.Map, ClassLoader, org.jboss.msc.value.InjectedValue)} instead
     */
    @Deprecated
    public EjbDeploymentInformation(final String ejbName, final InjectedValue<EJBComponent> ejbComponent, final Map<String, InjectedValue<ComponentView>> componentViews, final ClassLoader deploymentClassLoader, final InjectedValue<EjbIIOPService> iorFactory) {
        this.ejbName = ejbName;
        this.ejbComponent = ejbComponent;
        this.componentViews = componentViews;
        this.deploymentClassLoader = deploymentClassLoader;
        this.iorFactory = iorFactory;
    }

    /**
     * @param ejbName               Name of the EJB
     * @param ejbComponent          The EJB component
     * @param remoteViews           The component views, which are exposed remotely, by the EJB. Can be null.
     * @param localViews            The component views which are exposed locally by the EJB. Can be null.
     * @param deploymentClassLoader The deployment classloader of the EJB component
     * @param iorFactory            The {@link EjbIIOPService}
     */
    public EjbDeploymentInformation(final String ejbName, final InjectedValue<EJBComponent> ejbComponent,
                                    final Map<String, InjectedValue<ComponentView>> remoteViews, final Map<String, InjectedValue<ComponentView>> localViews,
                                    final ClassLoader deploymentClassLoader, final InjectedValue<EjbIIOPService> iorFactory) {
        this.ejbName = ejbName;
        this.ejbComponent = ejbComponent;
        this.componentViews = new HashMap<String, InjectedValue<ComponentView>>();
        if (remoteViews != null) {
            this.componentViews.putAll(remoteViews);
            this.remoteViewClassNames.addAll(remoteViews.keySet());
        }
        if (localViews != null) {
            this.componentViews.putAll(localViews);
        }
        this.deploymentClassLoader = deploymentClassLoader;
        this.iorFactory = iorFactory;
    }

    public String getEjbName() {
        return ejbName;
    }

    public EJBComponent getEjbComponent() {
        return ejbComponent.getValue();
    }

    public Collection<String> getViewNames() {
        return componentViews.keySet();
    }

    public ComponentView getView(String name) {
        final InjectedValue<ComponentView> value = componentViews.get(name);
        if (value == null) {
            throw new IllegalArgumentException("View " + name + " was not found");
        }
        return value.getValue();
    }

    public ClassLoader getDeploymentClassLoader() {
        return deploymentClassLoader;
    }

    public EjbIIOPService getIorFactory() {
        return iorFactory.getOptionalValue();
    }

    /**
     * Returns true if the passed <code>viewClassName</code> represents a remote view of the EJB component.
     * Else returns false.
     *
     * @param viewClassName The fully qualified classname of the view
     * @return
     */
    public boolean isRemoteView(final String viewClassName) {
        return this.remoteViewClassNames.contains(viewClassName);
    }
}
