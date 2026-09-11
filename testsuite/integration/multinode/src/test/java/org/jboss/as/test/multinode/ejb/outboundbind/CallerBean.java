/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.ejb.outboundbind;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import javax.naming.InitialContext;

/**
 * Deployed client EJB that looks up {@link SourceAddressBean} via the configured
 * remoting profile (server-side configuration) and returns the source address the
 * server sees for this connection.
 *
 * <p>The connection goes through the IO worker whose {@code outbound-bind-address}
 * is configured by the test setup. The source address reported by
 * {@link SourceAddressBean} should match the configured bind address.</p>
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CallerBean implements CallerRemote {

    @Override
    public String callAndGetSourceAddress() throws Exception {
        InitialContext ctx = new InitialContext();
        String lookupName = "ejb:/outbound-bind-address-server/SourceAddressBean!" +
                SourceAddressRemote.class.getName() + "?stateful";
        SourceAddressRemote bean = (SourceAddressRemote) ctx.lookup(lookupName);
        return bean.getCallerSourceAddress();
    }
}
