/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
