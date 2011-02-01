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

package org.jboss.as.ee.component.processor;

import javax.naming.Context;
import javax.naming.LinkRef;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.injection.ResourceInjection;
import org.jboss.as.ee.component.injection.ResourceInjectionConfiguration;
import org.jboss.as.ee.component.injection.ResourceInjectionDependency;
import org.jboss.as.naming.deployment.NamingLookupValue;
import org.jboss.as.naming.deployment.ResourceBinder;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Values;

/**
 * @author John Bailey
 */
public class ResourceInjectionInstallProcessor extends AbstractComponentConfigProcessor {

    protected void processComponentConfig(DeploymentUnit deploymentUnit, DeploymentPhaseContext phaseContext, ComponentConfiguration componentConfiguration) throws DeploymentUnitProcessingException {
        final Class<?> componentClass = componentConfiguration.getComponentClass();

        final ServiceName envContextServiceName = componentConfiguration.getEnvContextServiceName();

        // Process the component's injections
        for (ResourceInjectionConfiguration resourceConfiguration : componentConfiguration.getResourceInjectionConfigs()) {
            final NamingLookupValue<Object> lookupValue = new NamingLookupValue<Object>(resourceConfiguration.getLocalContextName());
            final ResourceInjection injection = ResourceInjection.Factory.create(resourceConfiguration, componentClass, lookupValue);
            if (injection != null) {
                componentConfiguration.addResourceInjection(injection);
                componentConfiguration.addDependency(bindResource(phaseContext.getServiceTarget(), componentConfiguration, resourceConfiguration));
            }
            componentConfiguration.addDependency(new ResourceInjectionDependency<Context>(envContextServiceName, Context.class, lookupValue.getContextInjector()));
        }
    }

    private ResourceInjectionDependency<?> bindResource(final ServiceTarget serviceTarget, final ComponentConfiguration componentConfiguration, final ResourceInjectionConfiguration resourceConfiguration) throws DeploymentUnitProcessingException {
        final ServiceName binderName = componentConfiguration.getEnvContextServiceName().append(resourceConfiguration.getBindName());

        final LinkRef linkRef = new LinkRef(resourceConfiguration.getBindTargetName());
        final ResourceBinder<LinkRef> resourceBinder = new ResourceBinder<LinkRef>(resourceConfiguration.getBindName(), Values.immediateValue(linkRef));

        serviceTarget.addService(binderName, resourceBinder)
                .addDependency(componentConfiguration.getEnvContextServiceName(), Context.class, resourceBinder.getContextInjector())
                .install();

        return new ResourceInjectionDependency<Void>(binderName);
    }
}
