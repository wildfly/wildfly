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

package org.jboss.as.ejb3.component.stateless;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentBinding;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentFactory;
import org.jboss.as.ee.component.service.ComponentObjectFactory;
import org.jboss.as.ee.naming.ContextNames;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;

import java.util.Arrays;
import java.util.Collection;

/**
 * Author : Jaikiran Pai
 */
public class StatelessSessionComponentFactory implements ComponentFactory {

    @Override
    public Component createComponent(DeploymentUnit deploymentUnit, ComponentConfiguration componentConfiguration) {
        ClassLoader cl = this.getClassLoader(deploymentUnit);
        DeploymentReflectionIndex reflectionIndex = deploymentUnit.getAttachment(Attachments.REFLECTION_INDEX);
        return new StatelessSessionComponent(componentConfiguration, cl, reflectionIndex);
    }

    private ClassLoader getClassLoader(DeploymentUnit deploymentUnit) {
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            throw new IllegalStateException("Module not found for deployment unit: " + deploymentUnit);
        }
        return module.getClassLoader();
    }

    @Override
    public Collection<ComponentBinding> getComponentBindings(DeploymentUnit deploymentUnit, ComponentConfiguration componentConfiguration, ServiceName componentServiceName) {
//        final NamingContextConfig appNamespaceConfig = deploymentUnit.getAttachment(org.jboss.as.ee.naming.Attachments.APPLICATION_CONTEXT_CONFIG);
//        final NamingContextConfig moduleNamespaceConfig = deploymentUnit.getAttachment(org.jboss.as.ee.naming.Attachments.MODULE_CONTEXT_CONFIG);

        // EJB 3.1 FR 4.4.1

        // If it is an EAR set the appName
        String appName = null;

        // TODO: needs to take descriptor overrides into account
        String moduleName = deploymentUnit.getName();

        String beanName = componentConfiguration.getName();

        String globalJNDIName = (appName != null ? appName + "/" : "") + moduleName + "/" + beanName;
        return Arrays.asList(
                new ComponentBinding(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, globalJNDIName, ComponentObjectFactory.createReference(componentServiceName, componentConfiguration.getComponentClass()))
//                new ComponentBinding(moduleNamespaceConfig.getContextServiceName(), componentConfiguration.getName(), ComponentObjectFactory.createReference(componentServiceName, componentConfiguration.getComponentClass())),
//                new ComponentBinding(appNamespaceConfig.getContextServiceName().append(deploymentUnit.getName()), componentConfiguration.getName(), new LinkRef(ContextNames.MODULE_CONTEXT_NAME.append(componentConfiguration.getName()).getAbsoluteName()))
        );

    }
}
