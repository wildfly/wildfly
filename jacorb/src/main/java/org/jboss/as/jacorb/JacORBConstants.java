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

/**
 * <p>
 * Collection of constants used in the JacORB subsystem.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public final class JacORBConstants {

    /**
     * <p>
     * Private constructor as required by the {@code Singleton} pattern.
     * </p>
     */
    private JacORBConstants() {
    }

    // common orb properties constants.
    public static final String ORB_ADDRESS = "OAIAddr";
    public static final String ORB_PORT = "OAPort";
    public static final String ORB_SSL_PORT = "OASSLPort";
    public static final String ORB_CLASS = "org.omg.CORBA.ORBClass";
    public static final String ORB_SINGLETON_CLASS = "org.omg.CORBA.ORBSingletonClass";
    public static final String ORB_INITIALIZER_PREFIX = "org.omg.PortableInterceptor.ORBInitializerClass.";

    // JacORB subsystem configuration properties.
    public static final String SSL_COMPONENTS_ENABLED = "ssl_components_enabled";
    public static final String SEND_SAS_ACCEPT_WITH_EXCEPTION = "send_sas_accept_with_exception";
    public static final String NAMING_ROOT_CONTEXT = "root_context";
    public static final String NAMING_DEFAULT_ROOT_CONTEXT = "JBoss/Naming/root";
    public static final String NAMING_EXPORT_CORBALOC = "export_corbaloc";

    // JacORB implementation classes and standard interceptors.
    public static final String JACORB_ORB_CLASS = "org.jacorb.orb.ORB";
    public static final String JacORB_ORB_SINGLETON_CLASS = "org.jacorb.orb.ORBSingleton";
    public static final String JACORB_STD_INITIALIZER_KEY = ORB_INITIALIZER_PREFIX + "standard_init";
    public static final String JACORB_STD_INITIALIZER_VALUE = "org.jacorb.orb.standardInterceptors.IORInterceptorInitializer";

    // JacORB configuration properties that are set directly by the ORB service.
    public static final String JACORB_NAME_SERVICE_INIT_REF = "ORBInitRef.NameService";
    public static final String JACORB_NAME_SERVICE_MAP_KEY = "jacorb.orb.objectKeyMap.NameService";
}
