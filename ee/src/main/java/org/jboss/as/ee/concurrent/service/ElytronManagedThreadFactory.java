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

package org.jboss.as.ee.concurrent.service;

import org.glassfish.enterprise.concurrent.AbstractManagedThread;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;
import org.glassfish.enterprise.concurrent.spi.ContextHandle;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.manager.WildFlySecurityManager;

import javax.enterprise.concurrent.ManagedThreadFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * {@link ManagedThreadFactory} implementation ensuring {@link SecurityIdentity} propagation into new threads.
 *
 * @author <a href="mailto:jkalina@redhat.com">Jan Kalina</a>
 */
public class ElytronManagedThreadFactory extends ManagedThreadFactoryImpl {

    public ElytronManagedThreadFactory(String name, ContextServiceImpl contextService, int priority) {
        super(name, contextService, priority);
    }

    protected AbstractManagedThread createThread(final Runnable r, final ContextHandle contextHandleForSetup) {
        boolean checking = WildFlySecurityManager.isChecking();
        SecurityDomain domain = checking ?
                AccessController.doPrivileged((PrivilegedAction<SecurityDomain>) SecurityDomain::getCurrent) :
                SecurityDomain.getCurrent();
        SecurityIdentity identity = domain == null ? null : domain.getCurrentSecurityIdentity();

        if (checking) {
            return AccessController.doPrivileged((PrivilegedAction<ElytronManagedThread>)
                    () -> new ElytronManagedThread(r, contextHandleForSetup, identity)
            );
        } else {
            return new ElytronManagedThread(r, contextHandleForSetup, identity);
        }
    }

    public class ElytronManagedThread extends ManagedThread {

        final SecurityIdentity securityIdentity;

        ElytronManagedThread(Runnable target, ContextHandle contextHandleForSetup, SecurityIdentity securityIdentity) {
            super(target, contextHandleForSetup);
            this.securityIdentity = securityIdentity;
        }

        @Override
        public void run() {
            if (securityIdentity != null) {
                securityIdentity.runAs(super::run);
            } else {
                super.run();
            }
        }

    }

}
