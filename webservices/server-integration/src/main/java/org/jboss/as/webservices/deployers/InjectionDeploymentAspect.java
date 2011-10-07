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

package org.jboss.as.webservices.deployers;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.ws.common.integration.AbstractDeploymentAspect;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.InstanceProvider;

/**
 * TODO: move to injection package!
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class InjectionDeploymentAspect extends AbstractDeploymentAspect {

    @Override
    public void start(final Deployment dep) {
        if (WSHelper.isJaxrpcDeployment(dep)) return;

        for (final Endpoint ep : dep.getService().getEndpoints()) {
            setInjectionAwareInstanceProvider(ep);
        }
    }

    @Override
    public void stop(final Deployment dep) {
        // TODO: deregister new InstantiationHandler
    }

    // TODO: handler undeployment - destroy instance & call @PreDestroy
    private void setInjectionAwareInstanceProvider(final Endpoint ep) {
        final InstanceProvider stackInstanceProvider = ep.getInstanceProvider();
        final DeploymentUnit unit = ep.getService().getDeployment().getAttachment(DeploymentUnit.class);
        final InstanceProvider injectionAwareInstanceProvider = new InjectionAwareInstanceProvider(stackInstanceProvider, ep, unit);
        ep.setInstanceProvider(injectionAwareInstanceProvider);
    }

    private static final class InjectionAwareInstanceProvider implements InstanceProvider {
        private final InstanceProvider delegate;
        private final String endpointName;
        private final String endpointClass;
        private final boolean isEjb3Endpoint;
        private final ServiceName componentPrefix;
        private static final String componentSuffix = "START";

        private InjectionAwareInstanceProvider(final InstanceProvider delegate, final Endpoint endpoint, final DeploymentUnit unit) {
            this.delegate = delegate;
            endpointName = endpoint.getShortName();
            endpointClass = endpoint.getTargetBeanName();
            isEjb3Endpoint = WSHelper.isEjbEndpoint(endpoint);
            componentPrefix = unit.getServiceName().append("component");
        }

        @Override
        public Object getInstance(final String className) {
            // TODO: hacks first !
            if (className.equals("org.jboss.ws.common.invocation.RecordingServerHandler")) return delegate.getInstance(className);
            if (className.equals("org.jboss.ws.extensions.security.jaxws.WSSecurityHandlerServer")) return delegate.getInstance(className);
            // TODO: implement cache to prevent multiple instantiations
            if (className.equals(endpointClass)) {
                if (isEjb3Endpoint) return delegate.getInstance(className); // TODO: ooops! Fix me
                // handle POJO endpoint instantiation
                final ServiceName endpointComponentName = getEndpointComponentServiceName();
                final BasicComponent endpointComponent = ((ServiceController<BasicComponent>)WSServices.getContainerRegistry().getRequiredService(endpointComponentName)).getValue();
                final ComponentInstance endpointComponentInstance = endpointComponent.createInstance(delegate.getInstance(className));
                return endpointComponentInstance.getInstance();
            } else {
                // handle JAXWS handler instantiation
                final ServiceName handlerComponentName = getHandlerComponentServiceName(className);
                final BasicComponent handlerComponent = ((ServiceController<BasicComponent>)WSServices.getContainerRegistry().getRequiredService(handlerComponentName)).getValue();
                final ComponentInstance handlerComponentInstance = handlerComponent.createInstance(delegate.getInstance(className));
                return handlerComponentInstance.getInstance();
            }
        }

        private ServiceName getEndpointComponentServiceName() {
            return componentPrefix.append(endpointName).append(componentSuffix);
        }

        private ServiceName getHandlerComponentServiceName(final String handlerClassName) {
            return componentPrefix.append(endpointName + "-" + handlerClassName).append(componentSuffix);
        }

        private InstanceProvider getDelegate() {
            return this.delegate;
        }

    }

}
