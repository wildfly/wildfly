/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.transaction.nooutbound;

import java.rmi.RemoteException;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

@Stateless
public class ClientStatelessBean {
    private static final Logger log = Logger.getLogger(ClientStatelessBean.class);

    Function<ProviderUrlData, InitialContext> initialContextGetter = (providerUrl) -> {
        Properties jndiProperties = new Properties();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        jndiProperties.put(javax.naming.Context.PROVIDER_URL, providerUrl.getProviderUrl());
        jndiProperties.put(Context.SECURITY_PRINCIPAL, "user1");
        jndiProperties.put(Context.SECURITY_CREDENTIALS, "password1");
        // the authentication is forced to go through remote MD5 authentication
        jndiProperties.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");
        try {
            return new InitialContext(jndiProperties);
        } catch (NamingException ne) {
            throw new IllegalStateException("Cannot create initial context for '" + providerUrl + "'", ne);
        }
    };
    BiFunction<ProviderUrlData, String, Integer> remoteCall = (providerUrl, beanName) -> {
        InitialContext ctx = initialContextGetter.apply(providerUrl);
        try {
            ServerStatelessRemote bean = (ServerStatelessRemote) ctx.lookup("ejb:/" +
                TransactionContextRemoteCallTestCase.SERVER_DEPLOYMENT + "/" + beanName + "!" + ServerStatelessRemote.class.getName());
            return bean.transactionStatus();
        } catch (NamingException | RemoteException nre) {
            throw new IllegalStateException("Cannot do remote bean call '" + beanName + "' with context " + ctx, nre);
        } finally {
            try {
                ctx.close();
            } catch (NamingException closeNamingException) {
                log.warn("Cannot close context after remote call", closeNamingException);
            }
        }
    };

    public void call(ProviderUrlData providerUrl, String beanName) {
        Assert.assertEquals(new Integer(0),
                remoteCall.apply(providerUrl, beanName));
    }
}
