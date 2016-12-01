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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import javax.ejb.EJB;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.ContextNames.BindInfo;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.util.ResourceInjectionUtilities;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.injection.spi.helpers.SimpleResourceReference;
import org.jboss.weld.logging.BeanLogger;
import org.jboss.weld.util.reflection.Reflections;

/**
 * Implementation of EjbInjectionServices.
 *
 * @author Stuart Douglas
 */
public class WeldEjbInjectionServices extends AbstractResourceInjectionServices implements EjbInjectionServices {

    private final EEApplicationDescription applicationDescription;

    private final VirtualFile deploymentRoot;

    private final boolean warModule;


    public WeldEjbInjectionServices(ServiceRegistry serviceRegistry, EEModuleDescription moduleDescription, final EEApplicationDescription applicationDescription, final VirtualFile deploymentRoot, Module module, boolean warModule) {
        super(serviceRegistry, moduleDescription, module);
        this.warModule = warModule;
        if (serviceRegistry == null) {
            throw WeldLogger.ROOT_LOGGER.parameterCannotBeNull("serviceRegistry");
        }
        if (moduleDescription == null) {
            throw WeldLogger.ROOT_LOGGER.parameterCannotBeNull("moduleDescription");
        }
        if (applicationDescription == null) {
            throw WeldLogger.ROOT_LOGGER.parameterCannotBeNull("applicationDescription");
        }
        if (deploymentRoot == null) {
            throw WeldLogger.ROOT_LOGGER.parameterCannotBeNull("deploymentRoot");
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
        EJB ejb = ResourceInjectionUtilities.getResourceAnnotated(injectionPoint).getAnnotation(EJB.class);
        if (ejb == null) {
            throw WeldLogger.ROOT_LOGGER.annotationNotFound(EJB.class, injectionPoint.getMember());
        }
        if (injectionPoint.getMember() instanceof Method && ((Method) injectionPoint.getMember()).getParameterTypes().length != 1) {
            throw WeldLogger.ROOT_LOGGER.injectionPointNotAJavabean((Method) injectionPoint.getMember());
        }
        if (!ejb.lookup().equals("")) {
            if (ejb.lookup().startsWith("ejb:")) {
                return new ResourceReferenceFactory<Object>() {
                    @Override
                    public ResourceReference<Object> createResource() {
                        return new SimpleResourceReference<Object>(doLookup(ejb.lookup(), null));
                    }
                };
            }
            return handleServiceLookup(ejb.lookup(), injectionPoint);
        } else {
            final ViewDescription viewDescription = getViewDescription(ejb, injectionPoint);
            if (viewDescription != null) {
                return handleServiceLookup(viewDescription, injectionPoint);
            } else {

                final String proposedName = getEjbBindLocation(injectionPoint);
                return new ResourceReferenceFactory<Object>() {
                    @Override
                    public ResourceReference<Object> createResource() {
                        return new SimpleResourceReference<Object>(doLookup(proposedName, null));
                    }
                };
            }
        }
    }

    private ResourceReferenceFactory<Object> handleServiceLookup(ViewDescription viewDescription, InjectionPoint injectionPoint) {
        /*
         * Try to obtain ComponentView eagerly and validate the resource type
         */
        final ComponentView view = getComponentView(viewDescription);
        if (view != null && injectionPoint.getAnnotated().isAnnotationPresent(Produces.class)) {
            Class<?> clazz = view.getViewClass();

            Class<?> injectionPointRawType = Reflections.getRawType(injectionPoint.getType());
            //we just compare names, as for remote views the actual classes may be loaded from different class loaders
            Class<?> c = clazz;
            boolean found = false;
            while (c != null && c != Object.class) {
                if (injectionPointRawType.getName().equals(c.getName())) {
                    found = true;
                    break;
                }
                c = c.getSuperclass();
            }
            if (!found) {
                throw BeanLogger.LOG.invalidResourceProducerType(injectionPoint.getAnnotated(), clazz.getName());
            }
            return new ComponentViewToResourceReferenceFactoryAdapter<Object>(view);
        } else {
            return new LazyResourceReferenceFactory(viewDescription, serviceRegistry);
        }
    }

    private ComponentView getComponentView(ViewDescription viewDescription) {
        final ServiceController<?> controller = serviceRegistry.getService(viewDescription.getServiceName());
        if (controller == null) {
            return null;
        }
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
        if (injectionPoint.getAnnotated().isAnnotationPresent(Produces.class)) {
            if (viewService.isEmpty()) {
                throw WeldLogger.ROOT_LOGGER.ejbNotResolved(ejb, injectionPoint.getMember());
            } else if (viewService.size() > 1) {
                throw WeldLogger.ROOT_LOGGER.moreThanOneEjbResolved(ejb, injectionPoint.getMember(), viewService);
            }
        } else {
            if (viewService.isEmpty()) {
                return null;
            } else if (viewService.size() > 1) {
                return null;
            }
        }
        return viewService.iterator().next();
    }

    @Override
    protected BindInfo getBindInfo(String result) {
        return ContextNames.bindInfoForEnvEntry(moduleDescription.getApplicationName(), moduleDescription.getModuleName(), moduleDescription.getModuleName(), !warModule, result);
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
            throw WeldLogger.ROOT_LOGGER.couldNotDetermineUnderlyingType(type);
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

    public Object doLookup(String jndiName, String mappedName) {
        String name = ResourceInjectionUtilities.getResourceName(jndiName, mappedName);
        try {
            return new InitialContext().lookup(name);
        } catch (NamingException e) {
            throw WeldLogger.ROOT_LOGGER.couldNotFindResource(name, e);
        }
    }

    private String getEjbBindLocation(InjectionPoint injectionPoint) {
        EJB ejb = ResourceInjectionUtilities.getResourceAnnotated(injectionPoint).getAnnotation(EJB.class);
        String mappedName = ejb.mappedName();
        if (!mappedName.equals("")) {
            return mappedName;
        }
        String name = ejb.name();
        if (!name.equals("")) {
            return ResourceInjectionUtilities.RESOURCE_LOOKUP_PREFIX + "/" + name;
        }
        String propertyName;
        if (injectionPoint.getMember() instanceof Field) {
            propertyName = injectionPoint.getMember().getName();
        } else if (injectionPoint.getMember() instanceof Method) {
            propertyName = ResourceInjectionUtilities.getPropertyName((Method) injectionPoint.getMember());
            if (propertyName == null) {
                throw WeldLogger.ROOT_LOGGER.injectionPointNotAJavabean((Method) injectionPoint.getMember());
            }
        } else {
            throw WeldLogger.ROOT_LOGGER.cannotInject(injectionPoint);
        }
        String className = injectionPoint.getMember().getDeclaringClass().getName();
        return ResourceInjectionUtilities.RESOURCE_LOOKUP_PREFIX + "/" + className + "/" + propertyName;
    }

    private static class LazyResourceReferenceFactory implements ResourceReferenceFactory<Object> {

        private final ViewDescription viewDescription;

        private final ServiceRegistry serviceRegistry;

        LazyResourceReferenceFactory(ViewDescription viewDescription, ServiceRegistry serviceRegistry) {
            this.viewDescription = viewDescription;
            this.serviceRegistry = serviceRegistry;
        }

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

    }
}
