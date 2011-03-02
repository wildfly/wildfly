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
package org.jboss.as.weld.injection;

import org.jboss.as.ee.component.ComponentInjector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.introspector.WeldClass;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ComponentInjector that performs CDI injections
 *
 * @author Stuart Douglas
 */
public class WeldComponentInjectionService implements ComponentInjector, Service<ComponentInjector> {


    private final ServiceName serviceName;
    private final Class<?> componentClass;
    private final InjectedValue<BeanManagerImpl> beanManager = new InjectedValue<BeanManagerImpl>();
    private volatile List<CDIInjectionPoint> injectionPoints;
    private final ClassLoader classLoader;

    public WeldComponentInjectionService(ServiceName serviceName, Class<?> componentClass, ClassLoader classLoader) {
        this.serviceName = serviceName;
        this.componentClass = componentClass;
        this.classLoader = classLoader;
    }

    @Override
    public ServiceName getServiceName() {
        return serviceName;
    }

    @Override
    public WeldInjectionHandle inject(Object instance) {
        final ClassLoader oldTCCL = SecurityActions.getContextClassLoader();
        try {
            SecurityActions.setContextClassLoader(classLoader);
            final BeanManagerImpl bm = beanManager.getValue();
            WeldInjectionHandle weldHandle = new WeldInjectionHandle(injectionPoints.size());
            for (int i = 0; i < injectionPoints.size(); ++i) {
                final CDIInjectionPoint injectionPoint = injectionPoints.get(i);
                try {
                    final CreationalContext<?> ctx = bm.createCreationalContext(injectionPoint.bean);
                    final Object value = bm.getReference(injectionPoint.bean, injectionPoint.field.getGenericType(), ctx);
                    injectionPoint.field.set(instance, value);
                    weldHandle.instances[i] = value;
                    weldHandle.creationalContexts[i] = ctx;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to perform CDI injection of field: " + injectionPoint.field + " on " + componentClass, e);
                }
            }
            return weldHandle;
            } finally {
            SecurityActions.setContextClassLoader(oldTCCL);
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        final ClassLoader oldTCCL = SecurityActions.getContextClassLoader();
        try {
            SecurityActions.setContextClassLoader(classLoader);
            final BeanManagerImpl bm = beanManager.getValue();
            final ClassTransformer transformer = bm.getServices().get(ClassTransformer.class);
            final List<CDIInjectionPoint> injectionPoints = new ArrayList<CDIInjectionPoint>();
            //we do it this way to get changes introduced by extensions
            WeldClass<?> weldClass = transformer.loadClass(componentClass);
            for (AnnotatedField<?> field : weldClass.getFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    final Set<Annotation> qualifiers = new HashSet<Annotation>();
                    for (Annotation annotation : field.getAnnotations()) {
                        if (bm.isQualifier(annotation.annotationType())) {
                            qualifiers.add(annotation);
                        }
                    }
                    Set<Bean<?>> beans = bm.getBeans(field.getBaseType(), qualifiers);
                    if (beans.size() > 1) {
                        throw new StartException("Error resolving CDI injection point " + field + " on " + componentClass + ". Injection points is ambiguous " + beans);
                    } else if (beans.isEmpty()) {
                        throw new StartException("Error resolving CDI injection point " + field + " on " + componentClass + ". No bean satisfies the injection point.");
                    }
                    Bean<?> bean = bm.resolve(beans);
                    injectionPoints.add(new CDIInjectionPoint(field.getJavaMember(), bean));
                }
            }

            this.injectionPoints = injectionPoints;
        } finally {
            SecurityActions.setContextClassLoader(oldTCCL);
        }
    }

    @Override
    public void stop(StopContext context) {
        injectionPoints = null;
    }

    @Override
    public ComponentInjector getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<BeanManagerImpl> getBeanManager() {
        return beanManager;
    }

    /**
     * Injection Handle that can be used to end the lifecycle of the injected CDI beans
     */
    private class WeldInjectionHandle implements InjectionHandle {
        private final CreationalContext<?>[] creationalContexts;
        private final Object[] instances;

        public WeldInjectionHandle(int size) {
            this.creationalContexts = new CreationalContext[size];
            this.instances = new Object[size];
        }

        @Override
        public void disinject() {
            for (int i = 0; i < injectionPoints.size(); ++i) {
                ((Bean) injectionPoints.get(i).bean).destroy(instances[i], creationalContexts[i]);
            }
        }
    }

    /**
     * tracks fields to be injected
     */
    private static class CDIInjectionPoint {
        private final Field field;
        private final Bean<?> bean;

        public CDIInjectionPoint(final Field field, final Bean<?> bean) {
            this.bean = bean;
            this.field = field;
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    field.setAccessible(true);
                    return null;
                }
            });
        }
    }

}
