/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.weld.util;

import java.util.Map;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.common.WebComponentDescription;

/**
 * Various utilities for working with WildFly APIs
 *
 * @author Jozef Hartinger
 *
 */
public class Utils {

    private Utils() {
    }

    public static boolean isClassesRoot(ResourceRoot resourceRoot) {
        return "classes".equals(resourceRoot.getRootName());
    }

    /**
     * Returns the parent of the given deployment unit if such a parent exists. If the given deployment unit is the parent
     * deployment unit, it is returned.
     */
    public static DeploymentUnit getRootDeploymentUnit(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.getParent() == null) {
            return deploymentUnit;
        }
        return deploymentUnit.getParent();
    }

    public static String getDeploymentUnitId(DeploymentUnit deploymentUnit) {
        String id = deploymentUnit.getName();
        if (deploymentUnit.getParent() != null) {
            id = deploymentUnit.getParent().getName() + "/" + id;
        }
        return id;
    }

    public static void registerAsComponent(String listener, DeploymentUnit deploymentUnit) {
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final EEModuleDescription module = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final WebComponentDescription componentDescription = new WebComponentDescription(listener, listener, module, deploymentUnit.getServiceName(),
                applicationClasses);
        module.addComponent(componentDescription);
        deploymentUnit.addToAttachmentList(WebComponentDescription.WEB_COMPONENTS, componentDescription.getStartServiceName());
    }

    public static <K, V> void putIfValueNotNull(Map<K, V> map, K key, V value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
