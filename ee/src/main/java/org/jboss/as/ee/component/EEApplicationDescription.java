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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * @author John Bailey
 */
public class EEApplicationDescription {
    private final Map<String, List<ViewInformation>> componentsByViewName = new HashMap<String, List<ViewInformation>>();
    private final Map<String, List<Description>> componentsByName = new HashMap<String, List<Description>>();
    private final Map<String, LazyValue<EEModuleClassConfiguration>> classesByName = new HashMap<String, LazyValue<EEModuleClassConfiguration>>();

    /**
     * Add a component to this application.
     *
     * @param description    the component description
     * @param deploymentRoot
     */
    public void addComponent(ComponentDescription description, final VirtualFile deploymentRoot) {
        for (ViewDescription viewDescription : description.getViews()) {
            List<ViewInformation> viewComponents = componentsByViewName.get(viewDescription.getViewClassName());
            if (viewComponents == null) {
                viewComponents = new ArrayList<ViewInformation>(1);
                componentsByViewName.put(viewDescription.getViewClassName(), viewComponents);
            }
            viewComponents.add(new ViewInformation(viewDescription, deploymentRoot, description.getComponentName()));
        }
        List<Description> components = componentsByName.get(description.getComponentName());
        if (components == null) {
            componentsByName.put(description.getComponentName(), components = new ArrayList<Description>(1));
        }
        components.add(new Description(description, deploymentRoot));
    }

    /**
     * Get all views that have the given type in the application
     *
     * @param viewType The view type
     * @return All views of the given type
     */
    public Set<ViewDescription> getComponentsForViewName(final String viewType) {
        final List<ViewInformation> info = componentsByViewName.get(viewType);

        if (info == null) {
            return Collections.<ViewDescription>emptySet();
        }
        final Set<ViewDescription> ret = new HashSet<ViewDescription>();
        for (ViewInformation i : info) {
            ret.add(i.viewDescription);
        }
        return ret;
    }

    /**
     * Get all components in the application that have the given name
     *
     * @param componentName  The name of the component
     * @param deploymentRoot The deployment root of the component doing the lookup
     * @return A set of all views for the given component name and type
     */
    public Set<ComponentDescription> getComponents(final String componentName, final VirtualFile deploymentRoot) {
        final List<Description> info = componentsByName.get(componentName);
        if (info == null) {
            return Collections.emptySet();
        }
        if (componentName.contains("#")) {
            final String[] parts = componentName.split("#");
            final String path = parts[0];
            final VirtualFile virtualPath = deploymentRoot.getChild(path);
            final String name = parts[1];
            final Set<ComponentDescription> ret = new HashSet<ComponentDescription>();
            for (Description i : info) {
                //now we need to check the path
                if (virtualPath.equals(i.deploymentRoot)) {
                    ret.add(i.componentDescription);
                }
            }
            return ret;
        } else {
            final Set<ComponentDescription> all = new HashSet<ComponentDescription>();
            final Set<ComponentDescription> thisDeployment = new HashSet<ComponentDescription>();
            for (Description i : info) {
                all.add(i.componentDescription);
                if(i.deploymentRoot.equals(deploymentRoot)) {
                    thisDeployment.add(i.componentDescription);
                }
            }
            //if there are multiple e
            if (all.size() > 1) {
                return thisDeployment;
            }
            return all;
        }
    }

    /**
     * Get all views in the application that have the given name and view type
     *
     * @param componentName  The name of the component
     * @param viewName       The view type
     * @param deploymentRoot The deployment root of the component doing the lookup
     * @return A set of all views for the given component name and type
     */
    public Set<ViewDescription> getComponents(final String componentName, final String viewName, final VirtualFile deploymentRoot) {
        final List<ViewInformation> info = componentsByViewName.get(viewName);
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
                if (i.beanName.equals(name)) {
                    //now we need to check the path
                    if (virtualPath.equals(i.deploymentRoot)) {
                        ret.add(i.viewDescription);
                    }
                }
            }
            return ret;
        } else {
            final Set<ViewDescription> all = new HashSet<ViewDescription>();
            final Set<ViewDescription> thisDeployment = new HashSet<ViewDescription>();
            for (ViewInformation i : info) {
                if (i.beanName.equals(componentName)) {
                    all.add(i.viewDescription);
                    if(i.deploymentRoot.equals(deploymentRoot)) {
                        thisDeployment.add(i.viewDescription);
                    }
                }
            }
            if (all.size() > 1) {
                return thisDeployment;
            }
            return all;
        }
    }

    public void addClass(final String className, LazyValue<EEModuleClassConfiguration> eeModuleClassDescription) {
        classesByName.put(className, eeModuleClassDescription);
    }

    public EEModuleClassConfiguration getClassConfiguration(String name) {
        LazyValue<EEModuleClassConfiguration> result =  classesByName.get(name);
        if(result == null) {
            return null;
        }
        try {
            return result.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
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

    private static class Description {
        private final ComponentDescription componentDescription;
        private final VirtualFile deploymentRoot;

        public Description(final ComponentDescription componentDescription, final VirtualFile deploymentRoot) {
            this.componentDescription = componentDescription;
            this.deploymentRoot = deploymentRoot;
        }
    }


}
