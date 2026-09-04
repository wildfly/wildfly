/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.client.outboundbind;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Returns the TCP source address of the caller as populated by jboss-remoting
 * in the EJB invocation context under the key {@code jboss.source-address}.
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SourceAddressBean implements SourceAddressRemote {

    @Resource
    private SessionContext ctx;

    @Override
    public String getCallerSourceAddress() {
        Object addr = ctx.getContextData().get("jboss.source-address");
        return addr == null ? "null" : addr.toString();
    }
}
