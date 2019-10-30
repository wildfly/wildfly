/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.transactions;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import org.jboss.logging.Logger;

/**
 * Util class which is used for remote lookups in transaction relatd tests.
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
public final class TransactionTestLookupUtil {
    private static final Logger log = Logger.getLogger(TransactionTestLookupUtil.class);

    private TransactionTestLookupUtil() {
        // not instance here
    }

    public static <T> T lookupModule(InitialContext ctx, Class<T> beanType) throws NamingException {
        String lookupString = String.format("java:module/%s!%s", beanType.getSimpleName(), beanType.getName());
        log.debug("looking for: " + lookupString);
        return beanType.cast(ctx.lookup(lookupString));
    }

    public static <T> T lookupEjbStateless(InitialContext ctx, String archiveName, Class<? extends T> beanType,
            Class<T> remoteInterface) throws NamingException {
        return lookupEjb(ctx, archiveName, beanType, remoteInterface, false);
    }

    public static <T> T lookupEjbStateful(InitialContext ctx, String archiveName, Class<? extends T> beanType,
            Class<T> remoteInterface) throws NamingException {
        return lookupEjb(ctx, archiveName, beanType, remoteInterface, true);
    }

    public static <T> T lookupEjbStateless(String host, int port, String archiveName, Class<? extends T> beanType,
            Class<T> remoteInterface) throws NamingException {
        return lookupEjb(host, port, archiveName, beanType, remoteInterface, false);
    }

    public static <T> T lookupEjbStateful(String host, int port, String archiveName, Class<? extends T> beanType,
            Class<T> remoteInterface) throws NamingException {
        return lookupEjb(host, port, archiveName, beanType, remoteInterface, true);
    }

    public static <T> T lookupIIOP(String serverHost, int iiopPort, Class<T> homeClass, Class<?> beanClass) throws NamingException {
        final Properties prope = new Properties();
        prope.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");
        prope.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.iiop.naming:org.jboss.naming.client");
        prope.put(Context.PROVIDER_URL, "corbaloc::" + serverHost +":" + iiopPort + "/JBoss/Naming/root");
        final InitialContext context = new InitialContext(prope);
        final Object ejbHome = context.lookup(beanClass.getSimpleName());
        return homeClass.cast(PortableRemoteObject.narrow(ejbHome, homeClass));
    }

    private static <T> T lookupEjb(InitialContext ctx, String archiveName, Class<? extends T> beanType,
            Class<T> remoteInterface, boolean isStateful) throws NamingException {
        String ejbLookup = org.jboss.as.test.shared.integration.ejb.security.Util.createRemoteEjbJndiContext(
                "", archiveName, "", beanType.getSimpleName(), remoteInterface.getName(), isStateful);
        log.debug("looking for: " + ejbLookup);
        return remoteInterface.cast(ctx.lookup(ejbLookup));
    }

    private static <T> T lookupEjb(String host, int port, String archiveName, Class<? extends T> beanType,
            Class<T> remoteInterface, boolean isStateful) throws NamingException {
        InitialContext ctx = new InitialContext(getEjbClientProperties(host, port));
        return lookupEjb(ctx, archiveName, beanType, remoteInterface, isStateful);
    }

    private static Properties getEjbClientProperties(String node, int port) {
        Properties props = new Properties();
        props.put("org.jboss.ejb.client.scoped.context", true);
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        props.put("endpoint.name", "client");
        props.put("remote.connections", "main");
        props.put("remote.connection.main.host", node);
        props.put("remote.connection.main.port", Integer.toString(port));
        props.put("remote.connection.main.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
        props.put("remote.connection.main.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "true");
        props.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
        return props;
    }
}
