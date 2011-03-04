/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.session;

import org.jboss.as.ee.component.BindingDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ServiceBindingSourceDescription;
import org.jboss.as.ejb3.component.EJBComponentConfiguration;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Author : Jaikiran Pai
 */
public abstract class AbstractSessionComponentFactory {
    protected static ClassLoader getClassLoader(DeploymentUnit deploymentUnit) {
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            throw new IllegalStateException("Module not found for deployment unit: " + deploymentUnit);
        }
        return module.getClassLoader();
    }

    public Collection<BindingDescription> getComponentBindings(DeploymentUnit deploymentUnit, EJBComponentConfiguration componentConfiguration, ServiceName componentServiceName) {
//        final NamingContextConfig appNamespaceConfig = deploymentUnit.getAttachment(org.jboss.as.ee.naming.Attachments.APPLICATION_CONTEXT_CONFIG);
//        final NamingContextConfig moduleNamespaceConfig = deploymentUnit.getAttachment(org.jboss.as.ee.naming.Attachments.MODULE_CONTEXT_CONFIG);

        // EJB 3.1 FR 4.4.1

        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);

        // If it is an EAR set the appName
        //final String applicationName = deploymentUnit.getParent() == null ? deploymentUnit.getName() : deploymentUnit.getParent().getName();
        //final String applicationName = moduleDescription.getAppName();
        final String applicationName = null;

        final String moduleName = moduleDescription.getModuleName();

        String beanName = componentConfiguration.getName();

        String globalJNDIName = (applicationName != null ? applicationName + "/" : "") + moduleName + "/" + beanName;
        Collection<BindingDescription> bindings = new LinkedList<BindingDescription>();
        // TODO: should come directly from componentConfiguration
        for(String viewClassName : componentConfiguration.getDescription().getViewClassNames()) {
            final BindingDescription globalBinding = new BindingDescription();
            globalBinding.setAbsoluteBinding(true);
            globalBinding.setBindingName("java:global/" + globalJNDIName + "!" + viewClassName);
            globalBinding.setBindingType(viewClassName);
            //globalBinding.setReferenceSourceDescription(new ServiceBindingSourceDescription(baseName.append("VIEW").append(beanClassName)));
            globalBinding.setReferenceSourceDescription(new ServiceBindingSourceDescription(componentServiceName));
            bindings.add(globalBinding);
        }
        return bindings;
    }
}
