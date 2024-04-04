/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.persistencecontextref;

import jakarta.ejb.EJB;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Abstract superclass for EE components with persistence unit definitions.
 * Concrete implementations using different component tupes are in src/test/ee10/java and src/test/ee11/java.
 *
 * @author Stuart Douglas
 */
public abstract class AbstractPcBean {

    @PersistenceContext(unitName = "mypc")
    private EntityManager mypc;

    //this one will be overridden via deployment descriptor to be otherpu
    @PersistenceContext(unitName = "mypc", name = "otherPcBinding")
    private EntityManager otherpc;

    @EJB
    SFSB sfsb;

    public abstract EntityManager getMypc2();

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
