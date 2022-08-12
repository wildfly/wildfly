/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.globalstate;

import java.security.PrivilegedAction;
import java.util.Optional;

import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.globalstate.ScopedPersistentState;
import org.infinispan.globalstate.impl.GlobalStateManagerImpl;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Workaround for ISPN-14051.
 * @author Paul Ferraro
 */
@Scope(Scopes.GLOBAL)
@SuppressWarnings("synthetic-access")
public class PrivilegedGlobalStateManager extends GlobalStateManagerImpl {
    @Override
    public void start() {
        WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
            @Override
            public Void run() {
                PrivilegedGlobalStateManager.super.start();
                return null;
            }
        });
    }

    @Override
    public void stop() {
        WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
            @Override
            public Void run() {
                PrivilegedGlobalStateManager.super.stop();
                return null;
            }
        });
    }

    @Override
    public void writeGlobalState() {
        WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
            @Override
            public Void run() {
                PrivilegedGlobalStateManager.super.writeGlobalState();
                return null;
            }
        });
    }

    @Override
    public void writeScopedState(ScopedPersistentState state) {
        WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
            @Override
            public Void run() {
                PrivilegedGlobalStateManager.super.writeScopedState(state);
                return null;
            }
        });
    }

    @Override
    public void deleteScopedState(String scope) {
        WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
            @Override
            public Void run() {
                PrivilegedGlobalStateManager.super.deleteScopedState(scope);
                return null;
            }
        });
    }

    @Override
    public Optional<ScopedPersistentState> readScopedState(String scope) {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
            @Override
            public Optional<ScopedPersistentState> run() {
                return PrivilegedGlobalStateManager.super.readScopedState(scope);
            }
        });
    }
}
