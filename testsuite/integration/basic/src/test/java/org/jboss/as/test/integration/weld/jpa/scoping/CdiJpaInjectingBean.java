/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.jpa.scoping;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceProperty;
import jakarta.persistence.Query;


public class CdiJpaInjectingBean {

    @PersistenceContext(unitName = "cdiPu", properties = { @PersistenceProperty(name = "CdiJpaInjectingBean", value = "true")})
    EntityManager em;

    public Employee queryEmployeeName(int id) {
        Query q = em.createQuery("SELECT e FROM Employee e where e.id=:employeeId");
        q.setParameter("employeeId", id);
        return (Employee) q.getSingleResult();
    }

    public String getInitialPropertyValue() {
        return (String) em.getProperties().get("CdiJpaInjectingBean");
    }

    public void setAdditionalPropertyValue(String value) {
        System.out.println("setAdditionalPropertyValue on em=" + em);
        em.setProperty("CdiJpaInjectingBean.additional", value);
    }

    public String getAdditionalPropertyValue() {
        System.out.println("getAdditionalPropertyValue on em=" + em);
        return (String) em.getProperties().get("CdiJpaInjectingBean.additional");
    }
}
