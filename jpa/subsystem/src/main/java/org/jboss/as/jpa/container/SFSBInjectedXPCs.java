/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks extended persistence context that have been injected into a bean.
 * Supports two different inheritance models, in an efficient manner.
 *
 * The reason why both strategies are handled by one class, is to make it easier to use both
 * (DEEP + SHALLOW) in the same application as recommended to the Jakarta Persistence EG
 * see http://java.net/projects/jpa-spec/lists/jsr338-experts/archive/2012-06/message/13 and the rest of the thread.
 *
 * Deep (JBoss legacy) mode will inherit across sibling beans and travel several levels up the beans in the
 * current Jakarta Enterprise Beans container instance.
 *
 * Shallow (EE standard) mode will inherit only from the immediate parent bean (in the current Jakarta Enterprise Beans container instance).
 *
 * @author Scott Marlow
 */
class SFSBInjectedXPCs {

    private Map<String, ExtendedEntityManager> injectedXPCsByPuName;
    private SFSBInjectedXPCs parent;    // parent or null if at top level
    private SFSBInjectedXPCs toplevel;  // null if already at top level, otherwise references the toplevel

    SFSBInjectedXPCs(SFSBInjectedXPCs parent, SFSBInjectedXPCs toplevel) {
        this.parent = parent;
        this.toplevel = toplevel;
    }

    SFSBInjectedXPCs getParent() {
        return parent;
    }

    /**
     * Note that InjectedXPCs is about creation time of a stateful bean.
     * If deep inheritance is enabled, return the top most bean's XPCs
     * For shallow inheritance, return the current bean being created XPCs
     */
    SFSBInjectedXPCs getTopLevel() {
        return toplevel != null ? toplevel : this;
    }

    void registerDeepInheritance(String scopedPuName, ExtendedEntityManager entityManager) {
        SFSBInjectedXPCs target = this;
        if (toplevel != null) {  // all XPCs are registered at the top level bean
            target = toplevel;
        }

        if (target.injectedXPCsByPuName == null) {
            target.injectedXPCsByPuName = new HashMap<String, ExtendedEntityManager>();
        }
        target.injectedXPCsByPuName.put(scopedPuName, entityManager);

    }

    void registerShallowInheritance(String scopedPuName, ExtendedEntityManager entityManager) {
        SFSBInjectedXPCs target = this;

        if (target.injectedXPCsByPuName == null) {
            target.injectedXPCsByPuName = new HashMap<String, ExtendedEntityManager>();
        }
        target.injectedXPCsByPuName.put(scopedPuName, entityManager);

    }

    ExtendedEntityManager findExtendedPersistenceContextDeepInheritance(String puScopedName) {
        SFSBInjectedXPCs target = this;

        if (toplevel != null) {  // all XPCs are registered at the top level bean
            target = toplevel;
        }
        return  target.injectedXPCsByPuName != null ?
                target.injectedXPCsByPuName.get(puScopedName) :
                null;
    }

    ExtendedEntityManager findExtendedPersistenceContextShallowInheritance(String puScopedName) {
        SFSBInjectedXPCs target = this;

        return  target.injectedXPCsByPuName != null ?
                target.injectedXPCsByPuName.get(puScopedName) :
                null;
    }
}
