/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.hibernate.generator;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class TestSlsb {

    @PersistenceContext
    private EntityManager em;

    public void save(Employee employee) {
        em.persist(employee);
    }
}
