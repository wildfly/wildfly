/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.container.interceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptors;
import org.jboss.as.ee.container.injection.ResourceInjectionConfiguration;
import static org.jboss.as.ee.container.Util.getSingleAnnotatedMethod;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 * Configuration for an interceptor bound to a managed bean class.
 *
 * @author John E. Bailey
 */
public class MethodInterceptorConfiguration {
    private static final DotName INTERCEPTORS_ANNOTATION_NAME = DotName.createSimple(Interceptors.class.getName());

    private final Class<?> interceptorClass;
    private final Method aroundInvokeMethod;
    private final MethodInterceptorFilter methodFilter;
    private final List<ResourceInjectionConfiguration> resourceConfigurations;

    /**
     * Create an instance with the interceptor class and the resource configurations.
     *
     * @param interceptorClass       The interceptor class type
     * @param aroundInvokeMethod     The around invoke method
     * @param methodFilter           The method filter
     * @param resourceConfigurations The resource injection configurations
     */
    public MethodInterceptorConfiguration(final Class<?> interceptorClass, final Method aroundInvokeMethod, final MethodInterceptorFilter methodFilter, final List<ResourceInjectionConfiguration> resourceConfigurations) {
        this.interceptorClass = interceptorClass;
        this.aroundInvokeMethod = aroundInvokeMethod;
        this.methodFilter = methodFilter;
        this.resourceConfigurations = resourceConfigurations;
    }

    /**
     * Get the interceptor class.
     *
     * @return The interceptor class
     */
    public Class<?> getInterceptorClass() {
        return interceptorClass;
    }

    /**
     * Get the resource configurations.
     *
     * @return The resource configurations
     */
    public List<ResourceInjectionConfiguration> getResourceConfigurations() {
        return resourceConfigurations;
    }

    /**
     * Get the around invoke method.
     *
     * @return The around invoke method
     */
    public Method getAroundInvokeMethod() {
        return aroundInvokeMethod;
    }

    public MethodInterceptorFilter getMethodFilter() {
        return methodFilter;
    }

    public static final List<MethodInterceptorConfiguration> from(final ClassInfo classInfo, final Index index, final Class<?> beanClass, final ClassLoader beanClassLoader) {
        final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();
        if (classAnnotations == null) {
            return Collections.emptyList();
        }

        final List<AnnotationInstance> interceptorAnnotations = classAnnotations.get(INTERCEPTORS_ANNOTATION_NAME);
        if (interceptorAnnotations == null || interceptorAnnotations.isEmpty()) {
            return Collections.emptyList();
        }
        final List<MethodInterceptorConfiguration> interceptorConfigurations = new ArrayList<MethodInterceptorConfiguration>(interceptorAnnotations.size());

        final Interceptors interceptorsAnnotation = beanClass.getAnnotation(Interceptors.class);
        final Class<?>[] interceptorTypes = interceptorsAnnotation.value();
        for (AnnotationInstance annotationInstance : interceptorAnnotations) {

            final Class<?> interceptorType;
            try {
                interceptorType = beanClassLoader.loadClass(annotationInstance.name().toString());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Failed to interceptor class " + annotationInstance.name(), e);
            }

            final ClassInfo interceptorClassInfo = index.getClassByName(DotName.createSimple(interceptorType.getName()));
            if (interceptorClassInfo == null) {
                continue; // TODO: Process without index info
            }

            final AnnotationTarget target = annotationInstance.target();
            final MethodInterceptorFilter methodFilter;
            if (target instanceof MethodInfo) {
                final MethodInfo methodInfo = MethodInfo.class.cast(target);
                    final List<String> argTypes = new ArrayList<String>(methodInfo.args().length);
                    for (Type argType : methodInfo.args()) {
                        argTypes.add(argType.name().toString());
                    }
                    methodFilter = new MatchMethodInterceptorFilter(methodInfo.name(), argTypes.toArray(new String[argTypes.size()]));
            } else {
                methodFilter = AllMethodInterceptorFilter.INSTANCE;
            }

            final Method aroundInvokeMethod = getSingleAnnotatedMethod(interceptorType, interceptorClassInfo, AroundInvoke.class, true);
            final List<ResourceInjectionConfiguration> resourceConfigurations = ResourceInjectionConfiguration.from(interceptorClassInfo, interceptorType, beanClassLoader);
            interceptorConfigurations.add(new MethodInterceptorConfiguration(interceptorType, aroundInvokeMethod, methodFilter, resourceConfigurations));
        }

        //Look for any @AroundInvoke methods on bean class
//        if (classInfo != null) {
//            final Method aroundInvokeMethod = getSingleAnnotatedMethod(beanClass, classInfo, AroundInvoke.class, true);
//            if (aroundInvokeMethod != null) {
//                final List<ResourceInjectionConfiguration> resources = processClassResources(beanClass);
//                interceptorConfigurations.add(new InterceptorConfiguration(beanClass, aroundInvokeMethod, resources));
//            }
//        }

        return interceptorConfigurations;
    }
}
