/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.epcpropagation.unsync;

import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.persistence.PersistenceProperty;

/**
 * CMT stateful bean
 *
 * @author Scott Marlow
 */
@Stateful
public class CMTPCStatefulBean {
    @PersistenceContext(type = PersistenceContextType.TRANSACTION, unitName = "mypc")
    private EntityManager em;

    @PersistenceContext(type = PersistenceContextType.TRANSACTION, unitName = "mypc", properties={@PersistenceProperty(name="wildfly.jpa.allowjoinedunsync", value="true")})
    private EntityManager allowjoinedunsyncEm;

    @PersistenceContext(type = PersistenceContextType.TRANSACTION, unitName = "allowjoinedunsyncPU")
    private EntityManager allowjoinedunsyncEmViaPersistenceXml;


    public Employee getEmp(int id) {
        return em.find(Employee.class, id);
    }

    public Employee getEmpAllowJoinedUnsync(int id) {
        return allowjoinedunsyncEm.find(Employee.class, id);
    }

    public Employee getEmpAllowJoinedUnsyncPersistenceXML(int id) {
        return allowjoinedunsyncEmViaPersistenceXml.find(Employee.class, id);
    }

}
