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
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * A configurator which adds interceptors to the component which establish the naming context.  The interceptor is
 * added to the beginning of each chain.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class NamespaceConfigurator implements ComponentConfigurator {

    /** {@inheritDoc} */
    public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
        final ComponentNamingMode namingMode = description.getNamingMode();
        final InjectedEENamespaceContextSelector selector = new InjectedEENamespaceContextSelector();
        final String applicationName = configuration.getApplicationName();
        final String moduleName = configuration.getModuleName();
        final String compName = configuration.getComponentName();
        final ServiceName appContextServiceName = ContextNames.contextServiceNameOfApplication(applicationName);
        final ServiceName moduleContextServiceName = ContextNames.contextServiceNameOfModule(applicationName, moduleName);
        final ServiceName compContextServiceName = ContextNames.contextServiceNameOfComponent(applicationName, moduleName, compName);
        final Injector<NamingStore> appInjector = selector.getAppContextInjector();
        final Injector<NamingStore> moduleInjector = selector.getModuleContextInjector();
        final Injector<NamingStore> compInjector = selector.getCompContextInjector();
        final Injector<NamingStore> jbossInjector = selector.getJbossContextInjector();
        final Injector<NamingStore> globalInjector = selector.getGlobalContextInjector();
        final Injector<NamingStore> exportedInjector = selector.getExportedContextInjector();

        configuration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
            public void configureDependency(final ServiceBuilder<?> serviceBuilder, ComponentStartService service) {
                serviceBuilder.addDependency(appContextServiceName, NamingStore.class, appInjector);
                serviceBuilder.addDependency(moduleContextServiceName, NamingStore.class, moduleInjector);
                if (namingMode == ComponentNamingMode.CREATE) {
                    serviceBuilder.addDependency(compContextServiceName, NamingStore.class, compInjector);
                } else if(namingMode == ComponentNamingMode.USE_MODULE) {
                    serviceBuilder.addDependency(moduleContextServiceName, NamingStore.class, compInjector);
                }
                serviceBuilder.addDependency(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, NamingStore.class, globalInjector);
                serviceBuilder.addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, NamingStore.class, jbossInjector);
                serviceBuilder.addDependency(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME, NamingStore.class, exportedInjector);
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
