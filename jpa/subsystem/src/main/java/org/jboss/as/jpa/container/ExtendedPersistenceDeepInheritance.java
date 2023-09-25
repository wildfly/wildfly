/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import java.util.Map;

/**
 * ExtendedPersistenceInheritance.DEEP inheritance strategy where we can inherit from any bean being created or from the
 * parent bean call stack.
 *
 * @author Scott Marlow
 */
public final class ExtendedPersistenceDeepInheritance implements ExtendedPersistenceInheritanceStrategy {

    public static final ExtendedPersistenceDeepInheritance INSTANCE = new ExtendedPersistenceDeepInheritance();

    @Override
    public void registerExtendedPersistenceContext(String scopedPuName, ExtendedEntityManager entityManager) {
        if (SFSBCallStack.getSFSBCreationBeanNestingLevel() > 0) {
            SFSBCallStack.getSFSBCreationTimeInjectedXPCs(scopedPuName).registerDeepInheritance(scopedPuName, entityManager);
        }
    }

    @Override
    public ExtendedEntityManager findExtendedPersistenceContext(String puScopedName) {
        ExtendedEntityManager result;
        SFSBInjectedXPCs currentInjectedXPCs = SFSBCallStack.getSFSBCreationTimeInjectedXPCs(puScopedName);
        // will look directly at the top level bean being created (registerExtendedPersistenceContext() registers xpc there).
        result = currentInjectedXPCs.findExtendedPersistenceContextDeepInheritance(puScopedName);

        if (result == null) {
            // walk up the BEAN call stack (this also covers the case of a bean method JNDI searching for another bean)
            for (Map<String, ExtendedEntityManager> handle : SFSBCallStack.currentSFSBCallStack()) {
                result = handle.get(puScopedName);
                if(result != null) {
                    return result;
                }
            }
        }

        return result;

    }
}
