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

    @PersistenceContext(unitName = "cdiPu")
    EntityManager emEmptyProperties;

    public Employee queryEmployeeName(int id) {
        Query q = em.createQuery("SELECT e FROM Employee e where e.id=:employeeId");
        q.setParameter("employeeId", id);
        return (Employee) q.getSingleResult();
    }

    public String getInitialPropertyValue() {
        return (String) em.getProperties().get("CdiJpaInjectingBean");
    }

    public void setAdditionalPropertyValue(String value) {
        em.setProperty("CdiJpaInjectingBean.additional", value);
    }

    public String getAdditionalPropertyValue() {
        return (String) em.getProperties().get("CdiJpaInjectingBean.additional");
    }

    public String addPropertyToEmptyPropertyMap(String value) {
        emEmptyProperties.setProperty("CdiJpaInjectingBean.addToEmptyPropertyMap", value);
        return (String) emEmptyProperties.getProperties().get("CdiJpaInjectingBean.addToEmptyPropertyMap");
    }
}
