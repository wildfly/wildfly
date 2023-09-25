/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.epcpropagation.slsbxpc;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateless
public class StatelessBeanWithXPC {
    @PersistenceContext(unitName = "mypc", type = PersistenceContextType.EXTENDED)
    EntityManager em;

    public void test() {
        em.getEntityManagerFactory();
    }
}
