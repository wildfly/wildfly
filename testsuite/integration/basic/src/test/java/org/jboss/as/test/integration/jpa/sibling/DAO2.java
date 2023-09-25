/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.sibling;

import java.util.Map;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.persistence.PersistenceUnit;

/**
 * @author Scott Marlow
 */
@Stateful
public class DAO2 {

    @PersistenceContext(unitName = "mypc", type = PersistenceContextType.EXTENDED)
    EntityManager em;

    @PersistenceUnit(unitName = "mypc")
    private EntityManagerFactory emf;

    public Map<String, Object> getEMInfo() {
        return emf.getProperties();
    }

    /**
     * The PostConstruct callback invocations occur before the first business method invocation on thebean.
     * This is at a point after which any dependency injection has been performed by the container.
     */
    @PostConstruct
    public void postconstruct() {
        //System.out.println("DAO2 PostConstruct occurred for " + this.toString() +", current thread=" + Thread.currentThread().getName() +", all dependency injection has been performed.");
    }

    public void myFunction() {
        em.find(Employee.class, 123);
    }

}
