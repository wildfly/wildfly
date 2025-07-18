/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.cdi;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */

@RequestScoped
public class SFSB1 {
    @Resource
    private UserTransaction transaction;

    @Inject
    @Pu1Qualifier
    EntityManager em;

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

}
