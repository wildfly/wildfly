/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.managedbean.container;

import java.util.ArrayList;
import java.util.List;
import javax.naming.Context;
import org.jboss.as.ee.component.injection.ResourceInjection;
import org.jboss.as.ee.component.injection.ResourceInjectionConfiguration;
import org.jboss.as.ee.component.injection.ResourceInjectionResolver;
import org.jboss.as.ee.naming.ContextNames;
import org.jboss.as.ee.naming.NamingContextConfig;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.naming.deployment.NamingLookupValue;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceName;

/**
 * @author John Bailey
 */
public class ManagedBeanResourceInjectionResolver implements ResourceInjectionResolver {
    public static final ManagedBeanResourceInjectionResolver INSTANCE = new ManagedBeanResourceInjectionResolver();

    private ManagedBeanResourceInjectionResolver() {
    }

    public ResolverResult resolve(final DeploymentUnit deploymentUnit, final String beanName, final Class<?> beanClass, final ResourceInjectionConfiguration configuration) {
        final JndiName managedBeanContextJndiName = ContextNames.MODULE_CONTEXT_NAME.append("env").append(beanName);
        final JndiName localContextName = managedBeanContextJndiName.append(configuration.getLocalContextName());

        final NamingLookupValue<Object> lookupValue = new NamingLookupValue<Object>(localContextName);
        final ResourceInjection injection = ResourceInjection.Factory.create(configuration, beanClass, lookupValue);

        final NamingContextConfig moduleContext = deploymentUnit.getAttachment(org.jboss.as.ee.naming.Attachments.MODULE_CONTEXT_CONFIG);
        final ServiceName managedBeanContextServiceName = moduleContext.getContextServiceName().append("env").append(beanName);

        final String targetName = configuration.getTargetContextName();
        if(targetName == null) {
            throw new IllegalArgumentException("Resource configuration does not have target JNDI name");
        }

        final JndiName targetLookupName = targetName.startsWith("java") ? JndiName.of(targetName) : ContextNames.MODULE_CONTEXT_NAME.append(targetName);

        final List<ResolverDependency<?>> dependencies = new ArrayList<ResolverDependency<?>>();
        dependencies.add(new ResolverDependency<Context>() {
            public ServiceName getServiceName() {
                return managedBeanContextServiceName;
            }

            public Injector<Context> getInjector() {
                return lookupValue.getContextInjector();
            }

            public Class<Context> getInjectorType() {
                return Context.class;
            }
        });

        return new ResolverResult() {
            public ResourceInjection getInjection() {
                return injection;
            }

            public ServiceName getBindContextName() {
                return managedBeanContextServiceName;
            }

            public String getBindName() {
                return localContextName.getLocalName();
            }

            public String getBindTargetName() {
                return targetLookupName.getAbsoluteName();
            }

            public List<ResolverDependency<?>> getDependencies() {
                return dependencies;
            }

            public boolean shouldBind() {
                return injection != null;
            }
        };
    }
}
