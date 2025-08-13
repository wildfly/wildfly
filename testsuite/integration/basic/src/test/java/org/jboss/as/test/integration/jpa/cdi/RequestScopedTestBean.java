/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.cdi;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.Cache;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnitUtil;
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
    @Named("pu1")
    EntityManagerFactory entityManagerFactoryByPuName;

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

    public EntityManagerFactory getEntityManagerFactoryByPuName() {
        return entityManagerFactoryByPuName;
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
}
