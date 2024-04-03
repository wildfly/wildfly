/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.persistencecontextref;

import jakarta.annotation.ManagedBean;
import jakarta.persistence.EntityManager;

/**
 * Managed bean with persistence unit definitions.
 * In the ee11 source tree this is an {@code @Singleton} as the {@code @ManagedBean}
 * annotation is not available in EE 11.
 * @author Stuart Douglas
 */
@SuppressWarnings("deprecation")
@ManagedBean("pcManagedBean")
public class PcBean extends AbstractPcBean {

    //this one is injected via deployment descriptor
    private EntityManager mypc2;

    @Override
    public EntityManager getMypc2() {
        return mypc2;
    }
}
