/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import org.jboss.threads.JBossThreadFactory;
import org.wildfly.security.ParametricPrivilegedAction;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Default {@link ThreadFactory} implementation that applies a specific context {@link ClassLoader}.
 * @author Paul Ferraro
 */
public class DefaultThreadFactory extends ContextualThreadFactory<ClassLoader> {

    private enum ThreadPoolFactory implements ParametricPrivilegedAction<ThreadFactory, Supplier<ThreadGroup>> {
        INSTANCE;

        @Override
        public ThreadFactory run(Supplier<ThreadGroup> group) {
            return new JBossThreadFactory(group.get(), Boolean.FALSE, null, "%G - %t", null, null);
        }
    }

    public DefaultThreadFactory(Class<?> targetClass) {
        this(targetClass, new Supplier<>() {
            @Override
            public ThreadGroup get() {
                return new ThreadGroup(targetClass.getSimpleName());
            }
        });
    }

    DefaultThreadFactory(Class<?> targetClass, Supplier<ThreadGroup> group) {
        this(WildFlySecurityManager.doUnchecked(group, ThreadPoolFactory.INSTANCE), targetClass);
    }

    public DefaultThreadFactory(ThreadFactory factory) {
        this(factory, factory.getClass());
    }

    private DefaultThreadFactory(ThreadFactory factory, Class<?> targetClass) {
        super(factory, WildFlySecurityManager.getClassLoaderPrivileged(targetClass), ContextClassLoaderReference.INSTANCE);
    }
}
