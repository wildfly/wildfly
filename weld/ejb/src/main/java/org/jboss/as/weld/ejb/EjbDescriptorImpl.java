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
package org.jboss.as.weld.ejb;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.msc.service.ServiceName;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.ejb.spi.BusinessInterfaceDescriptor;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.resources.spi.ResourceLoader;

/**
 * Implementation of EjbDescriptor
 *
 * @author Stuart Douglas
 */
public class EjbDescriptorImpl<T> implements EjbDescriptor<T> {

    private final ServiceName baseName;
    private final Set<BusinessInterfaceDescriptor<?>> localInterfaces;
    private final Set<BusinessInterfaceDescriptor<?>> remoteInterfaces;
    private final Map<Class<?>, ServiceName> viewServices;
    private final Set<Method> removeMethods;
    private final Class<T> ejbClass;
    private final String ejbName;


    private final boolean stateless;
    private final boolean stateful;
    private final boolean singleton;
    private final boolean messageDriven;
    private final boolean passivationCapable;

    public EjbDescriptorImpl(EJBComponentDescription componentDescription, BeanDeploymentArchive beanDeploymentArchive, final DeploymentReflectionIndex reflectionIndex) {
        final SessionBeanComponentDescription description = componentDescription instanceof SessionBeanComponentDescription ? (SessionBeanComponentDescription) componentDescription : null;
        final Set<BusinessInterfaceDescriptor<?>> localInterfaces = new HashSet<BusinessInterfaceDescriptor<?>>();
        final Set<BusinessInterfaceDescriptor<?>> remoteInterfaces = new HashSet<BusinessInterfaceDescriptor<?>>();
        final ResourceLoader loader = beanDeploymentArchive.getServices().get(ResourceLoader.class);

        ejbClass = (Class<T>) loader.classForName(componentDescription.getEJBClassName());

        if (componentDescription.getViews() != null) {
            for (ViewDescription view : componentDescription.getViews()) {

                if (description == null || getMethodIntf(view) == MethodIntf.LOCAL) {
                    final String viewClassName = view.getViewClassName();
                    localInterfaces.add(new BusinessInterfaceDescriptorImpl<Object>(beanDeploymentArchive, viewClassName));
                } else if (getMethodIntf(view) == MethodIntf.REMOTE) {
                    remoteInterfaces.add(new BusinessInterfaceDescriptorImpl<Object>(beanDeploymentArchive, view.getViewClassName()));
                }
            }
        }
        if(componentDescription instanceof StatefulComponentDescription) {
            Set<Method> removeMethods = new HashSet<Method>();
            final Collection<StatefulComponentDescription.StatefulRemoveMethod> methods = ((StatefulComponentDescription) componentDescription).getRemoveMethods();
            for(final StatefulComponentDescription.StatefulRemoveMethod method : methods) {
                Class<?> c = ejbClass;
                while (c != null && c != Object.class) {
                    ClassReflectionIndex index = reflectionIndex.getClassIndex(c);
                    Method m = index.getMethod(method.getMethodIdentifier());
                    if(m != null) {
                        removeMethods.add(m);
                    }
                    c = c.getSuperclass();
                }
            }
            this.removeMethods = Collections.unmodifiableSet(removeMethods);
        } else {
            removeMethods = Collections.emptySet();
        }


        this.ejbName = componentDescription.getEJBName();
        this.localInterfaces = localInterfaces;
        this.remoteInterfaces = remoteInterfaces;
        this.baseName = componentDescription.getServiceName();
        this.stateless = componentDescription.isStateless();
        this.messageDriven = componentDescription.isMessageDriven();
        this.stateful = componentDescription.isStateful();
        this.singleton = componentDescription.isSingleton();

        this.passivationCapable = componentDescription.isPassivationApplicable();

        final Map<Class<?>, ServiceName> viewServices = new HashMap<Class<?>, ServiceName>();
        final Map<String, Class<?>> views = new HashMap<String, Class<?>>();

        Map<Class<?>, ServiceName> viewServicesMap = new HashMap<Class<?>, ServiceName>();
        for (ViewDescription view : componentDescription.getViews()) {
            viewServicesMap.put(loader.classForName(view.getViewClassName()), view.getServiceName());
        }
        for (BusinessInterfaceDescriptor<?> view : remoteInterfaces) {
            views.put(view.getInterface().getName(), view.getInterface());
        }
        for (BusinessInterfaceDescriptor<?> view : localInterfaces) {
            views.put(view.getInterface().getName(), view.getInterface());
        }

        for (Map.Entry<Class<?>, ServiceName> entry : viewServicesMap.entrySet()) {
            final Class<?> viewClass = entry.getKey();
            if (viewClass != null) {
                //see WELD-921
                //this is horrible, but until it is fixed there is not much that can be done

                final Set<Class<?>> seen = new HashSet<Class<?>>();
                final Set<Class<?>> toProcess = new HashSet<Class<?>>();

                toProcess.add(viewClass);

                while (!toProcess.isEmpty()) {
                    Iterator<Class<?>> it = toProcess.iterator();
                    final Class<?> clazz = it.next();
                    it.remove();
                    seen.add(clazz);
                    viewServices.put(clazz, entry.getValue());
                    final Class<?> superclass = clazz.getSuperclass();
                    if (superclass != Object.class && superclass != null && !seen.contains(superclass)) {
                        toProcess.add(superclass);
                    }
                    for (Class<?> iface : clazz.getInterfaces()) {
                        if (!seen.contains(iface)) {
                            toProcess.add(iface);
                        }
                    }
                }
            }
        }
        this.viewServices = viewServices;
    }

    private MethodIntf getMethodIntf(final ViewDescription view) {
        if (view instanceof EJBViewDescription) {
            final EJBViewDescription ejbView = (EJBViewDescription) view;
            return ejbView.getMethodIntf();
        }

        return null;
    }

    @Override
    public Class<T> getBeanClass() {
        return ejbClass;
    }

    @Override
    public Collection<BusinessInterfaceDescriptor<?>> getLocalBusinessInterfaces() {
        return localInterfaces;
    }

    @Override
    public Collection<BusinessInterfaceDescriptor<?>> getRemoteBusinessInterfaces() {
        return remoteInterfaces;
    }

    @Override
    public String getEjbName() {
        return ejbName;
    }

    @Override
    public Collection<Method> getRemoveMethods() {
        return removeMethods;
    }

    @Override
    public boolean isStateless() {
        return stateless;
    }

    @Override
    public boolean isSingleton() {
        return singleton;
    }

    @Override
    public boolean isStateful() {
        return stateful;
    }

    @Override
    public boolean isMessageDriven() {
        return messageDriven;
    }

    public ServiceName getBaseName() {
        return baseName;
    }

    public ServiceName getCreateServiceName() {
        return baseName.append("CREATE");
    }

    public ServiceName getStartServiceName() {
        return baseName.append("START");
    }

    public Map<Class<?>, ServiceName> getViewServices() {
        return viewServices;
    }

    @Override
    public boolean isPassivationCapable() {
        return passivationCapable;
    }

}
