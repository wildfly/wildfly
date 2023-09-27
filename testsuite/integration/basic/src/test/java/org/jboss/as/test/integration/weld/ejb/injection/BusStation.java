/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.injection;

import jakarta.ejb.EJB;

/**
 * @author Stuart Douglas
 */
public class BusStation {

    @EJB
    private Bus bus;

    @EJB(lookup = "java:module/Bus")
    private Bus lookupBus;


    @EJB(lookup = "java:comp/Bus")
    private Bus lookupBus2;

    public Bus getBus() {
        return bus;
    }

    public Bus getLookupBus() {
        return lookupBus;
    }

    public Bus getLookupBus2() {
        return lookupBus2;
    }
}
