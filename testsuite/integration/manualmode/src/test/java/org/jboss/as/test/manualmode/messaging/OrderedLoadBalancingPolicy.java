/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.messaging;

import org.apache.activemq.artemis.api.core.client.loadbalance.ConnectionLoadBalancingPolicy;

/**
 * Simple add-on for Activemq Artemis.
 * @author Emmanuel Hugonnet (c) 2022 Red Hat, Inc.
 */
public class OrderedLoadBalancingPolicy implements ConnectionLoadBalancingPolicy {

    private int pos = -1;

    @Override
    public int select(final int max) {
        pos++;
        if (pos >= max) {
            pos = 0;
        }
        return pos;
    }
}
