/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.initializeinorder;

import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;

public class CdiJpaInjectingBean {

    @Produces
    @QualifyEntityManagerFactory
    @PersistenceUnit(unitName = "pu1")
    EntityManagerFactory emf;

    @Produces
    @QualifyEntityManager
    @PersistenceContext(unitName = "pu1")
    EntityManager em;

    public EntityManagerFactory entityManagerFactory() {
        return emf;
    }


    public EntityManager entityManager() {
        return em;
    }
}
