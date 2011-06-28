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

    /** The ProcessController address */
    public static final String INTERPROCESS_PC_ADDRESS = "--pc-address";

    /** The ProcessController port */
    public static final String INTERPROCESS_PC_PORT = "--pc-port";

    /** The name of a process started by the process controller */
    public static final String INTERPROCESS_NAME = "--interprocess-name";

    /** The HostController address */
    public static final String OLD_INTERPROCESS_HC_ADDRESS = "-interprocess-hc-address";
    public static final String INTERPROCESS_HC_ADDRESS = "--interprocess-hc-address";

    /** The HostController port */
    public static final String OLD_INTERPROCESS_HC_PORT = "-interprocess-hc-port";
    public static final String INTERPROCESS_HC_PORT = "--interprocess-hc-port";

    /** Get the version of the server */
    public static final String OLD_VERSION = "-version";
    public static final String VERSION = "--version";
    public static final String SHORT_VERSION = "-V";

    /** Configure the file to be used to read properties */
    public static final String OLD_PROPERTIES = "-properties";
    public static final String PROPERTIES = "--properties";
    public static final String SHORT_PROPERTIES = "-P";

    /** Configure a default jvm */
    public static final String OLD_DEFAULT_JVM = "-default-jvm";
    public static final String DEFAULT_JVM = "--default-jvm";

    /** Passed in when the host controller is respawned by process controller */
    public static final String RESTART_HOST_CONTROLLER = "--restarted-host-controller";

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

    /** Passed in to a HC to choose the domain.xml file */
    public static final String OLD_HOST_CONFIG = "-host-config";
    public static final String HOST_CONFIG = "--host-config";

    /** Passed in to a standalone instance to choose the standalone.xml file */
    public static final String OLD_SERVER_CONFIG = "-server-config";
    public static final String SERVER_CONFIG = "--server-config";

    /** Address on which the process controller listens */
    public static final String OLD_BIND_ADDR = "-bind-addr";
    public static final String BIND_ADDR = "--bind-addr";

    /** Port on which the process controller listens */
    public static final String OLD_BIND_PORT = "-bind-port";
    public static final String BIND_PORT = "--bind-port";

    private CommandLineConstants() {
    }
}
