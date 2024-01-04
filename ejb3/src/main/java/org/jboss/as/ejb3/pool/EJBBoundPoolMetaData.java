/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.pool;

import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaData;

/**
 * Metadata represents the pool name configured for EJBs via the jboss-ejb3.xml deployment descriptor
 *
 * @author Jaikiran Pai
 */
public class EJBBoundPoolMetaData extends AbstractEJBBoundMetaData {

    private String poolName;

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(final String poolName) {
        this.poolName = poolName;
    }
}
