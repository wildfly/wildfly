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
package org.jboss.as.ee.concurrent.handle;

import org.wildfly.security.manager.WildFlySecurityManager;

import javax.enterprise.concurrent.ContextService;
import java.util.Map;

/**
 * A context handle factory which is responsible for saving and setting proper classloading context.
 *
 * @author Eduardo Martins
 */
public class ClassLoaderContextHandleFactory implements ContextHandleFactory {

    public static ClassLoaderContextHandleFactory INSTANCE = new ClassLoaderContextHandleFactory();

    private ClassLoaderContextHandleFactory() {
    }

    @Override
    public ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new ClassLoaderContextHandle();
    }

    @Override
    public Type getType() {
        return Type.CLASS_LOADER;
    }

    private static class ClassLoaderContextHandle implements ContextHandle {

        private final ClassLoader classLoader;
        private ClassLoader previous;

        private ClassLoaderContextHandle() {
            this.classLoader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        }

        @Override
        public void setup() throws IllegalStateException {
            previous = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
        }

        @Override
        public void reset() {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(previous);
        }
    }
}
