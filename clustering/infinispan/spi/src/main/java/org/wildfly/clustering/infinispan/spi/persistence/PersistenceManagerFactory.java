/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi.persistence;

import java.security.PrivilegedAction;

import org.infinispan.factories.AbstractNamedCacheComponentFactory;
import org.infinispan.factories.AutoInstantiableFactory;
import org.infinispan.factories.annotations.DefaultFactoryFor;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.persistence.manager.PassivationPersistenceManager;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.manager.PersistenceManagerImpl;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Workaround for ISPN-11121 that disables permission checking during {@link PersistenceManager#start()}.
 * @author Paul Ferraro
 */
@DefaultFactoryFor(classes = PersistenceManager.class)
@Scope(Scopes.NAMED_CACHE)
public class PersistenceManagerFactory extends AbstractNamedCacheComponentFactory implements AutoInstantiableFactory {

    @Override
    public Object construct(String name) {
        PersistenceManager manager = new PrivilegedStartPersistenceManager();
        return this.configuration.persistence().passivation() ? new PassivationPersistenceManager(manager) : manager;
    }

    @Scope(Scopes.NAMED_CACHE)
    static class PrivilegedStartPersistenceManager extends PersistenceManagerImpl implements PrivilegedAction<Void> {

        @Override
        public void start() {
            WildFlySecurityManager.doUnchecked(this);
        }

        @Override
        public Void run() {
            super.start();
            return null;
        }
    }
}
