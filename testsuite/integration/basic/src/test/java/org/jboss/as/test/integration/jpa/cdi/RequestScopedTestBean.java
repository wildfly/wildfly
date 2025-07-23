/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.cdi;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.Cache;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.transaction.UserTransaction;

/**
 * test bean
 *
 * @author Scott Marlow
 */

@RequestScoped
public class RequestScopedTestBean {
    @Resource
    private UserTransaction transaction;

    @Inject
    @Pu1Qualifier
    EntityManager em;

    @Inject
    @Pu1Qualifier
    EntityManagerFactory emf;

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


    public Employee getEmployeeExpectNullResult(int id) {

        try {
            transaction.begin();
            return em.find(Employee.class, id);
        } catch( Throwable throwable) {
            throw new RuntimeException(throwable);
        } finally {
            try {
                transaction.rollback();
            } catch (Throwable ignore) {}
        }
    }

    public EntityManagerFactory entityManagerFactoryOfEntityManager() {
        try {
            transaction.begin();
            return em.getEntityManagerFactory();
        } catch( Throwable throwable) {
            throw new RuntimeException(throwable);
        } finally {
            try {
                transaction.rollback();
            } catch (Throwable ignore) {}
        }
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

    public Metamodel testMetamodel() {
        return metamodel;
    }
}
