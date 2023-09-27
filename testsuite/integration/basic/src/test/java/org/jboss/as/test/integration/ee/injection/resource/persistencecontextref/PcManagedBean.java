/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.persistencecontextref;

import jakarta.annotation.ManagedBean;
import jakarta.ejb.EJB;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Managed bean with persistence unit definitions.
 * @author Stuart Douglas
 */
@ManagedBean("pcManagedBean")
public class PcManagedBean {

    @PersistenceContext(unitName = "mypc")
    private EntityManager mypc;

    //this one will be overridden via deployment descriptor to be otherpu
    @PersistenceContext(unitName = "mypc", name = "otherPcBinding")
    private EntityManager otherpc;

    @EJB
    SFSB sfsb;


    //this one is injected via deployment descriptor
    private EntityManager mypc2;

    public EntityManager getMypc2() {
        return mypc2;
    }

    public EntityManager getMypc() {
        return mypc;
    }

    public EntityManager getOtherpc() {
        return otherpc;
    }

    public boolean unsynchronizedIsNotJoinedToTX() {
        return sfsb.unsynchronizedIsNotJoinedToTX();
    }

    public boolean synchronizedIsJoinedToTX() {
        return sfsb.synchronizedIsJoinedToTX();
    }

}
