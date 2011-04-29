/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jacorb;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Enumeration of the pre-configured {@code ORB} initializers.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public enum ORBInitializer {
    // Unknown is just a default value to avoid dealing with exceptions when parsing the JacORB subsystem.
    UNKNOWN(null, null),
    // Codebase initializer creates the IOR interceptor that inserts the codebase location into generated IORs.
    CODEBASE("Codebase", "org.jboss.as.jacorb.codebase.CodebaseInterceptorInitializer"),
    // CSIv2 initializer creates the IOR interceptor that inserts the security requirements into IORs.
    CSIV2("CSIv2", "org.jboss.as.jacorb.csiv2.CSIv2Initializer"),
    // SAS initializer creates the request interceptors that implement the security attribute service.
    SAS("SAS", "org.jboss.as.jacorb.csiv2.SASInitializer");

    private String initializerName;
    private String initializerClass;

    /**
     * <p>
     * {@code ORBInitializer} constructor. Sets the initializer name and implementation class.
     * </p>
     *
     * @param initializerName  the name that identifies the initializer.
     * @param initializerClass the fully-qualified class name of the initializer.
     */
    ORBInitializer(String initializerName, String initializerClass) {
        this.initializerName = initializerName;
        this.initializerClass = initializerClass;
    }

    /**
     * <p>
     * Obtains the initializer name.
     * </p>
     *
     * @return a {@code String} that represents the initializer name.
     */
    public String getInitializerName() {
        return this.initializerName;
    }

    /**
     * <p>
     * Obtains the initializer class name.
     * </p>
     *
     * @return a {@code String} containing the fully-qualified class name of the initializer.
     */
    public String getInitializerClass() {
        return this.initializerClass;
    }

    // a map that caches all available initializers by name.
    private static final Map<String, ORBInitializer> MAP;

    static {
        final Map<String, ORBInitializer> map = new HashMap<String, ORBInitializer>();
        for (ORBInitializer element : values()) {
            final String name = element.getInitializerName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    /**
     * <p>
     * Gets the {@code ORBInitializer} identified by the specified name.
     * </p>
     *
     * @param initializerName a {@code String} representing the implementation name.
     * @return the {@code ORBInitializer} identified by the name. If no implementation can be found, the
     *         {@code ORBInitializer.UNKNOWN} type is returned.
     */
    static ORBInitializer getInitializer(String initializerName) {
        final ORBInitializer element = MAP.get(initializerName);
        return element == null ? UNKNOWN : element;
    }

}
