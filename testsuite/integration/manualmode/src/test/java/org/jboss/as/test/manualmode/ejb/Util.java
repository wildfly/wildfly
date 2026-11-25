/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.ejb;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class Util {

    /**
     * Creates JNDI context string based on given parameters.
     * See details at <i>EJB invocations from a remote client using JNDI</i> section of WildFly developer documentation.
     *
     * @param appName       - typically the ear name without the .ear
     *                      - could be empty string when deploying just jar with EJBs
     * @param moduleName    - jar file name without trailing .jar
     * @param distinctName  - AS7 allows each deployment to have an (optional) distinct name
     *                      - could be empty string when not specified
     * @param beanName      - The EJB name which by default is the simple class name of the bean implementation class
     * @param viewClassName - the remote view is fully qualified class name of @Remote EJB interface
     * @param isStateful    - if the bean is stateful set to true
     * @return - JNDI context string to use in your client JNDI lookup
     */
    public static String createRemoteEjbJndiContext(
            String appName,
            String moduleName,
            String distinctName,
            String beanName,
            String viewClassName,
            boolean isStateful) {

        return "ejb:" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!" + viewClassName
                + (isStateful ? "?stateful" : "");
    }

    /**
     * Helper to create InitialContext with necessary properties.
     *
     * @return new InitialContext.
     * @throws javax.naming.NamingException
     */
    public static Context createNamingContext() throws NamingException {

        final Properties jndiProps = new Properties();
        jndiProps.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");

        return new InitialContext(jndiProps);

    }

    /**
     * Helper to create the InitialContext with the given properties.
     *
     * @param properties the environment properties
     * @return the constructed InitialContext
     * @throws NamingException if an error occurs while creating the InitialContext
     */
    public static Context createNamingContext(final Properties properties) throws NamingException {
        final Properties jndiProps = new Properties();
        jndiProps.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        jndiProps.putAll(properties);
        return new InitialContext(jndiProps);

    }

    public static <T> T lookup(final String name, final Class<T> cls) throws NamingException {
        InitialContext ctx = new InitialContext();
        try {
            return cls.cast(ctx.lookup(name));
        } finally {
            ctx.close();
        }
    }

}
