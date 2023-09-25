/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.forwarding;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.stateful.RemoteStatefulSB;

@Stateful
@TransactionAttribute(TransactionAttributeType.REQUIRED) // this is the default anyway
public class ForwardingStatefulSBImpl extends AbstractForwardingStatefulSBImpl implements RemoteStatefulSB {

    // We need to override these methods so that the TransactionAttribute gets processed on this class!

    @Override
    public int getSerialAndIncrement() {
        return super.getSerialAndIncrement();
    }
}
