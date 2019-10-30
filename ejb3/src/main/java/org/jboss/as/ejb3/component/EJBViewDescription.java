/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.remote.RemoteViewInjectionSource;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Value;

/**
 * EJB specific view description.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class EJBViewDescription extends ViewDescription {

    private final MethodIntf methodIntf;
    private final boolean hasJNDIBindings;

    /**
     * Should be set to true if this view corresponds to an EJB 2.x
     * local or remote view
     */
    private final boolean ejb2xView;

    public EJBViewDescription(final ComponentDescription componentDescription, final String viewClassName, final MethodIntf methodIntf, final boolean ejb2xView) {
        //only add the default configurator if an ejb 3.x business view
        super(componentDescription, viewClassName, !ejb2xView && methodIntf != MethodIntf.HOME && methodIntf != MethodIntf.LOCAL_HOME );
        this.methodIntf = methodIntf;
        this.ejb2xView = ejb2xView;
        hasJNDIBindings = initHasJNDIBindings(methodIntf);

        //add a configurator to attach the MethodIntf for this view
        getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.putPrivateData(MethodIntf.class, getMethodIntf());
            }
        });
        // add a view configurator for setting up application specific container interceptors for the EJB view
        getConfigurators().add(EJBContainerInterceptorsViewConfigurator.INSTANCE);
    }

    public MethodIntf getMethodIntf() {
        return methodIntf;
    }

    @Override
    public ViewConfiguration createViewConfiguration(final Class<?> viewClass, final ComponentConfiguration componentConfiguration, final ProxyFactory<?> proxyFactory) {
        return new EJBViewConfiguration(viewClass, componentConfiguration, getServiceName(), proxyFactory, getMethodIntf());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EJBViewDescription that = (EJBViewDescription) o;

        // since the views are added to the component description, that should already be equal
        if (hasJNDIBindings != that.hasJNDIBindings) return false;
        if (methodIntf != that.methodIntf) return false;
        if (!getViewClassName().equals(that.getViewClassName())) return false;
        //we compare the components based on ==
        //as you can have two components with the same name
        if (getComponentDescription() != that.getComponentDescription()) return false;

        return super.equals(o);
    }

    @Override
    protected InjectionSource createInjectionSource(final ServiceName serviceName, Value<ClassLoader> viewClassLoader, boolean appclient) {
        if(methodIntf != MethodIntf.REMOTE && methodIntf != MethodIntf.HOME) {
            return super.createInjectionSource(serviceName, viewClassLoader, appclient);
        } else {
            final EJBComponentDescription componentDescription = getComponentDescription();
            final EEModuleDescription desc = componentDescription.getModuleDescription();
            final String earApplicationName = desc.getEarApplicationName();
            return new RemoteViewInjectionSource(serviceName, earApplicationName, desc.getModuleName(), desc.getDistinctName(), componentDescription.getComponentName(), getViewClassName() , componentDescription.isStateful(),viewClassLoader, appclient);
        }
    }

    @Override
    public EJBComponentDescription getComponentDescription() {
        return (EJBComponentDescription)super.getComponentDescription();
    }

    @Override // TODO: what to do in JNDI if multiple views are available for no interface view ?
    public ServiceName getServiceName() {
        return super.getServiceName().append(methodIntf.toString());
    }

    @Override
    public int hashCode() {
        int result = methodIntf.hashCode();
        result = 31 * result + (hasJNDIBindings ? 1 : 0);
        result = 31 * result + getViewClassName().hashCode();
        result = 31 * result + getComponentDescription().getComponentName().hashCode();
        return result;
    }

    public boolean hasJNDIBindings() {
        return hasJNDIBindings;
    }

    private boolean initHasJNDIBindings(final MethodIntf methodIntf) {
        if (methodIntf == MethodIntf.MESSAGE_ENDPOINT) {
            return false;
        }
        if (methodIntf == MethodIntf.SERVICE_ENDPOINT) {
            return false;
        }
        if (methodIntf == MethodIntf.TIMER) {
            return false;
        }

        return true;
    }

    public boolean isEjb2xView() {
        return ejb2xView;
    }

}
