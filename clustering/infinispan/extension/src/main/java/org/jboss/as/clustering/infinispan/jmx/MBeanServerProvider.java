/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.jmx;

import java.util.Properties;

import javax.management.MBeanServer;

import org.infinispan.commons.jmx.MBeanServerLookup;

/**
 * @author Paul Ferraro
 */
public class MBeanServerProvider implements MBeanServerLookup {

    private final MBeanServer server;

    public MBeanServerProvider(MBeanServer server) {
        this.server = server;
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.jmx.MBeanServerLookup#getMBeanServer(java.util.Properties)
     */
    @Override
    public MBeanServer getMBeanServer(Properties properties) {
        return this.server;
    }
}
