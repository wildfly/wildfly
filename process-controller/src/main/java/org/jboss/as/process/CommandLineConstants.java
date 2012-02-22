/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.process;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class CommandLineConstants {

    /** The HostController address */
    public static final String OLD_INTERPROCESS_HC_ADDRESS = "-interprocess-hc-address";
    public static final String INTERPROCESS_HC_ADDRESS = "--interprocess-hc-address";

    /** The HostController port */
    public static final String OLD_INTERPROCESS_HC_PORT = "-interprocess-hc-port";
    public static final String INTERPROCESS_HC_PORT = "--interprocess-hc-port";

    /** Get the version of the server */
    public static final String OLD_VERSION = "-version";
    public static final String VERSION = "--version";
    public static final String SHORT_VERSION = "-v";
    public static final String OLD_SHORT_VERSION = "-V";

    /** Configure the file to be used to read properties */
    public static final String OLD_PROPERTIES = "-properties";
    public static final String PROPERTIES = "--properties";
    public static final String SHORT_PROPERTIES = "-P";

    /** Configure a default jvm */
    public static final String OLD_DEFAULT_JVM = "-default-jvm";
    public static final String DEFAULT_JVM = "--default-jvm";

    /** Flag indicating when a process was restarted. */
    public static final String PROCESS_RESTARTED = "--process-restarted";

    /** Passed in when the host controller is respawned by process controller */
    public static final String RESTART_HOST_CONTROLLER = PROCESS_RESTARTED;

    /** Passed in to a slave host controller to get a backup of all files on the domain controller" */
    public static final String OLD_BACKUP_DC = "-backup";
    public static final String BACKUP_DC = "--backup";

    /** Passed in to a slave host controller to attempt to start up using its cached copy of the remote DC, if the remote DC can not be contacted" */
    public static final String OLD_CACHED_DC = "-cached-dc";
    public static final String CACHED_DC = "--cached-dc";

    /** Output usage */
    public static final String OLD_HELP = "-help";
    public static final String HELP = "--help";
    public static final String SHORT_HELP = "-h";

    /** Passed in to a DC to choose the domain.xml file */
    public static final String OLD_DOMAIN_CONFIG = "-domain-config";
    public static final String DOMAIN_CONFIG = "--domain-config";
    public static final String SHORT_DOMAIN_CONFIG = "-c";

    /** Passed in to a HC to choose the host.xml file */
    public static final String OLD_HOST_CONFIG = "-host-config";
    public static final String HOST_CONFIG = "--host-config";

    /** Passed in to a standalone instance to choose the standalone.xml file */
    public static final String SHORT_SERVER_CONFIG = "-c";
    public static final String OLD_SERVER_CONFIG = "-server-config";
    public static final String SERVER_CONFIG = "--server-config";

    /** Address on which the process controller listens */
    public static final String OLD_PROCESS_CONTROLLER_BIND_ADDR = "-bind-addr";
    public static final String PROCESS_CONTROLLER_BIND_ADDR = "--pc-address";

    /** Port on which the process controller listens */
    public static final String OLD_PROCESS_CONTROLLER_BIND_PORT = "-bind-port";
    public static final String PROCESS_CONTROLLER_BIND_PORT = "--pc-port";

    public static final String SYS_PROP = "-D";
    public static final String SECURITY_PROP = "-S";

    public static final String PUBLIC_BIND_ADDRESS = "-b";

    public static final String DEFAULT_MULTICAST_ADDRESS = "-u";

    public static final String ADMIN_ONLY = "--admin-only";

    public static final String MASTER_ADDRESS = "--master-address";
    public static final String MASTER_PORT = "--master-port";

    /** Additional class path items, used only by app client*/
    public static final String APPCLIENT_CONFIG = "--appclient-config";
    public static final String SHORT_HOST = "-H";
    public static final String HOST = "--host";
    public static final String CONNECTION_PROPERTIES = "--ejb-client-properties";


    private CommandLineConstants() {
    }
}
