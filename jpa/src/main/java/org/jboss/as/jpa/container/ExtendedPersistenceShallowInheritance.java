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
