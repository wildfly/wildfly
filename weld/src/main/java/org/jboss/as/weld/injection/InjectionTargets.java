/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.weld.injection;

import javax.enterprise.inject.spi.Bean;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.manager.api.WeldInjectionTarget;
import org.jboss.weld.manager.api.WeldInjectionTargetBuilder;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.util.Beans;

/**
 * Utility class for working with CDI InjectionTargets
 *
 * @author Jozef Hartinger
 *
 */
public class InjectionTargets {

    private InjectionTargets() {
    }

    /**
    * Creates a new InjectionTarget for a given class. If the interceptionSupport flag is set to true the resulting instance will support
    * interception (support provided by Weld). If an InjectionTarget is created for a component where interception support is implemented
    * through component's view (EJBs, managed beans) the flag must be set to false.
    *
    * @param componentClass
    * @param bean
    * @param beanManager
    * @param interceptionSupport
    * @return
    */
    public static <T> WeldInjectionTarget<T> createInjectionTarget(Class<?> componentClass, Bean<T> bean, BeanManagerImpl beanManager,
            boolean interceptionSupport) {
        final ClassTransformer transformer = beanManager.getServices().get(ClassTransformer.class);
        @SuppressWarnings("unchecked")
        final Class<T> clazz = (Class<T>) componentClass;
        EnhancedAnnotatedType<T> type = transformer.getEnhancedAnnotatedType(clazz, beanManager.getId());

        if (!type.getJavaClass().equals(componentClass)) {
            /*
             * Jasper loads a class with multiple classloaders which is not supported by Weld.
             * If this happens, use a combination of a bean archive identifier and class' classloader hashCode as the BDA ID.
             * This breaks AnnotatedType serialization but that does not matter as these are non-contextual components.
             */
            final String bdaId = beanManager.getId() + componentClass.getClassLoader().hashCode();
            type = transformer.getEnhancedAnnotatedType(clazz, bdaId);
        }

        if (Beans.getBeanConstructor(type) == null) {
            /*
             * For example, AsyncListeners may be CDI-incompatible as long as the application never calls javax.servletAsyncContext#createListener(Class)
             * and only instantiates the listener itself.
             */
            return beanManager.getInjectionTargetFactory(type).createNonProducibleInjectionTarget();
        }
        WeldInjectionTargetBuilder<T> builder = beanManager.createInjectionTargetBuilder(type);
        builder.setResourceInjectionEnabled(false); // because these are all EE components where resource injection is not handled by Weld
        if (interceptionSupport) {
            return builder.build();
        } else {
            // suppress interception/decoration because this is a component for which WF provides interception support
            return builder.setInterceptionEnabled(false).setTargetClassLifecycleCallbacksEnabled(false).setDecorationEnabled(false).build();
        }
    }
}
