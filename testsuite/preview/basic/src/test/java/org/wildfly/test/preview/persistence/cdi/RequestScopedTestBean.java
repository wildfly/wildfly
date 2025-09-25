/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.preview.persistence.cdi;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.persistence.Cache;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.SchemaManager;
import jakarta.transaction.Transactional;

/**
 * test bean
 *
 * @author Scott Marlow
 */

@RequestScoped
public class RequestScopedTestBean {

    @Produces
    @PersistenceContext(unitName = "pu2")
    private EntityManager emApplicationExistingProducer;

    @Inject
    @Pu1Qualifier
    EntityManager em;

    @Inject
    @Pu1Qualifier
    EntityManagerFactory emf;

    @Inject
    @Pu2Qualifier
    EntityManagerFactory emf2;

    @Inject
    @Pu1Qualifier
    CriteriaBuilder criteriaBuilder;

    @Inject
    @Pu1Qualifier
    PersistenceUnitUtil persistenceUnitUtil;

    @Inject
    @Pu1Qualifier
    Cache cache;

    @Inject
    @Pu1Qualifier
    Metamodel metamodel;

    @Inject
    @Pu1Qualifier
    SchemaManager schemaManager;

    // The persistence unit with persistence unit hint "wildfly.jpa.default-unit" set to true should be the default persistence unit that is injected.
    @Inject
    EntityManagerFactory defaultEntityManagerFactory;

    @Transactional
    public Employee getEmployeeExpectNullResult(int id) {
        return em.find(Employee.class, id);
    }

    @Transactional
    public EntityManagerFactory entityManagerFactoryOfEntityManager() {
        return em.getEntityManagerFactory();
    }

    public EntityManagerFactory injectedEntityManagerFactory() {
        return emf;
    }

    public CriteriaQuery<Object> testCreateQuery() {
        return criteriaBuilder.createQuery();
    }

    public PersistenceUnitUtil testPersistenceUnitUtil() {
        return persistenceUnitUtil;
    }

    public Cache testCache() {
        return cache;
    }

    public EntityManager getEmApplicationExistingProducer() {
        return emApplicationExistingProducer;
    }

    public Metamodel testMetamodel() {
        return metamodel;
    }

    public SchemaManager testSchemaManager() {
        return schemaManager;
    }

    public EntityManagerFactory getDefaultEntityManagerFactory() {
        return defaultEntityManagerFactory;
    }

}
