/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.forwarding;

import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.stateful.RemoteStatefulSB;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.logging.Logger;

/**
 * @author Radoslav Husar
 */
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public abstract class AbstractForwardingStatefulSBImpl {

    private static final Logger log = Logger.getLogger(AbstractForwardingStatefulSBImpl.class.getName());
    public static final String MODULE_NAME = AbstractForwardingStatefulSBImpl.class.getSimpleName() + "-terminus";

    private RemoteStatefulSB bean;

    private RemoteStatefulSB forward() {
        if (bean == null) {
            try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
                bean = directory.lookupStateful("RemoteStatefulSBImpl", RemoteStatefulSB.class);
            } catch (Exception e) {
                log.infof("exception occurred looking up ejb on forwarding node %s", getCurrentNode());
                throw new RuntimeException(e);
            }
        }
        return bean;
    }

    public int getSerialAndIncrement() {
        log.debugf("getSerialAndIncrement() called on forwarding node %s", getCurrentNode());
        return forward().getSerialAndIncrement();
    }

    private static String getCurrentNode() {
        return System.getProperty("jboss.node.name", "unknown");
    }
}
