/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.transactions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.integration.ejb.security.CallbackHandler;
import org.jboss.logging.Logger;

/**
 * Util class which is used for remote lookups in transaction related tests.
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
public final class RemoteLookups {
    private static final Logger log = Logger.getLogger(RemoteLookups.class);

    private RemoteLookups() {
        // not instance here
    }

    public static <T> T lookupModule(InitialContext ctx, Class<T> beanType) throws NamingException {
        String lookupString = String.format("java:module/%s!%s", beanType.getSimpleName(), beanType.getName());
        log.debugf("looking for: %s", lookupString);
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
            Class<T> remoteInterface) throws NamingException, URISyntaxException {
        return lookupEjb(host, port, archiveName, beanType, remoteInterface, false);
    }

    public static <T> T lookupEjbStateful(String host, int port, String archiveName, Class<? extends T> beanType,
            Class<T> remoteInterface) throws NamingException, URISyntaxException {
        return lookupEjb(host, port, archiveName, beanType, remoteInterface, true);
    }

    public static <T> T lookupEjbStateless(final ManagementClient managementClient, final String archiveName, Class<? extends T> beanType,
                                           final Class<T> remoteInterface) throws NamingException, URISyntaxException {
        return lookupEjb(managementClient, archiveName, beanType, remoteInterface, false);
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
                "",
                 archiveName,
                "",
                 beanType.getSimpleName(),
                 remoteInterface.getName(),
                 isStateful);
        log.debugf("looking for: %s", ejbLookup);
        return remoteInterface.cast(ctx.lookup(ejbLookup));
    }

    private static <T> T lookupEjb(String host, int port, String archiveName, Class<? extends T> beanType,
            Class<T> remoteInterface, boolean isStateful) throws NamingException, URISyntaxException {
        InitialContext ctx = getRemoteContext(host, port);
        return lookupEjb(ctx, archiveName, beanType, remoteInterface, isStateful);
    }

    private static <T> T lookupEjb(ManagementClient managementClient, String archiveName, Class<? extends T> beanType,
                                   Class<T> remoteInterface, boolean isStateful) throws NamingException, URISyntaxException {
        InitialContext ctx = getRemoteContext(managementClient);
        return lookupEjb(ctx, archiveName, beanType, remoteInterface, isStateful);
    }

    private static InitialContext getRemoteContext(final String host, int port) throws NamingException, URISyntaxException {
        Properties props = getEjbClientProperties();
        URI namingUri = new URI("remote+http", null, host, port, "", "", "");
        props.put(Context.PROVIDER_URL, namingUri.toString());
        return new InitialContext(props);
    }

    private static InitialContext getRemoteContext(final ManagementClient managementClient) throws NamingException, URISyntaxException {
        Properties props = getEjbClientProperties();
        URI webUri = managementClient.getWebUri();
        URI namingUri = new URI("remote+http", webUri.getUserInfo(), webUri.getHost(), webUri.getPort(), "", "", "");
        props.put(Context.PROVIDER_URL, namingUri.toString());
        return new InitialContext(props);
    }

    private static Properties getEjbClientProperties() {
        final Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, org.wildfly.naming.client.WildFlyInitialContextFactory.class.getName());
        props.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
        props.put("jboss.naming.client.security.callback.handler.class", CallbackHandler.class.getName());
        props.put("jboss.naming.client.ejb.context", true);
        return props;
    }
}
