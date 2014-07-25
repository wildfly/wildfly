/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
