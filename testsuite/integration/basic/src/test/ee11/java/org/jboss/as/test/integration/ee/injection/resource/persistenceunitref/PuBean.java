/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.persistenceunitref;

import jakarta.ejb.Singleton;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

/**
 * Managed bean with persistence unit definitions.
 * In the ee11 source tree this is an {@code @ManagedBean}, a type that is not available in EE 11.
 *
 * @author Stuart Douglas
 */
@Singleton(name="puManagedBean")
public class PuBean {

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
