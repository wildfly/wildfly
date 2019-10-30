/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ee.concurrent;

import org.glassfish.enterprise.concurrent.AbstractManagedThread;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;
import org.glassfish.enterprise.concurrent.spi.ContextHandle;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.manager.WildFlySecurityManager;

import javax.enterprise.concurrent.ManagedThreadFactory;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * {@link ManagedThreadFactory} implementation ensuring {@link SecurityIdentity} propagation into new threads.
 *
 * @author <a href="mailto:jkalina@redhat.com">Jan Kalina</a>
 */
public class ElytronManagedThreadFactory extends ManagedThreadFactoryImpl {

    /**
     * the factory's ACC
     */
    private final AccessControlContext accessControlContext;

    public ElytronManagedThreadFactory(String name, ContextServiceImpl contextService, int priority) {
        super(name, contextService, priority);
        this.accessControlContext = AccessController.getContext();
    }

    protected AbstractManagedThread createThread(Runnable r, final ContextHandle contextHandleForSetup) {
        if (contextHandleForSetup != null) {
            // app thread, do identity wrap
            r = SecurityIdentityUtils.doIdentityWrap(r);
        }
        // use the factory's acc as privileged, otherwise the new thread inherits current thread's acc
        final AbstractManagedThread t = AccessController.doPrivileged(new CreateThreadAction(r, contextHandleForSetup), accessControlContext);
        // reset thread classloader to prevent leaks
        if (!WildFlySecurityManager.isChecking()) {
            t.setContextClassLoader(null);
        } else {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                t.setContextClassLoader(null);
                return null;
            });
        }
        return t;
    }

    private final class CreateThreadAction implements PrivilegedAction<AbstractManagedThread> {
        private final Runnable r;
        private final ContextHandle contextHandleForSetup;

        private CreateThreadAction(Runnable r, ContextHandle contextHandleForSetup) {
            this.r = r;
            this.contextHandleForSetup = contextHandleForSetup;
        }

        @Override
        public AbstractManagedThread run() {
            return ElytronManagedThreadFactory.super.createThread(r, contextHandleForSetup);
        }
    }
}
