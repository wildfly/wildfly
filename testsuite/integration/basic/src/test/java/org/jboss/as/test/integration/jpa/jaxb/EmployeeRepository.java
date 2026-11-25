/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.jaxb;

import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Stateful
@Transactional
public class EmployeeRepository {
    @PersistenceContext
    EntityManager em;

    public void create(Employee employee) {
        em.persist(employee);
    }

    public Employee get(int id) {
        return em.find(Employee.class, id);
    }
}
