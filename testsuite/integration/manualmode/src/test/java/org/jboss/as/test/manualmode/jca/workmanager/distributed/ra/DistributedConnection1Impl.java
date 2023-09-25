/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.jca.workmanager.distributed.ra;

public class DistributedConnection1Impl implements DistributedConnection1 {

    private DistributedManagedConnection1 dmc;
    private DistributedManagedConnectionFactory1 dmcf;

    public DistributedConnection1Impl(DistributedManagedConnection1 dmc, DistributedManagedConnectionFactory1 dmcf) {
        this.dmc = dmc;
        this.dmcf = dmcf;
    }

    public String test(String s) {
        return null;
    }

    public void close() {
        dmc.closeHandle(this);
    }
}
