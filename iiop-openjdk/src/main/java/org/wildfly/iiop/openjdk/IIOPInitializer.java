/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.iiop.openjdk;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Enumeration of {@code ORB} initializer groups. Each member contains one or more initializer classes that belong to a
 * specific group.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public enum IIOPInitializer {

    UNKNOWN("", ""),

    // the security group encompasses both CSIv2 and SAS initializers.
    SECURITY_CLIENT("client", "org.wildfly.iiop.openjdk.csiv2.CSIv2Initializer", "org.wildfly.iiop.openjdk.csiv2.SASClientInitializer"),
    SECURITY_IDENTITY("identity", "org.wildfly.iiop.openjdk.csiv2.CSIv2Initializer", "org.wildfly.iiop.openjdk.csiv2.SASInitializer"),
    SECURITY_ELYTRON("elytron", "org.wildfly.iiop.openjdk.csiv2.CSIv2Initializer", "org.wildfly.iiop.openjdk.csiv2.ElytronSASInitializer"),

    // the transaction group encompasses the Interposition and InboundCurrent initializers.
    TRANSACTIONS("transactions",
            "com.arjuna.ats.jts.orbspecific.javaidl.interceptors.interposition.InterpositionORBInitializerImpl",
            "org.jboss.iiop.tm.InboundTransactionCurrentInitializer",
            "org.wildfly.iiop.openjdk.tm.TxIORInterceptorInitializer",
            "org.wildfly.iiop.openjdk.tm.TxServerInterceptorInitializer"),

    // the transaction group that is used for spec compliant mode without JTS
    SPEC_TRANSACTIONS("specTransactions",
            "org.wildfly.iiop.openjdk.tm.TxServerInterceptorInitializer");

    private final String initializerName;
    private final String[] initializerClasses;

    /**
     * <p>
     * {@code ORBInitializer} constructor. Sets the group name and implementation classes of the initializers that are
     * part of the group.
     * </p>
     *
     * @param initializerName    the name that identifies the initializer group.
     * @param initializerClasses an array containing the fully-qualified name of the initializers that compose the group.
     */
    IIOPInitializer(final String initializerName, final String... initializerClasses) {
        this.initializerName = initializerName;
        this.initializerClasses = initializerClasses;
    }

    /**
     * <p>
     * Obtains the name of this initializer group.
     * </p>
     *
     * @return a {@code String} that represents the initializer group name.
     */
    public String getInitializerName() {
        return this.initializerName;
    }

    /**
     * <p>
     * Obtains the class names of the initializers that are part of this group.
     * </p>
     *
     * @return a {@code String[]} containing the fully-qualified class names of the initializers.
     */
    public String[] getInitializerClasses() {
        return this.initializerClasses;
    }

    // a map that caches all available initializer groups by name.
    private static final Map<String, IIOPInitializer> MAP;

    static {
        final Map<String, IIOPInitializer> map = new HashMap<String, IIOPInitializer>();
        for (IIOPInitializer element : values()) {
            final String name = element.getInitializerName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    /**
     * <p>
     * Gets the {@code ORBInitializer} instance identified by the specified group name.
     * </p>
     *
     * @param initializerName a {@code String} representing the initializer group name.
     * @return the {@code ORBInitializer} identified by the name. If no implementation can be found, the
     *         {@code ORBInitializer.UNKNOWN} type is returned.
     */
    static IIOPInitializer fromName(final String initializerName) {
        final IIOPInitializer element = MAP.get(initializerName);
        return element == null ? UNKNOWN : element;
    }

}