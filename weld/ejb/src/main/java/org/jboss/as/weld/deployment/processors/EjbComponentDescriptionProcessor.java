/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment.processors;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.weld.ejb.EjbDescriptorImpl;
import org.jboss.as.weld.spi.ComponentDescriptionProcessor;
import org.jboss.as.weld.spi.WildFlyBeanDeploymentArchive;
import org.jboss.weld.util.collections.Multimap;
import org.jboss.weld.util.collections.SetMultimap;

/**
 *
 * @author Martin Kouba
 */
public class EjbComponentDescriptionProcessor implements ComponentDescriptionProcessor {

    private final Multimap<ResourceRoot, EJBComponentDescription> ejbComponentDescriptions = SetMultimap.newSetMultimap();

    @Override
    public void processComponentDescription(ResourceRoot resourceRoot, ComponentDescription component) {
        if (component instanceof EJBComponentDescription) {
            ejbComponentDescriptions.put(resourceRoot, (EJBComponentDescription) component);
        }
    }

    @Override
    public boolean hasBeanComponents(ResourceRoot resourceRoot) {
        return !ejbComponentDescriptions.get(resourceRoot).isEmpty();
    }

    @Override
    public void registerComponents(ResourceRoot resourceRoot, WildFlyBeanDeploymentArchive beanDeploymentArchive, DeploymentReflectionIndex reflectionIndex) {
        for (EJBComponentDescription ejb : ejbComponentDescriptions.get(resourceRoot)) {
            beanDeploymentArchive.addEjbDescriptor(new EjbDescriptorImpl<Object>(ejb, beanDeploymentArchive, reflectionIndex));
            beanDeploymentArchive.addBeanClass(ejb.getComponentClassName());
        }
    }

}
