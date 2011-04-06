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
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    private volatile List<Injection> injectionPoints;
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
            WeldInjectionHandle weldHandle = new WeldInjectionHandle();
            for (Injection injectionPoint : injectionPoints) {
                weldHandle.addAll(injectionPoint.inject(instance,bm));
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
            final List<Injection> injectionPoints = new ArrayList<Injection>();
            //we do it this way to get changes introduced by extensions
            WeldClass<?> weldClass = transformer.loadClass(componentClass);
            for (AnnotatedField<?> field : weldClass.getFields()) {
                if (field.isAnnotationPresent(Inject.class)) {

                    if(InjectionPoint.class.isAssignableFrom(field.getJavaMember().getType())) {
                        throw new StartException("Component " + componentClass + " is attempting to inject the InjectionPoint into a field: " + field.getJavaMember());
                    }

                    final Set<Annotation> qualifiers = new HashSet<Annotation>();
                    for (Annotation annotation : field.getAnnotations()) {
                        if (bm.isQualifier(annotation.annotationType())) {
                            qualifiers.add(annotation);
                        }
                    }
                    FieldInjectionPoint ip = new FieldInjectionPoint(field, qualifiers);
                    Set<Bean<?>> beans = bm.getBeans(ip);
                    if (beans.size() > 1) {
                        throw new StartException("Error resolving CDI injection point " + field + " on " + componentClass + ". Injection points is ambiguous " + beans);
                    } else if (beans.isEmpty()) {
                        throw new StartException("Error resolving CDI injection point " + field + " on " + componentClass + ". No bean satisfies the injection point.");
                    }
                    Bean<?> bean = bm.resolve(beans);
                    injectionPoints.add(new CDIFieldInjection(field.getJavaMember(), bean, ip));
                }
            }
            //now look for @Inject methods
            for (AnnotatedMethod<?> method : weldClass.getMethods()) {
                if (method.isAnnotationPresent(Inject.class)) {
                    final List<Bean<?>> parameterBeans = new ArrayList<Bean<?>>();
                    final List<InjectionPoint> ips = new ArrayList<InjectionPoint>();
                    for (AnnotatedParameter<?> param : method.getParameters()) {
                        final Set<Annotation> qualifiers = new HashSet<Annotation>();
                        for (Annotation annotation : param.getAnnotations()) {
                            if (bm.isQualifier(annotation.annotationType())) {
                                qualifiers.add(annotation);
                            }
                        }
                        final Class<?> parameterType = method.getJavaMember().getParameterTypes()[param.getPosition()];
                        if(InjectionPoint.class.isAssignableFrom(parameterType)) {
                            throw new StartException("Component " + componentClass + " is attempting to inject the InjectionPoint into a method on a component that is not a CDI bean " + method.getJavaMember());
                        }

                        ParameterInjectionPoint ip = new ParameterInjectionPoint(param, qualifiers);
                        Set<Bean<?>> beans = bm.getBeans(ip);
                        if (beans.size() > 1) {
                            throw new StartException("Error resolving CDI injection point " + param + " on " + componentClass + ". Injection points is ambiguous " + beans);
                        } else if (beans.isEmpty()) {
                            throw new StartException("Error resolving CDI injection point " + param + " on " + componentClass + ". No bean satisfies the injection point.");
                        }
                        Bean<?> bean = bm.resolve(beans);
                        parameterBeans.add(bean);
                        ips.add(ip);
                    }
                    injectionPoints.add(new CDIMethodInjection(method.getJavaMember(), parameterBeans, ips));
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
    private static final class WeldInjectionHandle implements InjectionHandle {
        private final List<CreationalContext<?>> creationalContexts = new ArrayList<CreationalContext<?>>();

        public void add(CreationalContext<?> ctx) {
            creationalContexts.add(ctx);
        }

        public void addAll(Collection<CreationalContext<?>> ctxs) {
            creationalContexts.addAll(ctxs);
        }

        @Override
        public void uninject() {
            for(CreationalContext<?> ctx : creationalContexts) {
                ctx.release();
            }
        }
    }


    private static interface Injection {
        Collection<CreationalContext<?>> inject(Object instance, BeanManager beanManager);
    }

    /**
     * tracks fields to be injected
     */
    private static final class CDIFieldInjection implements Injection {
        private final Field field;
        private final Bean<?> bean;
        private final FieldInjectionPoint injectionPoint;

        public CDIFieldInjection(final Field field, final Bean<?> bean, final FieldInjectionPoint injectionPoint) {
            this.bean = bean;
            this.field = field;
            this.injectionPoint = injectionPoint;
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    field.setAccessible(true);
                    return null;
                }
            });
        }

        /**
         * Injects into a field injection point
         * @param instance The instance to inject
         * @param beanManager The current BeanManager
         * @return A collections of CreationalContexts that can be used to release the injected resources
         */
        @Override
        public Collection<CreationalContext<?>> inject(Object instance, BeanManager beanManager) {
            try {
                final CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
                final Object value = beanManager.getInjectableReference(injectionPoint, ctx);
                field.set(instance, value);
                return Collections.<CreationalContext<?>>singleton(ctx);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to perform CDI injection of field: " + field + " on " + instance.getClass(), e);
            }
        }
    }

    /**
     * tracks initalizer methods
     */
    private static final class CDIMethodInjection implements Injection {
        private final Method method;
        private final List<Bean<?>> beans;
        private final List<InjectionPoint> injectionPoints;

        public CDIMethodInjection(final Method method, final List<Bean<?>> beans, final List<InjectionPoint> injectionPoints) {
            this.beans = beans;
            this.method = method;
            this.injectionPoints = injectionPoints;
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    method.setAccessible(true);
                    return null;
                }
            });
        }


        /**
         * Invokes an Inject annotated method
         * @param instance The instance to invoke on
         * @param beanManager The current BeanManager
         * @return A collections of CreationalContexts that can be used to release the injected resources
         */
        @Override
        public Collection<CreationalContext<?>> inject(Object instance, BeanManager beanManager) {
            try {
                final Object[] params = new Object[beans.size()];
                final Set<CreationalContext<?>> contexts = new HashSet<CreationalContext<?>>();
                int i = 0;
                for(Bean<?> bean : beans) {
                    final CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
                    contexts.add(ctx);
                    final Object value = beanManager.getInjectableReference(injectionPoints.get(i), ctx);
                    params[i++] = value;
                }
                method.invoke(instance,params);
                return contexts;
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to perform CDI injection of initalizer method: " + method + " on " + instance.getClass(), e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Failed to perform CDI injection of field: " + method + " on " + instance.getClass(), e);
            }
        }
    }

    /**
     * InjectionPoint implementation for a field
     */
    private static final class FieldInjectionPoint implements InjectionPoint {

        private final AnnotatedField<?> field;
        private final Set<Annotation> qualifiers;

        public FieldInjectionPoint(AnnotatedField<?> field, Set<Annotation> qualifiers) {
            this.field = field;
            this.qualifiers = qualifiers;
        }

        @Override
        public Type getType() {
            return field.getJavaMember().getGenericType();

        }

        @Override
        public Set<Annotation> getQualifiers() {
            return qualifiers;
        }

        @Override
        public Bean<?> getBean() {
            return null;
        }

        @Override
        public Member getMember() {
            return field.getJavaMember();
        }

        @Override
        public Annotated getAnnotated() {
            return field;
        }

        @Override
        public boolean isDelegate() {
            return false;
        }

        @Override
        public boolean isTransient() {
            return Modifier.isTransient(field.getJavaMember().getModifiers());
        }
    }

    /**
     * InjectionPoint implementation for method parameters
     */
    private static final class ParameterInjectionPoint implements InjectionPoint {

        private final AnnotatedParameter<?> parameter;
        private final Set<Annotation> qualifiers;

        public ParameterInjectionPoint(AnnotatedParameter<?> parameter, Set<Annotation> qualifiers) {
            this.parameter = parameter;
            this.qualifiers = qualifiers;
        }

        @Override
        public Type getType() {
            return parameter.getBaseType();

        }

        @Override
        public Set<Annotation> getQualifiers() {
            return qualifiers;
        }

        @Override
        public Bean<?> getBean() {
            return null;
        }

        @Override
        public Member getMember() {
            return parameter.getDeclaringCallable().getJavaMember();
        }

        @Override
        public Annotated getAnnotated() {
            return parameter;
        }

        @Override
        public boolean isDelegate() {
            return false;
        }

        @Override
        public boolean isTransient() {
            return false;
        }
    }

}
