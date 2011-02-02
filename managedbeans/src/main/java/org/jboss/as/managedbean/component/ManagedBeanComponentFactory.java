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

package org.jboss.as.managedbean.component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.naming.LinkRef;
import javax.naming.Reference;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentBinding;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentFactory;
import org.jboss.as.ee.component.injection.ResourceInjection;
import org.jboss.as.ee.component.interceptor.ComponentInterceptorFactories;
import org.jboss.as.ee.component.lifecycle.ComponentLifecycle;
import org.jboss.as.ee.component.service.ComponentObjectFactory;
import org.jboss.as.ee.naming.Attachments;
import org.jboss.as.ee.naming.ContextNames;
import org.jboss.as.ee.naming.NamingContextConfig;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;

/**
 * Manged-bean specific implementation of a {@link org.jboss.as.ee.component.ComponentFactory}.
 *
 * @author John Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ManagedBeanComponentFactory implements ComponentFactory {

    /**
     * The singleton instance.
     */
    public static final ManagedBeanComponentFactory INSTANCE = new ManagedBeanComponentFactory();

    private ManagedBeanComponentFactory() {
    }

    public Component createComponent(final DeploymentUnit deploymentUnit, final ComponentConfiguration componentConfiguration) throws DeploymentUnitProcessingException {
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        componentConfiguration.addViewClassName(componentConfiguration.getComponentClassName());
        final ClassLoader classLoader = module.getClassLoader();
        return new ManagedBeanComponent(componentConfiguration, classLoader, deploymentUnit.getAttachment(Attachments.REFLECTION_INDEX));
    }


    public Collection<ComponentBinding> getComponentBindings(final DeploymentUnit deploymentUnit, final ComponentConfiguration componentConfiguration, final ServiceName componentServiceName) {
        final NamingContextConfig appNamespaceConfig = deploymentUnit.getAttachment(Attachments.APPLICATION_CONTEXT_CONFIG);
        final NamingContextConfig moduleNamespaceConfig = deploymentUnit.getAttachment(Attachments.MODULE_CONTEXT_CONFIG);

        return Arrays.asList(
                new ComponentBinding(moduleNamespaceConfig.getContextServiceName(), componentConfiguration.getName(), ComponentObjectFactory.createReference(componentServiceName, componentConfiguration.getComponentClass())),
                new ComponentBinding(appNamespaceConfig.getContextServiceName().append(deploymentUnit.getName()), componentConfiguration.getName(), new LinkRef(ContextNames.MODULE_CONTEXT_NAME.append(componentConfiguration.getName()).getAbsoluteName()))
        );
    }
}
