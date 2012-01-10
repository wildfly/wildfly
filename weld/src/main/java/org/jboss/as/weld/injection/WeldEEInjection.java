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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;

import org.jboss.as.weld.WeldMessages;
import org.jboss.weld.bean.AbstractClassBean;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;

/**
 * Class that knows how to create and inject a class that requires CDI injection
 *
 * @author Stuart Douglas
 */
class WeldEEInjection {

    private final Class<?> componentClass;
    private final BeanManagerImpl beanManager;
    private final List<InjectableField> injectableFields;
    private final List<InjectableMethod> injectableMethods;
    private final InjectableConstructor constructor;
    private final InjectionTarget injectionTarget;

    public WeldEEInjection(Class<?> componentClass, BeanManagerImpl beanManager, List<InjectableField> injectableFields, List<InjectableMethod> injectableMethods, InjectableConstructor constructor, final InjectionTarget injectionTarget) {
        this.componentClass = componentClass;
        this.beanManager = beanManager;
        this.injectableFields = injectableFields;
        this.injectableMethods = injectableMethods;
        this.constructor = constructor;
        this.injectionTarget = injectionTarget;
    }

    /**
     * Run field and method injections. Resource injections should be performed before this method is called
     *
     * @param instance The instance to inject
     * @param ctx      The creational context that was used to create the instance
     */
    public void inject(Object instance, CreationalContext<?> ctx) {
        if (injectionTarget != null) {
            injectionTarget.inject(instance, ctx);
        }
        for (InjectableField field : injectableFields) {
            field.inject(instance, beanManager, ctx);
        }
        for (InjectableMethod method : injectableMethods) {
            method.inject(instance, beanManager, ctx);
        }
    }

    /**
     * Create an instance of the class by calling the bean constructor
     *
     * @param ctx The creational context to use
     * @return A new instance of the object
     */
    public Object produce(CreationalContext<?> ctx) {

        try {
            if (constructor != null) {
                return constructor.createInstance(ctx);
            } else {
                return componentClass.newInstance();
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }


    public static WeldEEInjection createWeldEEInjection(Class<?> componentClass, Bean<?> bean, final BeanManagerImpl beanManager) {
        final AnnotatedType<?> type = beanManager.getServices().get(ClassTransformer.class).loadClass(componentClass);
        List<InjectableField> injectableFields = new ArrayList<InjectableField>();
        List<InjectableMethod> injectableMethods = new ArrayList<InjectableMethod>();


        AnnotatedConstructor<?> injectConstructor = null;
        for (AnnotatedConstructor<?> constructor : type.getConstructors()) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                if (injectConstructor != null) {
                    throw WeldMessages.MESSAGES.moreThanOneBeanConstructor(componentClass);
                }
                injectConstructor = constructor;
            }
        }
        InjectableConstructor constructor = null;
        if (injectConstructor != null) {
            constructor = new InjectableConstructor(injectConstructor, beanManager, bean);
        }

        final InjectionTarget injectionTarget;
        if (bean instanceof AbstractClassBean) {
            //if we have the bean object we just use the CDI injectionTarget

            injectionTarget = ((AbstractClassBean) bean).getInjectionTarget();
        } else {
            injectionTarget = null;
            //if there is no bean we need to create all the injectable fields / methods ourselves
            //we do not use createInjectionTarget as that will result in EE injections being performed
            //twice

            //look for field injection points
            for (AnnotatedField<?> field : type.getFields()) {
                if (field.isAnnotationPresent(Inject.class)) {

                    if (InjectionPoint.class.isAssignableFrom(field.getJavaMember().getType())) {
                        throw WeldMessages.MESSAGES.attemptingToInjectInjectionPointIntoField(componentClass, field.getJavaMember());
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
                    if (ipBean == null) {
                        throw WeldMessages.MESSAGES.couldNotResolveInjectionPoint(field.getJavaMember().toGenericString(), qualifiers);
                    }
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
                            throw WeldMessages.MESSAGES.attemptingToInjectInjectionPointIntoNonBean(componentClass, method.getJavaMember());
                        }

                        ParameterInjectionPoint ip = new ParameterInjectionPoint(param, qualifiers, bean);
                        Set<Bean<?>> beans = beanManager.getBeans(ip);
                        Bean<?> ipBean = beanManager.resolve(beans);
                        if (ipBean == null) {
                            throw WeldMessages.MESSAGES.couldNotResolveInjectionPoint(param.toString(), qualifiers);
                        }
                        parameterBeans.add(ipBean);
                        ips.add(ip);
                    }
                    injectableMethods.add(new InjectableMethod(method.getJavaMember(), parameterBeans, ips));
                }
            }
        }
        return new WeldEEInjection(componentClass, beanManager, injectableFields, injectableMethods, constructor, injectionTarget);
    }
}
