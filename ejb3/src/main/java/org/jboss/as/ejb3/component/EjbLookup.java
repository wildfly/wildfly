/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VirtualFile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class that can be used to locate an EJB given an EJB name and
 * interface type.
 *
 * @author Stuart Douglas
 */
public class EjbLookup {

    private static final Logger log = Logger.getLogger("org.jboss.as.ejb");

    private final Map<String, EjbDescription> ejbs;
    private final Map<Class<?>, Set<EjbDescription>> ejbsByViewType;

    public EjbLookup(final Map<String, EjbDescription> ejbs) {
        this.ejbs = ejbs;
        final Map<Class<?>, Set<EjbDescription>> ejbsByViewType = new HashMap<Class<?>, Set<EjbDescription>>();
        for (final EjbDescription ejb : ejbs.values()) {
            for (final Class<?> view : ejb.serviceNames.keySet()) {
                Set<EjbDescription> ejbList = ejbsByViewType.get(view);
                if (ejbList == null) {
                    ejbsByViewType.put(view, ejbList = new HashSet<EjbDescription>());
                }
                ejbList.add(ejb);
            }
        }
        this.ejbsByViewType = ejbsByViewType;
    }


    public ServiceName getViewService(String ejbName, Class<?> type) {
        final EjbDescription description = ejbs.get(ejbName);
        if (description == null) {
            throw new RuntimeException("Could not find EJB with name " + ejbName);
        }
        if (type == null) {
            if (description.serviceNames.size() == 1) {
                return description.serviceNames.values().iterator().next();
            } else if (description.serviceNames.isEmpty()) {
                throw new RuntimeException("EJB " + ejbName + " does not have any views");
            } else {
                throw new RuntimeException("Could not determine view type to inject for ejb " + ejbName);
            }
        }
        if (description.serviceNames.size() == 1) {
            return description.serviceNames.values().iterator().next();
        }
        final ServiceName serviceName = description.serviceName(type);
        if (serviceName == null) {
            throw new RuntimeException("Could not find a view of type " + type + " for EJB " + ejbName);
        }
        return serviceName;
    }

    public ServiceName getViewService(Class<?> type) {
        final Set<EjbDescription> ejbs = this.ejbsByViewType.get(type);
        if (ejbs == null || ejbs.isEmpty()) {
            throw new RuntimeException("Could not resolve EJB with a view type of " + type);
        }
        if (ejbs.size() == 1) {
            return ejbs.iterator().next().serviceName(type);
        }
        EjbDescription localEjb = null;
        for (EjbDescription ejb : ejbs) {
            if (ejb.deployedInCurrentModule) {
                if (localEjb != null) {
                    throw new RuntimeException("More than one bean exists with a view of type " + type + " Beans: " + ejbs);
                } else {
                    localEjb = ejb;
                }
            }
        }
        if (localEjb != null) {
            return localEjb.serviceName(type);
        }
        throw new RuntimeException("More than one bean exists with a view of type " + type + " Beans: " + ejbs);
    }

    public static Builder builder(DeploymentUnit deploymentUnit) {
        return new Builder(deploymentUnit);
    }


    private static final class EjbDescription {
        private final Map<Class<?>, ServiceName> serviceNames;
        private final boolean deployedInCurrentModule;
        private final String ejbName;

        public EjbDescription(final Map<Class<?>, ServiceName> serviceNames, final boolean deployedInCurrentModule, final String ejbName) {
            this.serviceNames = serviceNames;
            this.deployedInCurrentModule = deployedInCurrentModule;
            this.ejbName = ejbName;
        }

        public ServiceName serviceName(Class<?> viewType) {
            return serviceNames.get(viewType);
        }

        @Override
        public String toString() {
            return ejbName;
        }
    }

    public static class Builder {

        private final DeploymentUnit deploymentUnit;
        private final VirtualFile jarRoot;

        private final Map<String, EjbDescription> ejbs = new HashMap<String, EjbDescription>();

        public Builder(final DeploymentUnit deploymentUnit) {
            this.deploymentUnit = deploymentUnit;
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            jarRoot = deploymentRoot.getRoot().getParent();
        }

        public void addEjb(final DeploymentUnit ejbDeploymentUnit, EJBComponentDescription componentDescription, final ClassLoader classLoader) {

            final Map<Class<?>, ServiceName> views = new HashMap<Class<?>, ServiceName>();

            for (final ViewDescription view : componentDescription.getViews()) {
                try {
                    final Class<?> viewClass = classLoader.loadClass(view.getViewClassName());
                    views.put(viewClass, view.getServiceName());
                } catch (ClassNotFoundException e) {
                    //we do not fail here, as this failure should be handed by the actual view processor for this deplyment
                    log.warn("Could not load view class " + view.getViewClassName() + " for ejb " + componentDescription.getEJBClassName());
                }
            }
            boolean deployedInCurrentModule = ejbDeploymentUnit.equals(deploymentUnit);
            final EjbDescription description = new EjbDescription(views, deployedInCurrentModule, componentDescription.getEJBName());
            if (deployedInCurrentModule || !ejbs.containsKey(componentDescription.getEJBName())) {
                ejbs.put(componentDescription.getEJBName(), description);
            }

            final ResourceRoot deploymentRoot = ejbDeploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            if (deploymentRoot != null) {
                ejbs.put("../" + deploymentRoot.getRoot().getPathNameRelativeTo(jarRoot) + "#" + componentDescription.getEJBName(), description);
            }
        }

        public EjbLookup build() {
            return new EjbLookup(ejbs);
        }

    }

}
