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
package org.jboss.as.weld.deployment.processors;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.injection.WeldInjectionInterceptor;
import org.jboss.as.weld.injection.WeldManagedReferenceFactory;
import org.jboss.as.weld.services.BeanManagerService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * Deployment unit processor that add the {@link org.jboss.as.weld.injection.WeldManagedReferenceFactory} instantiator
 * to components that are part of a bean archive.
 *
 * @author Stuart Douglas
 */
public class WeldComponentIntegrationProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            return;
        }


        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final ServiceName beanManagerServiceName = BeanManagerService.serviceName(deploymentUnit);

        final ServiceName serviceName = deploymentUnit.getServiceName().append("WeldComponentInstantiatorService");
        final InjectedValue<BeanManagerImpl> beanManager = new InjectedValue<BeanManagerImpl>();
        phaseContext.getServiceTarget().addService(serviceName, Service.NULL)
            .addDependency(beanManagerServiceName, BeanManagerImpl.class, beanManager)
            .install();

        for (ComponentDescription component : eeModuleDescription.getComponentDescriptions()) {
            final String beanName;
            if (component instanceof SessionBeanComponentDescription) {
                beanName = component.getComponentName();
            } else {
                beanName = null;
            }
            component.getConfigurators().addFirst(new ComponentConfigurator() {
                @Override
                public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                    final Class<?> componentClass = configuration.getModuleClassConfiguration().getModuleClass();

                    addWeldInstantiator(configuration, componentClass, beanName, serviceName, beanManager);

                    configuration.getPostConstructInterceptors().addLast(new WeldInjectionInterceptor.Factory(configuration));
                }
            });

        }

    }

    /**
     * As the weld based instantiator needs access to the bean manager it is installed as a service.
     */
    private void addWeldInstantiator(final ComponentConfiguration configuration, final Class<?> componentClass, final String beanName, final ServiceName serviceName, final InjectedValue<BeanManagerImpl> beanManager) {
        final WeldManagedReferenceFactory factory = new WeldManagedReferenceFactory(componentClass, beanName, beanManager);
        configuration.setInstanceFactory(factory);
        configuration.getStartDependencies().add(new DependencyConfigurator() {
            @Override
            public void configureDependency(final ServiceBuilder<?> serviceBuilder) throws DeploymentUnitProcessingException {
                serviceBuilder.addDependency(serviceName);
            }
        });
    }


    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
