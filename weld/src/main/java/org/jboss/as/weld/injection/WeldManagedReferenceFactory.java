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

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Managed reference factory that can be used to create and inject components.
 *
 * @author Stuart Douglas
 */
public class WeldManagedReferenceFactory implements ManagedReferenceFactory {

    private final Class<?> componentClass;
    private final Value<BeanManagerImpl> beanManager;
    private final String ejbName;

    private volatile WeldEEInjection injectionTarget;
    private volatile Bean<?> bean;

    public WeldManagedReferenceFactory(Class<?> componentClass, String ejbName, final InjectedValue<BeanManagerImpl> beanManager) {
        this.componentClass = componentClass;
        this.ejbName = ejbName;
        this.beanManager = beanManager;
    }

    @Override
    public ManagedReference getReference() {
        final BeanManagerImpl beanManager = this.beanManager.getValue();
        if (injectionTarget == null) {
            synchronized (this) {
                if (injectionTarget == null) {
                    if (ejbName != null) {
                        EjbDescriptor<Object> descriptor = beanManager.getEjbDescriptor(ejbName);
                        bean = beanManager.getBean(descriptor);
                    }
                    injectionTarget = createInjectionTarget(componentClass, bean);
                }
            }
        }
        final CreationalContext<?> ctx;
        if (bean == null) {
            ctx = beanManager.createCreationalContext(null);
        } else {
            ctx = beanManager.createCreationalContext(bean);
        }
        final Object instance = injectionTarget.produce(ctx);
        return new WeldManagedReference(ctx, instance, injectionTarget);
    }

    private WeldEEInjection createInjectionTarget(Class<?> componentClass, Bean<?> bean) {
        final BeanManagerImpl beanManager = this.beanManager.getValue();
        final AnnotatedType<?> type = beanManager.getServices().get(ClassTransformer.class).loadClass(componentClass);
        List<InjectableField> injectableFields = new ArrayList<InjectableField>();
        List<InjectableMethod> injectableMethods = new ArrayList<InjectableMethod>();


        AnnotatedConstructor<?> injectConstructor = null;
        for (AnnotatedConstructor<?> constructor : type.getConstructors()) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                if (injectConstructor != null) {
                    throw new RuntimeException("Class " + componentClass + " has more that one constructor annotated with @Inject");
                }
                injectConstructor = constructor;
            }
        }
        InjectableConstructor constructor = null;
        if (injectConstructor != null) {
            constructor = new InjectableConstructor(injectConstructor, beanManager, bean);
        }

        //look for field injection points
        for (AnnotatedField<?> field : type.getFields()) {
            if (field.isAnnotationPresent(Inject.class)) {

                if (InjectionPoint.class.isAssignableFrom(field.getJavaMember().getType())) {
                    throw new RuntimeException("Component " + componentClass + " is attempting to inject the InjectionPoint into a field: " + field.getJavaMember());
                }

                final Set<Annotation> qualifiers = new HashSet<Annotation>();
                for (Annotation annotation : field.getAnnotations()) {
                    if (beanManager.isQualifier(annotation.annotationType())) {
                        qualifiers.add(annotation);
                    }
                }
                FieldInjectionPoint ip = new FieldInjectionPoint(field, qualifiers, bean);
                Set<Bean<?>> beans = beanManager.getBeans(ip);
                Bean<?> ipBean = beanManager.resolve(beans);
                injectableFields.add(new InjectableField(field.getJavaMember(), ipBean, ip));
            }
        }

        //now look for @Inject methods
        for (AnnotatedMethod<?> method : type.getMethods()) {
            if (method.isAnnotationPresent(Inject.class)) {
                final List<Bean<?>> parameterBeans = new ArrayList<Bean<?>>();
                final List<InjectionPoint> ips = new ArrayList<InjectionPoint>();
                for (AnnotatedParameter<?> param : method.getParameters()) {
                    final Set<Annotation> qualifiers = new HashSet<Annotation>();
                    for (Annotation annotation : param.getAnnotations()) {
                        if (beanManager.isQualifier(annotation.annotationType())) {
                            qualifiers.add(annotation);
                        }
                    }
                    final Class<?> parameterType = method.getJavaMember().getParameterTypes()[param.getPosition()];
                    if (InjectionPoint.class.isAssignableFrom(parameterType)) {
                        throw new RuntimeException("Component " + componentClass + " is attempting to inject the InjectionPoint into a method on a component that is not a CDI bean " + method.getJavaMember());
                    }

                    ParameterInjectionPoint ip = new ParameterInjectionPoint(param, qualifiers, bean);
                    Set<Bean<?>> beans = beanManager.getBeans(ip);
                    Bean<?> ipBean = beanManager.resolve(beans);
                    parameterBeans.add(ipBean);
                    ips.add(ip);
                }
                injectableMethods.add(new InjectableMethod(method.getJavaMember(), parameterBeans, ips));
            }
        }
        return new WeldEEInjection(componentClass, beanManager, injectableFields, injectableMethods, constructor);
    }
}
