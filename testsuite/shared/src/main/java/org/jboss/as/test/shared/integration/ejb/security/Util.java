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
package org.jboss.as.test.shared.integration.ejb.security;

import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.concurrent.Callable;

import javax.ejb.EJBAccessException;
import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.evidence.PasswordGuessEvidence;

/**
 * Holder for couple of utility methods used while testing EJB3 security.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class Util {

    /**
     * Creates JNDI context string based on given parameters.
     * See details at https://docs.jboss.org/author/display/AS71/EJB+invocations+from+a+remote+client+using+JNDI
     *
     * @param appName - typically the ear name without the .ear
     *                - could be empty string when deploying just jar with EJBs
     * @param moduleName - jar file name without trailing .jar
     * @param distinctName - AS7 allows each deployment to have an (optional) distinct name
     *                     - could be empty string when not specified
     * @param beanName - The EJB name which by default is the simple class name of the bean implementation class
     * @param viewClassName - the remote view is fully qualified class name of @Remote EJB interface
     * @param isStateful - if the bean is stateful set to true
     *
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
     * @throws NamingException
     */
    public static Context createNamingContext() throws NamingException {

        final Properties jndiProps = new Properties();
        jndiProps.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");

        return new InitialContext(jndiProps);

    }

    /**
     * Switch the user's identity using Elytron.
     *
     * @param username the new username
     * @param password the new password
     * @param callable the callable task to execute under the new identity
     * @param <T> the result type of the callable task
     * @return the result of the callable task
     * @throws Exception if an error occurs while switching the user's identity or if an error occurs while executing the callable task
     */
    public static <T> T switchIdentity(final String username, final String password, final Callable<T> callable) throws Exception {
        return switchIdentity(username, password, callable, false);
    }

    /**
     * Switch the user's identity using Elytron.
     *
     * @param username the new username
     * @param password the new password
     * @param callable the callable task to execute under the new identity
     * @param classLoader the class loader to use when checking for a security domain association
     * @param <T> the result type of the callable task
     * @return the result of the callable task
     * @throws Exception if an error occurs while switching the user's identity or if an error occurs while executing the callable task
     */
    public static <T> T switchIdentity(final String username, final String password, final Callable<T> callable, final ClassLoader classLoader) throws Exception {
        return switchIdentity(username, password, callable, false, classLoader);
    }

    /**
     * Switch the user's identity using Elytron.
     *
     * @param username the new username
     * @param password the new password
     * @param callable the callable task to execute under the new identity
     * @param validateException whether or not to validate an exception thrown by the callable task
     * @param useClientLoginModule {@code true} if {@link ClientLoginModule} should be used for legacy security,
     * {@code false} if {@link SecurityClientFactory} should be used for legacy security instead
     * @param <T> the result type of the callable task
     * @return the result of the callable task
     * @throws Exception if an error occurs while switching the user's identity or if an error occurs while executing the callable task
     */
    public static <T> T switchIdentity(final String username, final String password, final Callable<T> callable, boolean validateException) throws Exception {
        return switchIdentity(username, password, callable, validateException,  null);
    }

    /**
     * Switch the user's identity using Elytron.
     *
     * @param username the new username
     * @param password the new password
     * @param callable the callable task to execute under the new identity
     * @param validateException whether or not to validate an exception thrown by the callable task
     * {@code false} if {@link SecurityClientFactory} should be used for legacy security instead
     * @param classLoader the class loader to use when checking for a security domain association
     * @param <T> the result type of the callable task
     * @return the result of the callable task
     * @throws Exception if an error occurs while switching the user's identity or if an error occurs while executing the callable task
     */
    public static <T> T switchIdentity(final String username, final String password, final Callable<T> callable, boolean validateException, final ClassLoader classLoader) throws Exception {
        boolean initialAuthSucceeded = false;
        try {
            if (username != null && password != null) {
                final SecurityDomain securityDomain;
                if (classLoader != null) {
                    final ClassLoader current = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(classLoader);
                        securityDomain = SecurityDomain.getCurrent();
                    } finally {
                        Thread.currentThread().setContextClassLoader(current);
                    }
                } else {
                    securityDomain = SecurityDomain.getCurrent();
                }
                if (securityDomain != null) {
                    // elytron is enabled, use the new way to switch the identity
                    final SecurityIdentity securityIdentity = securityDomain.authenticate(username, new PasswordGuessEvidence(password.toCharArray()));
                    initialAuthSucceeded = true;
                    return securityIdentity.runAs(callable);
                } else {
                    // legacy security is enabled, use the ClientLoginModule or SecurityClientFactory to switch the identity
                    throw new IllegalStateException("Legacy security is no longer supported.");
                }
            }
            return callable.call();
        } catch (Exception e) {
            if (validateException) {
                validateException(e, initialAuthSucceeded);
            } else {
                throw e;
            }
        }
        return null;
    }

    private static void validateException(final Exception e, final boolean initialAuthSucceeded) {
        if (SecurityDomain.getCurrent() != null) {
            if (initialAuthSucceeded) {
                assertTrue("Expected EJBException due to bad password not thrown.", e instanceof EJBException && e.getCause() instanceof SecurityException);
            } else {
                assertTrue("Expected SecurityException due to bad password not thrown.", e instanceof SecurityException);
            }
        } else {
            assertTrue("Expected EJBAccessException due to bad password not thrown. (EJB 3.1 FR 17.6.9)", e instanceof EJBAccessException);
        }
    }

    /**
     * Switch the user's identity using Elytron.
     *
     * @param username the new username
     * @param password the new password
     * @param callable the callable task to execute under the new identity
     * @param <T> the result type of the callable task
     * @return the result of the callable task
     * @throws Exception if an error occurs while switching the user's identity or if an error occurs while executing the callable task
     */
    public static <T> T switchIdentitySCF(final String username, final String password, final Callable<T> callable) throws Exception {
        return switchIdentity(username, password, callable, false);
    }
}
