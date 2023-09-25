/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import java.util.Map;

/**
 *
 * @author Scott Marlow
 */
public final class ExtendedPersistenceShallowInheritance implements ExtendedPersistenceInheritanceStrategy {
    public static final ExtendedPersistenceShallowInheritance INSTANCE = new ExtendedPersistenceShallowInheritance();

    @Override
    public void registerExtendedPersistenceContext(String scopedPuName, ExtendedEntityManager entityManager) {
        if (SFSBCallStack.getSFSBCreationBeanNestingLevel() > 0) {
            SFSBCallStack.getSFSBCreationTimeInjectedXPCs(scopedPuName).registerShallowInheritance(scopedPuName, entityManager);
        }
    }


    @Override
    public ExtendedEntityManager findExtendedPersistenceContext(String puScopedName) {
        ExtendedEntityManager result = null;
        // if current bean is injected from a parent bean that is also being created, current bean
        // can inherit only from the parent bean.
        if (SFSBCallStack.getSFSBCreationBeanNestingLevel() > 1) {
            SFSBInjectedXPCs currentInjectedXPCs = SFSBCallStack.getSFSBCreationTimeInjectedXPCs(puScopedName);
            result = currentInjectedXPCs.findExtendedPersistenceContextShallowInheritance(puScopedName);
        } else {
            // else inherit from parent bean that created current bean (if any).  The parent bean is the one
            // that did a JNDI lookup of the current bean.
            Map<String, ExtendedEntityManager> handle = SFSBCallStack.getCurrentCall();
            if (handle != null) {
                result = handle.get(puScopedName);
                if (result != null) {
                    return result;
                }
            }
        }
        return result;

    }
}
