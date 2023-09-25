/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.security;

import java.net.UnknownHostException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Utility class for looking up EJBs. It contains also some common constants for this test.
 *
 * @author Josef Cacek
 */
class EJBUtil {

    protected static final String APPLICATION_NAME = "ejb-remote-security-test";

    protected static final String CONNECTION_USERNAME = "guest";
    protected static final String CONNECTION_PASSWORD = "guest";

    // Public methods --------------------------------------------------------

    /**
     * Lookup for remote EJBs.
     *
     * @param beanImplClass
     * @param remoteInterface
     * @param ejbProperties
     * @return
     * @throws NamingException
     */
    @SuppressWarnings("unchecked")
    public static <T> T lookupEJB(Class<? extends T> beanImplClass, Class<T> remoteInterface, Properties ejbProperties) throws Exception {
        final Properties jndiProperties = new Properties();
        jndiProperties.putAll(ejbProperties);
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        //        jndiProperties.put("jboss.naming.client.ejb.context", "true");
        final Context context = new InitialContext(jndiProperties);

        return (T) context.lookup("ejb:/" + APPLICATION_NAME + "/" + beanImplClass.getSimpleName() + "!"
                + remoteInterface.getName());
    }

    /**
     * Creates {@link Properties} for the EJB client configuration.
     *
     * <pre>
     * remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED=false
     *
     * remote.connections=default
     *
     * remote.connection.default.host=localhost
     * remote.connection.default.port = 8080
     * remote.connection.default.username=guest
     * remote.connection.default.password=guest
     *
     * remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED=false
     * </pre>
     *
     * @param hostName
     * @return
     * @throws UnknownHostException
     */
    public static Properties createEjbClientConfiguration(String hostName) throws UnknownHostException {
        final Properties pr = new Properties();
        pr.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
        pr.put("remote.connection.default.connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");
        pr.put("remote.connections", "default");
        pr.put("remote.connection.default.host", hostName);
        pr.put("remote.connection.default.port", "8080");
        pr.put("remote.connection.default.username", CONNECTION_USERNAME);
        pr.put("remote.connection.default.password", CONNECTION_PASSWORD);
        return pr;
    }

}
