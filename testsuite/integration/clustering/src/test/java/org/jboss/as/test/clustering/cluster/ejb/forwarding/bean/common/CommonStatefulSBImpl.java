/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.common;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Remove;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import org.jboss.logging.Logger;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CommonStatefulSBImpl implements CommonStatefulSB {

    private int serial;
    private static final Logger log = Logger.getLogger(CommonStatefulSBImpl.class.getName());

    @PostConstruct
    private void init() {
        serial = 0;
        log.debugf("New SFSB created: %s.", this);
    }

    @Override
    public int getSerialAndIncrement() {
        log.debugf("getSerialAndIncrement() called on non-forwarding node %s", getCurrentNode());
        return serial++;
    }

    @Remove
    private void destroy() {
        serial = -1;
    }

    private static String getCurrentNode() {
        return System.getProperty("jboss.node.name", "unknown");
    }
}
