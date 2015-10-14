/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.security.common.negotiation;

import static org.jboss.as.test.integration.security.common.Utils.IBM_JDK;
import static org.jboss.as.test.integration.security.common.Utils.OPEN_JDK;
import static org.jboss.as.test.integration.security.common.Utils.ORACLE_JDK;

import org.apache.commons.lang.SystemUtils;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.logging.Logger;
import org.junit.internal.AssumptionViolatedException;

/**
 * Helper methods for JGSSAPI &amp; SPNEGO &amp; Kerberos testcases. It mainly helps to skip tests on configurations which
 * contains issues.
 *
 * @author Josef Cacek
 */
public final class KerberosTestUtils {

    private static final Logger LOGGER = Logger.getLogger(KerberosTestUtils.class);

    public static final boolean PREFER_IPV4_STACK = Boolean
            .parseBoolean(System.getProperty("java.net.preferIPv4Stack", "true"));
    public static final boolean PREFER_IPV6_ADDR = Boolean.getBoolean("java.net.preferIPv6Addresses");

    /**
     * Just a private constructor.
     */
    private KerberosTestUtils() {
        // It's OK to be empty - we don't instantiate this class.
    }

    /**
     * This method throws an {@link AssumptionViolatedException} (i.e. it skips the test-case) if the configuration is
     * unsupported for HTTP authentication with Kerberos. Configuration in this case means combination of [ hostname used | JDK
     * vendor | Java version ].
     *
     * @param hostName
     * @throws AssumptionViolatedException
     */
    public static void assumeKerberosAuthenticationSupported(String hostName) throws AssumptionViolatedException {
        if (IBM_JDK && isRunningOnIPv6()) {
            throw new AssumptionViolatedException(
                    "Kerberos tests are not supported on IBM Java with IPv6. Find more info in https://bugzilla.redhat.com/show_bug.cgi?id=1188632");
        }
        if (isIPv6Hostname(hostName)) {
            if (IBM_JDK && SystemUtils.IS_JAVA_1_7) {
                throw new AssumptionViolatedException(
                        "Kerberos tests are not supported on IBM Java 7 with IPv6-based hostnames. Find more info in https://bugzilla.redhat.com/show_bug.cgi?id=1185917");
            }
            if (IBM_JDK && SystemUtils.IS_JAVA_1_6) {
                throw new AssumptionViolatedException(
                        "Kerberos tests are not supported on IBM Java 6 with IPv6-based hostnames. Find more info in https://bugzilla.redhat.com/show_bug.cgi?id=1186129");
            }
            if (OPEN_JDK && SystemUtils.IS_JAVA_1_6) {
                throw new AssumptionViolatedException(
                        "Kerberos tests are not supported on OpenJDK 6 with IPv6-based hostnames. Find more info in https://bugzilla.redhat.com/show_bug.cgi?id=1186132");
            }
        }
    }

    /**
     * This method throws an {@link AssumptionViolatedException} (i.e. it skips the test-case) if the configuration is
     * unsupported for EJB client authentication with Kerberos. Configuration in this case means combination of [ hostname used
     * | JDK vendor | Java version ].
     *
     * @param hostName
     */
    public static void assumeEjbKerberosAuthenticationSupported(String hostName) {
        // additionally there is an issue in Oracle Java 6 in EJB client
        if (isIPv6Hostname(hostName) && ORACLE_JDK && SystemUtils.IS_JAVA_1_6) {
            throw new AssumptionViolatedException(
                    "Kerberos tests are not supported on Oracle Java 6 with IPv6-based hostnames. Find more info in https://bugzilla.redhat.com/show_bug.cgi?id=1179710");
        }
        // lets check other known issues in basic Kerberos authentication support
        assumeKerberosAuthenticationSupported(hostName);
    }

    /**
     * This method throws an {@link AssumptionViolatedException} (i.e. it skips the test-case) if the configuration is
     * unsupported for JBoss CLI authentication with Kerberos. Configuration in this case means combination of [ hostname used |
     * JDK vendor | Java version ].
     *
     * @param hostName
     * @throws AssumptionViolatedException
     */
    public static void assumeCLIKerberosAuthenticationSupported(String hostName) throws AssumptionViolatedException {
        // there is an issue with kerberos ticket cache location on IBM Java
        if (IBM_JDK) {
            throw new AssumptionViolatedException(
                    "Kerberos CLI tests are not supported on IBM Java. Find more info in https://bugzilla.redhat.com/show_bug.cgi?id=1174156");
        }
        // additionally there is an issue in Oracle Java 6 which doesn't support KRB5CCNAME env. variable to hold cache file
        // location this variable is used in CLI tests
        if (ORACLE_JDK && SystemUtils.IS_JAVA_1_6) {
            throw new AssumptionViolatedException(
                    "Kerberos CLI tests are not supported on Oracle Java 6, because wrong support of KRB5CCNAME environment property. Find more info in https://bugzilla.redhat.com/show_bug.cgi?id=1173530#c3");
        }
        // lets check other known issues in basic Kerberos authentication support
        assumeKerberosAuthenticationSupported(hostName);
    }

    /**
     * Returns true if provided hostname is an IPv6 address.
     *
     * @param hostName
     * @return
     */
    private static boolean isIPv6Hostname(String hostName) {
        if (hostName == null) {
            hostName = Utils.getDefaultHost(true);
            LOGGER.warn("Fallback to a default host is used hostname = " + hostName);
        }
        final String formattedHost = NetworkUtils.formatPossibleIpv6Address(hostName);
        boolean isIPv6 = formattedHost.startsWith("[");
        return isIPv6;
    }

    /**
     * Returns true if the environment is configured to IPv6. I.e. values of the relevant system properties are following:
     *
     * <pre>
     * java.net.preferIPv4Stack     = false
     * java.net.preferIPv6Addresses = true
     * </pre>
     *
     * @return true if running in IPv6 stack. False otherwise.
     */
    private static boolean isRunningOnIPv6() {
        return !PREFER_IPV4_STACK && PREFER_IPV6_ADDR;
    }

}
