/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
