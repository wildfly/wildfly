/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.spi;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;

/**
 * Jakarta EE component description processor.
 * <p>
 * Processors may be stateful and are not suitable for sharing between threads.
 *
 * @author Martin Kouba
 */
public interface ComponentDescriptionProcessor {

    /**
     *
     * @param description
     */
    void processComponentDescription(ResourceRoot resourceRoot, ComponentDescription component);

    /**
     *
     * @param resourceRoot
     * @return <code>true</code> if any components were previously processed by this handler, <code>false</code> otherwise
     * @see ComponentDescriptionProcessor#processComponentDescription(ResourceRoot, ComponentDescription)
     */
    boolean hasBeanComponents(ResourceRoot resourceRoot);

    /**
     *
     * @param beanDeploymentArchive
     */
    void registerComponents(ResourceRoot resourceRoot, WildFlyBeanDeploymentArchive beanDeploymentArchive, DeploymentReflectionIndex reflectionIndex);

}
