/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.weld.util;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.managedbean.component.ManagedBeanComponentDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;

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
            id = deploymentUnit.getParent().getName() + "." + id;
        }
        return id;
    }

    public static boolean isComponentWithView(ComponentDescription component) {
        return (component instanceof EJBComponentDescription) || (component instanceof ManagedBeanComponentDescription);
    }
}
