/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.preview.persistence.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;

/**
 * Produces Jakarta Persistence resources for CDI injection.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@ApplicationScoped
public class PersistenceProducers {

    @Produces
    @PersistenceContext(unitName = "default")
    EntityManager em;

    @Produces
    @PersistenceUnit(unitName = "default")
    EntityManagerFactory emf;

    @Produces
    public CriteriaBuilder criteriaBuilder(final EntityManagerFactory emf) {
        return emf.getCriteriaBuilder();
    }

    @Produces
    public PersistenceUnitUtil persistenceUnitUtil(final EntityManagerFactory emf) {
        return emf.getPersistenceUnitUtil();
    }

    @Produces
    public Metamodel metamodel(final EntityManagerFactory emf) {
        return emf.getMetamodel();
    }

    @Produces
    public SchemaManager schemaManager(final EntityManagerFactory emf) {
        return emf.getSchemaManager();
    }
}
