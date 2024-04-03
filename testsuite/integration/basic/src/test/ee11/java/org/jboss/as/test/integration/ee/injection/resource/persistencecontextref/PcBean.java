/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.persistencecontextref;

import jakarta.ejb.Singleton;
import jakarta.persistence.EntityManager;

/**
 * Managed bean with persistence unit definitions.
 * In the ee11 source tree this is an {@code @ManagedBean}, a type that is not available in EE 11.
 *
 * @author Stuart Douglas
 */
@Singleton(name="pcManagedBean")
public class PcBean extends AbstractPcBean {


    //this one is injected via deployment descriptor
    private EntityManager mypc2;

    @Override
    public EntityManager getMypc2() {
        return mypc2;
    }
}
