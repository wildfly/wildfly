/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

public class CdiJpaInjectingBean {
    @PersistenceContext(unitName = "cdiPu")
    EntityManager em;

    public Employee queryEmployeeName(int id) {
        Query q = em.createQuery("SELECT e FROM Employee e where e.id=:employeeId");
        q.setParameter("employeeId", id);
        return (Employee) q.getSingleResult();
    }


}
