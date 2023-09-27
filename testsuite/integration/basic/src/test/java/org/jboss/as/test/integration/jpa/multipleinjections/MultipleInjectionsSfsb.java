/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.multipleinjections;

import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

/**
 * @author Stuart Douglas
 */
@Stateful
public class MultipleInjectionsSfsb {

    @PersistenceContext
    private EntityManager entityManager;

    @PersistenceContext
    private EntityManager entityManager2;


    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager extendedEntityManager;


    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager extendedEntityManager2;

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public EntityManager getExtendedEntityManager() {
        return extendedEntityManager;
    }

    public EntityManager getEntityManager2() {
        return entityManager2;
    }

    public EntityManager getExtendedEntityManager2() {
        return extendedEntityManager2;
    }
}
