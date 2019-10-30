/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
     * See details at https://docs.jboss.org/author/display/AS71/EJB+invocations+from+a+remote+client+using+JNDI
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
