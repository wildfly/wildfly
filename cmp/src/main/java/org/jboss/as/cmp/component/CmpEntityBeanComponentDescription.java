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

package org.jboss.as.cmp.component;

import org.jboss.as.cmp.component.interceptors.CmpEntityBeanSynchronizationInterceptor;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentStartService;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.ViewInstanceFactory;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentDescription;
import org.jboss.as.ejb3.component.entity.EntityBeanHomeViewConfigurator;
import org.jboss.as.ejb3.component.entity.EntityBeanObjectViewConfigurator;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassIndex;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.metadata.ejb.spec.EntityBeanMetaData;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * @author John Bailey
 */
public class CmpEntityBeanComponentDescription extends EntityBeanComponentDescription {
    private JDBCEntityMetaData entityMetaData;

    public CmpEntityBeanComponentDescription(String componentName, String componentClassName, EjbJarDescription ejbJarDescription, ServiceName deploymentUnitServiceName, final EntityBeanMetaData descriptorData) {
        super(componentName, componentClassName, ejbJarDescription, deploymentUnitServiceName, descriptorData);

        getConfigurators().addFirst(new ComponentConfigurator() {
            public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                CmpEntityBeanComponentDescription cmpDescription = (CmpEntityBeanComponentDescription) description;

                final CmpInstanceReferenceFactory factory = new CmpInstanceReferenceFactory(configuration.getComponentClass(), cmpDescription.getEntityMetaData().getLocalHomeClass(), cmpDescription.getEntityMetaData().getHomeClass(), cmpDescription.getEntityMetaData().getLocalClass(), cmpDescription.getEntityMetaData().getRemoteClass());
                configuration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
                    public void configureDependency(final ServiceBuilder<?> serviceBuilder, final ComponentStartService service) throws DeploymentUnitProcessingException {
                        serviceBuilder.addDependency(description.getCreateServiceName(), CmpEntityBeanComponent.class, factory.getComponentInjector());
                    }
                });
                configuration.setInstanceFactory(factory);
}
        });
    }

    @Override
    public ComponentConfiguration createEntityBeanConfiguration(final ClassIndex classIndex, final ClassLoader moduleClassLoader, final ModuleLoader moduleLoader) {
        final ComponentConfiguration configuration = new ComponentConfiguration(this, classIndex, moduleClassLoader, moduleLoader);
        configuration.setComponentCreateServiceFactory(CmpEntityBeanComponentCreateService.FACTORY);
        return configuration;
    }

    @Override
    protected void addRemoveInterceptor() {
        //No-OP, the remove method is not actually forwarded to the component chain
    }

    protected EntityBeanObjectViewConfigurator getObjectViewConfigurator() {
        return new CmpEntityBeanObjectViewConfigurator();
    }

    protected EntityBeanHomeViewConfigurator getHomeViewConfigurator() {
        return new CmpEntityBeanHomeViewConfigurator();
    }

    protected InterceptorFactory getSynchronizationInterceptorFactory() {
        return CmpEntityBeanSynchronizationInterceptor.FACTORY;
    }

    protected ViewInstanceFactory getRemoteViewInstanceFactory(final String applicationName, final String moduleName, final String distinctName, final String componentName) {
        return new CmpEntityBeanRemoteViewInstanceFactory(applicationName, moduleName, distinctName, componentName);
    }

    public JDBCEntityMetaData getEntityMetaData() {
        return entityMetaData;
    }

    public void setEntityMetaData(JDBCEntityMetaData entityMetaData) {
        this.entityMetaData = entityMetaData;
    }
}
