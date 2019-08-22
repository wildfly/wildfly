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

package org.jboss.as.ee.component;

import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.naming.InjectedEENamespaceContextSelector;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DelegatingSupplier;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * A configurator which adds interceptors to the component which establish the naming context.  The interceptor is
 * added to the beginning of each chain.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class NamespaceConfigurator implements ComponentConfigurator {

    /** {@inheritDoc} */
    public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
        final ComponentNamingMode namingMode = description.getNamingMode();
        final String applicationName = configuration.getApplicationName();
        final String moduleName = configuration.getModuleName();
        final String compName = configuration.getComponentName();
        final ServiceName appContextServiceName = ContextNames.contextServiceNameOfApplication(applicationName);
        final ServiceName moduleContextServiceName = ContextNames.contextServiceNameOfModule(applicationName, moduleName);
        final ServiceName compContextServiceName = ContextNames.contextServiceNameOfComponent(applicationName, moduleName, compName);
        final InjectedEENamespaceContextSelector selector = new InjectedEENamespaceContextSelector();
        final DelegatingSupplier<NamingStore> appSupplier = selector.getAppContextSupplier();
        final DelegatingSupplier<NamingStore> moduleSupplier = selector.getModuleContextSupplier();
        final DelegatingSupplier<NamingStore> compSupplier = selector.getCompContextSupplier();
        final DelegatingSupplier<NamingStore> jbossSupplier = selector.getJbossContextSupplier();
        final DelegatingSupplier<NamingStore> globalSupplier = selector.getGlobalContextSupplier();
        final DelegatingSupplier<NamingStore> exportedSupplier = selector.getExportedContextSupplier();

        configuration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
            public void configureDependency(final ServiceBuilder<?> serviceBuilder, ComponentStartService service) {
                appSupplier.set(serviceBuilder.requires(appContextServiceName));
                moduleSupplier.set(serviceBuilder.requires(moduleContextServiceName));
                if (namingMode == ComponentNamingMode.CREATE) {
                    compSupplier.set(serviceBuilder.requires(compContextServiceName));
                } else if(namingMode == ComponentNamingMode.USE_MODULE) {
                    compSupplier.set(serviceBuilder.requires(moduleContextServiceName));
                }
                globalSupplier.set(serviceBuilder.requires(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME));
                jbossSupplier.set(serviceBuilder.requires(ContextNames.JBOSS_CONTEXT_SERVICE_NAME));
                exportedSupplier.set(serviceBuilder.requires(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME));
            }
        });
        final InterceptorFactory interceptorFactory = new ImmediateInterceptorFactory(new NamespaceContextInterceptor(selector, context.getDeploymentUnit().getServiceName()));
        configuration.addPostConstructInterceptor(interceptorFactory, InterceptorOrder.ComponentPostConstruct.JNDI_NAMESPACE_INTERCEPTOR);
        configuration.addPreDestroyInterceptor(interceptorFactory, InterceptorOrder.ComponentPreDestroy.JNDI_NAMESPACE_INTERCEPTOR);
        if(description.isPassivationApplicable()) {
            configuration.addPrePassivateInterceptor(interceptorFactory, InterceptorOrder.ComponentPassivation.JNDI_NAMESPACE_INTERCEPTOR);
            configuration.addPostActivateInterceptor(interceptorFactory, InterceptorOrder.ComponentPassivation.JNDI_NAMESPACE_INTERCEPTOR);
        }

        configuration.setNamespaceContextInterceptorFactory(interceptorFactory);
        configuration.setNamespaceContextSelector(selector);
    }
}
