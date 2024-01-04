/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

/**
 *  for extended persistence inheritance strategies.
 *
 * @author Scott Marlow
 */
public interface ExtendedPersistenceInheritanceStrategy {

    /**
     * Register the extended persistence context so it is accessible to other SFSB's during the
     * creation process.
     *
     * Used when the SFSB bean is injecting fields (at creation time), after the bean is created but before its
     * PostConstruct has been invoked.
     *
     * @param scopedPuName
     * @param entityManager
     */
    void registerExtendedPersistenceContext(
            String scopedPuName,
            ExtendedEntityManager entityManager);


    /**
     * Check if the current EJB container instance contains the specified extended persistence context.
     *
     * This is expected to be used when the SFSB bean is injecting fields, after it the bean is created but before its
     * PostConstruct has been invoked.
     *
     * @param puScopedName Scoped pu name
     *
     * @return the extended persistence context that matches puScopedName or null if not found
     */
    ExtendedEntityManager findExtendedPersistenceContext(String puScopedName);
}
