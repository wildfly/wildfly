/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.weld.spi;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;

/**
 * Java EE component description processor.
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
