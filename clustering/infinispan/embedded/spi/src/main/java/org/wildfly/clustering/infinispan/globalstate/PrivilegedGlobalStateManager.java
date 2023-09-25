/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
