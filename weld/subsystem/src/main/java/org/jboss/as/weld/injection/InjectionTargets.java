/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.injection;

import static java.security.AccessController.doPrivileged;

import jakarta.enterprise.inject.spi.Bean;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.manager.api.WeldInjectionTarget;
import org.jboss.weld.manager.api.WeldInjectionTargetBuilder;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.util.Beans;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.security.manager.action.GetClassLoaderAction;

/**
 * Utility class for working with Jakarta Contexts and Dependency Injection InjectionTargets
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
    * through component's view (Jakarta Enterprise Beans, managed beans) the flag must be set to false.
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
            final ClassLoader classLoader = WildFlySecurityManager.isChecking() ? doPrivileged(new GetClassLoaderAction(componentClass)) : componentClass.getClassLoader();
            final String bdaId = beanManager.getId() + classLoader.hashCode();
            type = transformer.getEnhancedAnnotatedType(clazz, bdaId);
        }

        if (Beans.getBeanConstructor(type) == null) {
            /*
             * For example, AsyncListeners may be Jakarta Contexts and Dependency Injection incompatible as long as the application never calls javax.servletAsyncContext#createListener(Class)
             * and only instantiates the listener itself.
             */
            return beanManager.getInjectionTargetFactory(type).createNonProducibleInjectionTarget();
        }
        WeldInjectionTargetBuilder<T> builder = beanManager.createInjectionTargetBuilder(type);
        builder.setBean(bean);
        builder.setResourceInjectionEnabled(false); // because these are all EE components where resource injection is not handled by Weld
        if (interceptionSupport) {
            return builder.build();
        } else {
            // suppress interception/decoration because this is a component for which WF provides interception support
            return builder.setInterceptionEnabled(false).setTargetClassLifecycleCallbacksEnabled(false).setDecorationEnabled(false).build();
        }
    }
}
