/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.beanvalidation.testutil;

import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;

import org.jboss.as.ee.beanvalidation.testutil.WithContextClassLoader.NullClassLoader;
import org.wildfly.security.manager.action.GetContextClassLoaderAction;
import org.wildfly.security.manager.action.SetContextClassLoaderAction;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * A JUnit rule which allows to use a specific class loader as context class loader by annotating the concerned test method with
 * {@link WithContextClassLoader}. The previous context class loader will be set after test execution.
 *
 * @author Gunnar Morling
 */
public class ContextClassLoaderRule extends TestWatcher {
    private ClassLoader previousContextClassLoader;

    @Override
    protected void starting(Description description) {
        final WithContextClassLoader validationXml = description.getAnnotation(WithContextClassLoader.class);
        if (validationXml == null) {
            return;
        }

        previousContextClassLoader = getContextClassLoader();

        setContextClassLoader(validationXml.value() == NullClassLoader.class ? null : newClassLoaderInstance(
                validationXml.value(), previousContextClassLoader));
    }

    @Override
    protected void finished(Description description) {
        if (previousContextClassLoader != null) {
            setContextClassLoader(previousContextClassLoader);
        }
    }

    private ClassLoader getContextClassLoader() {
        return run(GetContextClassLoaderAction.getInstance());
    }

    private void setContextClassLoader(ClassLoader classLoader) {
        run(new SetContextClassLoaderAction(classLoader));
    }

    private <T extends ClassLoader> T newClassLoaderInstance(Class<T> clazz, ClassLoader parent) {
        return run(new NewClassLoaderInstanceAction<T>(clazz, parent));
    }

    private <T> T run(PrivilegedAction<T> action) {
        return ! WildFlySecurityManager.isChecking() ? action.run() : doPrivileged(action);
    }

    private static final class NewClassLoaderInstanceAction<T extends ClassLoader> implements PrivilegedAction<T> {

        private final Class<T> clazz;
        private final ClassLoader parent;

        private NewClassLoaderInstanceAction(Class<T> clazz, ClassLoader parent) {
            this.clazz = clazz;
            this.parent = parent;
        }

        @Override
        public T run() {
            try {
                return clazz.getConstructor(ClassLoader.class).newInstance(parent);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
