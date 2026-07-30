/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.client.outboundbind;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;

/**
 * Deployed client EJB that looks up {@link SourceAddressBean} via the configured
 * {@code self-remote-ejb-connection} (http-remoting, loopback) and returns the
 * source address the server sees for this connection.
 *
 * <p>The connection goes through the IO worker whose {@code outbound-bind-address}
 * is configured by the test setup. The source address reported by
 * {@link SourceAddressBean} should match the configured bind address and port.</p>
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CallerBean implements CallerRemote {

    @Override
    public String callAndGetSourceAddress() throws Exception {
        Properties props = new Properties();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        InitialContext ctx = new InitialContext(props);
        SourceAddressRemote bean = (SourceAddressRemote) ctx.lookup(
                "ejb:/" + OutboundBindAddressTestCase.SERVER_MODULE + "/SourceAddressBean!" +
                SourceAddressRemote.class.getName() + "?stateful");
        return bean.getCallerSourceAddress();
    }
}
