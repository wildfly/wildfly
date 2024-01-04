/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.persistenceunitref;

import jakarta.annotation.ManagedBean;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

/**
 * Managed bean with persistence unit definitions.
 * @author Stuart Douglas
 */
@ManagedBean("puManagedBean")
public class PuManagedBean {

    @PersistenceUnit(unitName = "mypc")
    private EntityManagerFactory mypu;

    //this one will be overridden via deployment descriptor to be otherpc
    @PersistenceUnit(unitName = "mypc", name = "otherPcBinding")
    private EntityManagerFactory otherpc;

    //this one is injected via deployment descriptor
    private EntityManagerFactory mypu2;

    public EntityManagerFactory getMypu2() {
        return mypu2;
    }

    public EntityManagerFactory getMypu() {
        return mypu;
    }

    public EntityManagerFactory getOtherpc() {
        return otherpc;
    }
}
