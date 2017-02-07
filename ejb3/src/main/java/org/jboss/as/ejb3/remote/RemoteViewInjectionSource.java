/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.remote;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Value;

/**
 * Injection source for EJB remote views.
 *
 * @author Stuart Douglas
 */
public class RemoteViewInjectionSource extends InjectionSource {

    private final ServiceName serviceName;
    private final String appName;
    private final String moduleName;
    private final String distinctName;
    private final String beanName;
    private final String viewClass;
    private final boolean stateful;
    private final Value<ClassLoader> viewClassLoader;
    private final boolean appclient;

    public RemoteViewInjectionSource(final ServiceName serviceName, final String appName, final String moduleName, final String distinctName, final String beanName, final String viewClass, final boolean stateful, final Value<ClassLoader> viewClassLoader, boolean appclient) {
        this.serviceName = serviceName;
        this.appName = appName;
        this.moduleName = moduleName;
        this.distinctName = distinctName;
        this.beanName = beanName;
        this.viewClass = viewClass;
        this.stateful = stateful;
        this.viewClassLoader = viewClassLoader;
        this.appclient = appclient;
    }

    /**
     * {@inheritDoc}
     */
    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) {
        if(serviceName != null) {
            serviceBuilder.addDependency(serviceName);
        }
        final RemoteViewManagedReferenceFactory factory = new RemoteViewManagedReferenceFactory(appName, moduleName, distinctName, beanName, viewClass, stateful, viewClassLoader, appclient);
        injector.inject(factory);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final RemoteViewInjectionSource that = (RemoteViewInjectionSource) o;

        if (stateful != that.stateful) return false;
        if (appName != null ? !appName.equals(that.appName) : that.appName != null) return false;
        if (beanName != null ? !beanName.equals(that.beanName) : that.beanName != null) return false;
        if (distinctName != null ? !distinctName.equals(that.distinctName) : that.distinctName != null) return false;
        if (moduleName != null ? !moduleName.equals(that.moduleName) : that.moduleName != null) return false;
        if (viewClass != null ? !viewClass.equals(that.viewClass) : that.viewClass != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = appName != null ? appName.hashCode() : 0;
        result = 31 * result + (moduleName != null ? moduleName.hashCode() : 0);
        result = 31 * result + (distinctName != null ? distinctName.hashCode() : 0);
        result = 31 * result + (beanName != null ? beanName.hashCode() : 0);
        result = 31 * result + (viewClass != null ? viewClass.hashCode() : 0);
        result = 31 * result + (stateful ? 1 : 0);
        return result;
    }
}
