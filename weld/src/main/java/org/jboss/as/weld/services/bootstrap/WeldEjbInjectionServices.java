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
package org.jboss.as.weld.services.bootstrap;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import javax.ejb.EJB;
import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.ContextNames.BindInfo;
import org.jboss.as.weld.WeldMessages;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

/**
 * Implementation of EjbInjectionServices.
 *
 * @author Stuart Douglas
 */
public class WeldEjbInjectionServices extends AbstractResourceInjectionServices implements EjbInjectionServices {

    private final EEApplicationDescription applicationDescription;

    private final VirtualFile deploymentRoot;


    public WeldEjbInjectionServices(ServiceRegistry serviceRegistry, EEModuleDescription moduleDescription, final EEApplicationDescription applicationDescription, final VirtualFile deploymentRoot) {
        super(serviceRegistry, moduleDescription);
        if (serviceRegistry == null) {
            throw WeldMessages.MESSAGES.parameterCannotBeNull("serviceRegistry");
        }
        if (moduleDescription == null) {
            throw WeldMessages.MESSAGES.parameterCannotBeNull("moduleDescription");
        }
        if (applicationDescription == null) {
            throw WeldMessages.MESSAGES.parameterCannotBeNull("applicationDescription");
        }
        if (deploymentRoot == null) {
            throw WeldMessages.MESSAGES.parameterCannotBeNull("deploymentRoot");
        }

        this.applicationDescription = applicationDescription;
        this.deploymentRoot = deploymentRoot;
    }

    @Override
    public Object resolveEjb(InjectionPoint injectionPoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceReferenceFactory<Object> registerEjbInjectionPoint(final InjectionPoint injectionPoint) {
        EJB ejb = injectionPoint.getAnnotated().getAnnotation(EJB.class);
        if (ejb == null) {
            throw WeldMessages.MESSAGES.annotationNotFound(EJB.class, injectionPoint.getMember());
        }
        if (injectionPoint.getMember() instanceof Method && ((Method) injectionPoint.getMember()).getParameterTypes().length != 1) {
            throw WeldMessages.MESSAGES.injectionPointNotAJavabean((Method) injectionPoint.getMember());
        }
        if (!ejb.lookup().equals("")) {
            return handleServiceLookup(ejb.lookup(), injectionPoint);
        } else {
            final ViewDescription viewDescription = getViewDescription(ejb, injectionPoint);
            return handleServiceLookup(viewDescription, injectionPoint);
        }
    }

    private ResourceReferenceFactory<Object> handleServiceLookup(ViewDescription viewDescription, InjectionPoint injectionPoint) {
        /*
         * Try to obtain ComponentView eagerly and validate the resource type
         */
        final ComponentView view = getComponentView(viewDescription);
        if (view != null) {
            Class<?> clazz = view.getViewClass();
            validateResourceInjectionPointType(clazz, injectionPoint);
            return new ComponentViewToResourceReferenceFactoryAdapter<Object>(view);
        } else {
            return createLazyResourceReferenceFactory(viewDescription);
        }
    }

    private ComponentView getComponentView(ViewDescription viewDescription) {
        final ServiceController<?> controller = serviceRegistry.getRequiredService(viewDescription.getServiceName());
        return (ComponentView) controller.getValue();
    }

    private ViewDescription getViewDescription(EJB ejb, InjectionPoint injectionPoint) {
        final Set<ViewDescription> viewService;
        if (ejb.beanName().isEmpty()) {
            if (ejb.beanInterface() != Object.class) {
                viewService = applicationDescription.getComponentsForViewName(ejb.beanInterface().getName(), deploymentRoot);
            } else {
                viewService = applicationDescription.getComponentsForViewName(getType(injectionPoint.getType()).getName(), deploymentRoot);
            }
        } else {
            if (ejb.beanInterface() != Object.class) {
                viewService = applicationDescription.getComponents(ejb.beanName(), ejb.beanInterface().getName(), deploymentRoot);
            } else {
                viewService = applicationDescription.getComponents(ejb.beanName(), getType(injectionPoint.getType()).getName(), deploymentRoot);
            }
        }
        if (viewService.isEmpty()) {
            throw WeldMessages.MESSAGES.ejbNotResolved(ejb, injectionPoint.getMember());
        } else if (viewService.size() > 1) {
            throw WeldMessages.MESSAGES.moreThanOneEjbResolved(ejb, injectionPoint.getMember(), viewService);
        }
        return viewService.iterator().next();
    }

    @Override
    protected BindInfo getBindInfo(String result) {
        return ContextNames.bindInfoFor(moduleDescription.getApplicationName(), moduleDescription.getModuleName(), moduleDescription.getModuleName(), result);
    }

    @Override
    public void cleanup() {

    }

    private static Class<?> getType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return getType(((ParameterizedType) type).getRawType());
        } else {
            throw WeldMessages.MESSAGES.couldNotDetermineUnderlyingType(type);
        }
    }

    protected ResourceReferenceFactory<Object> createLazyResourceReferenceFactory(final ViewDescription viewDescription) {
        return new ResourceReferenceFactory<Object>() {
            @Override
            public ResourceReference<Object> createResource() {
                final ManagedReference instance;
                try {
                    final ServiceController<?> controller = serviceRegistry.getRequiredService(viewDescription.getServiceName());
                    final ComponentView view = (ComponentView) controller.getValue();
                    instance = view.createInstance();
                    return new ResourceReference<Object>() {
                        @Override
                        public Object getInstance() {
                            return instance.getInstance();
                        }

                        @Override
                        public void release() {
                            instance.release();
                        }
                    };
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
