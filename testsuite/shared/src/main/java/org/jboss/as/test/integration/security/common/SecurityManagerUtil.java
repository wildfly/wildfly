/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.security.common;

/**
 * Utility to check if the application server is launched with Security manager enabled.
 *
 * @author Josef Cacek
 */
public class SecurityManagerUtil {

    private static Boolean securityManagerUsed;

    /**
     * Utility to check if the application server runs with security manager enabled.
     *
     * @return true if system property server.jvm.args contains "-Djava.security.manager" or System.getSecurityManager() is not-
     *         <code>null</code>.
     */
    public static boolean isSecurityManagerUsed() {
        if (securityManagerUsed == null) {
            // Check if java.security.manager option is present in system property, which come from Maven surefire-plugin and is
            // used as JVM args line in arquillian.xml.
            // Also check if the test itself runs under Security manager (i.e. it runs in a secured container)
            securityManagerUsed = System.getProperty("server.jvm.args", "").contains("-Djava.security.manager")
                    || System.getSecurityManager() != null;
        }
        return securityManagerUsed;
    }
}
