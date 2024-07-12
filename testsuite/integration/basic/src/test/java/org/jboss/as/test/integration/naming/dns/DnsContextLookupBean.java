/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.dns;

import jakarta.ejb.Stateless;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@Stateless
public class DnsContextLookupBean {

    public void testDnsContextLookup(String serverAddress) throws Exception {
        final Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put(Context.PROVIDER_URL, "dns://" + serverAddress);

        Context ctx = new InitialContext(env);
        ctx.close();
    }
}
