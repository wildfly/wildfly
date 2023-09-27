/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.globalstate;

import org.infinispan.factories.AbstractComponentFactory;
import org.infinispan.factories.AutoInstantiableFactory;
import org.infinispan.factories.annotations.DefaultFactoryFor;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.globalstate.GlobalStateManager;
import org.infinispan.globalstate.impl.GlobalStateManagerImpl;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Workaround for ISPN-14051.
 * @author Paul Ferraro
 */
@DefaultFactoryFor(classes = GlobalStateManager.class)
@Scope(Scopes.GLOBAL)
public class GlobalStateManagerFactory extends AbstractComponentFactory implements AutoInstantiableFactory {
    @Override
    public Object construct(String componentName) {
        return WildFlySecurityManager.isChecking() ? new PrivilegedGlobalStateManager() : new GlobalStateManagerImpl();
    }
}
