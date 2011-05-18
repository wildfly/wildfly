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

package org.jboss.as.ee.component;

import org.jboss.vfs.VirtualFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author John Bailey
 */
public class EEApplicationDescription {
    private final Map<String, Set<ViewInformation>> componentsByViewName = new HashMap<String, Set<ViewInformation>>();

    /**
     * Add a component to this application.
     *
     * @param description    the component description
     * @param deploymentRoot
     */
    public void addComponent(ComponentDescription description, final VirtualFile deploymentRoot) {
        for (ViewDescription viewDescription : description.getViews()) {
            Set<ViewInformation> viewComponents = componentsByViewName.get(viewDescription.getViewClassName());
            if (viewComponents == null) {
                viewComponents = new HashSet<ViewInformation>();
                componentsByViewName.put(viewDescription.getViewClassName(), viewComponents);
            }
            viewComponents.add(new ViewInformation(viewDescription, deploymentRoot, description.getComponentName()));
        }
    }

    public Set<ViewDescription> getComponentsForViewName(final String name) {
        final Set<ViewInformation> info = componentsByViewName.get(name);

        if (info == null) {
            return Collections.<ViewDescription>emptySet();
        }
        final Set<ViewDescription> ret = new HashSet<ViewDescription>();
        for (ViewInformation i : info) {
            ret.add(i.viewDescription);
        }
        return ret;
    }

    public Set<ViewDescription> getComponents(final String componentName, final String viewName, final VirtualFile deploymentRoot) {
        final Set<ViewInformation> info = componentsByViewName.get(viewName);
        if (info == null) {
            return Collections.<ViewDescription>emptySet();
        }
        if (componentName.contains("#")) {
            final String[] parts = componentName.split("#");
            final String path = parts[0];
            final VirtualFile virtualPath = deploymentRoot.getChild(path);
            final String name = parts[1];
            final Set<ViewDescription> ret = new HashSet<ViewDescription>();
            for (ViewInformation i : info) {
                if(i.beanName.equals(name)) {
                    //now we need to check the path
                    if(virtualPath.equals(i.deploymentRoot)) {
                        ret.add(i.viewDescription);
                    }
                }
            }
            return ret;
        } else {
            final Set<ViewDescription> ret = new HashSet<ViewDescription>();
            for (ViewInformation i : info) {
                if(i.beanName.equals(componentName)) {
                    ret.add(i.viewDescription);
                }
            }
            return ret;
        }

    }

    private static class ViewInformation {
        private final ViewDescription viewDescription;
        private final VirtualFile deploymentRoot;
        private final String beanName;


        public ViewInformation(final ViewDescription viewDescription, final VirtualFile deploymentRoot, final String beanName) {
            this.viewDescription = viewDescription;
            this.deploymentRoot = deploymentRoot;
            this.beanName = beanName;
        }
    }


}
