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

package org.jboss.as.host.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.protocol.old.StreamUtils;
import org.jboss.as.version.Version;
import org.jboss.logging.MDC;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;
import org.jboss.stdio.LoggingOutputStream;
import org.jboss.stdio.NullInputStream;
import org.jboss.stdio.SimpleStdioContextSelector;
import org.jboss.stdio.StdioContext;

/**
 * The main-class entry point for the host controller process.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:kkhan@redhat.com">Kabir Khan</a>
 * @author Brian Stansberry
 */
public final class Main {

    public static String getVersionString() {
        return Version.AS_VERSION;
    }

    private static void usage() {
        System.out.println("Usage: ./domain.sh [args...]\n");
        System.out.println("where args include:");
        System.out.println("    -backup                            Keep a copy of the persistent domain configuration even if this host is not the Domain Controller");
        System.out.println("    -cached-dc                         If this host is not the Domain Controller and cannot contact the Domain Controller at boot, boot using a locally cached copy of the domain configuration (see -backup)");
        System.out.println("    -D<name>[=<value>]                 Set a system property");
        System.out.println("    -domain-config <config>            Name of the domain configuration file to use (default is \"domain.xml\")");
        System.out.println("    -help                              Display this message and exit");
        System.out.println("    -host-config <config>              Name of the host configuration file to use (default is \"host.xml\")");
        System.out.println("    -pc-address <address>              Address of process controller socket");
        System.out.println("    -pc-port <port>                    Port of process controller socket");
        System.out.println("    -interprocess-name <proc>          Name of this process, used to register the socket with the server in the process controller");
        System.out.println("    -interprocess-hc-address <address> Address this host controller's socket should listen on");
        System.out.println("    -interprocess-hc-port <port>       Port of this host controller's socket  should listen on");
        System.out.println("    -P  <url>                          Load system properties from the given url");
        System.out.println("    -properties <url>                  Load system properties from the given url");
        System.out.println("    -version                           Print version and exit\n");
    }

    /**
     * The main method.
     *
     * @param args
     *            the command-line arguments
     */
    public static void main(String[] args) throws IOException {
        MDC.put("process", "host controller");

        // Grab copies of our streams.
        final InputStream in = System.in;
        final PrintStream out = System.out;
        final PrintStream err = System.err;

        final byte[] authKey = new byte[16];
        try {
            StreamUtils.readFully(System.in, authKey);
        } catch (IOException e) {
            System.err.printf("Failed to read authentication key: %s", e);
            System.exit(1);
            return;
        }

        // Install JBoss Stdio to avoid any nasty crosstalk.
        StdioContext.install();
        final StdioContext context = StdioContext.create(
            new NullInputStream(),
            new LoggingOutputStream(Logger.getLogger("stdout"), Level.INFO),
            new LoggingOutputStream(Logger.getLogger("stderr"), Level.ERROR)
        );
        StdioContext.setStdioContextSelector(new SimpleStdioContextSelector(context));

        create(args, in, out, err, authKey);

        while (in.read() != -1) {}
        System.exit(0);
    }

    private Main() {
    }

    private static HostControllerBootstrap create(String[] args, InputStream stdin, PrintStream stdout, PrintStream stderr, final byte[] authCode) {
        Main main = new Main();
        return main.boot(args, stdin, stdout, stderr, authCode);
    }

    private HostControllerBootstrap boot(String[] args, InputStream stdin, PrintStream stdout, PrintStream stderr, final byte[] authCode) {
        HostControllerBootstrap hc = null;
        try {
            HostControllerEnvironment config = determineEnvironment(args, stdin, stdout, stderr);
            if (config == null) {
                abort(null);
                return null;
            } else {
                hc = new HostControllerBootstrap(config, authCode);
                hc.start();
            }
        } catch (Throwable t) {
            t.printStackTrace(stderr);
            abort(t);
            return null;
        }
        return hc;
    }

    private void abort(Throwable t) {
        try {
            t.printStackTrace();
            // Inform the process controller that we are shutting down on purpose
            // so it doesn't try to respawn us

            // FIXME implement abort()
            throw new UnsupportedOperationException("implement me");

//            if (t != null) {
//                t.printStackTrace(System.err);
//            }

        } finally {
            SystemExiter.exit(1);
        }
    }

    public static HostControllerEnvironment determineEnvironment(String[] args, InputStream stdin, PrintStream stdout, PrintStream stderr) {
        Integer pmPort = null;
        InetAddress pmAddress = null;
        Integer smPort = Integer.valueOf(0);
        InetAddress smAddress = null;
        try {
            smAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        String procName = "Host Controller";
        String defaultJVM = null;
        boolean isRestart = false;
        boolean backupDomainFiles = false;
        boolean cachedDc = false;
        String domainConfig = null;
        String hostConfig = null;
        Map<String, String> hostSystemProperties = new HashMap<String, String>();

        final int argsLength = args.length;
        for (int i = 0; i < argsLength; i++) {
            final String arg = args[i];

            try {
                if (CommandLineConstants.VERSION.equals(arg)) {
                    System.out.println("JBoss Application Server " + getVersionString());
                    return null;
                } else if (CommandLineConstants.HELP.equals(arg)) {
                    usage();
                    return null;
                } else if (CommandLineConstants.PROPERTIES.equals(arg) || "-P".equals(arg)) {
                    // Set system properties from url/file
                    URL url = null;
                    try {
                        url = makeURL(args[++i]);
                        Properties props = new Properties();
                        props.load(url.openConnection().getInputStream());
                        System.getProperties().putAll(props);
                        for (Map.Entry<Object, Object> entry : props.entrySet()) {
                            hostSystemProperties.put(String.class.cast(entry.getKey()), String.class.cast(entry.getValue()));
                        }
                    } catch (MalformedURLException e) {
                        System.err.printf("Malformed URL provided for option %s\n", arg);
                        usage();
                        return null;
                    } catch (IOException e) {
                        System.err.printf("Unable to load properties from URL %s\n", url);
                        usage();
                        return null;
                    }
                } else if (CommandLineConstants.INTERPROCESS_PC_PORT.equals(arg)) {
                    try {
                        pmPort = Integer.valueOf(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.printf("Value for %s is not an Integer -- %s\n", CommandLineConstants.INTERPROCESS_PC_PORT, args[i]);
                        usage();
                        return null;
                    }
                } else if (CommandLineConstants.INTERPROCESS_PC_ADDRESS.equals(arg)) {
                    try {
                        pmAddress = InetAddress.getByName(args[++i]);
                    } catch (UnknownHostException e) {
                        System.err.printf("Value for %s is not a known host -- %s\n", CommandLineConstants.INTERPROCESS_PC_ADDRESS, args[i]);
                        usage();
                        return null;
                    }
                } else if (CommandLineConstants.INTERPROCESS_HC_PORT.equals(arg)) {
                    try {
                        smPort = Integer.valueOf(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.printf("Value for %s is not an Integer -- %s\n", CommandLineConstants.INTERPROCESS_HC_PORT, args[i]);
                        usage();
                        return null;
                    }
                } else if (CommandLineConstants.INTERPROCESS_HC_ADDRESS.equals(arg)) {
                    try {
                        smAddress = InetAddress.getByName(args[++i]);
                    } catch (UnknownHostException e) {
                        System.err.printf("Value for %s is not a known host -- %s\n", CommandLineConstants.INTERPROCESS_HC_ADDRESS, args[i]);
                        usage();
                        return null;
                    }
                } else if (CommandLineConstants.INTERPROCESS_NAME.equals(arg)) {
                    procName = args[++i];
                } else if (CommandLineConstants.RESTART_HOST_CONTROLLER.equals(arg)) {
                    isRestart = true;
                } else if (CommandLineConstants.BACKUP_DC.equals(arg)) {
                    backupDomainFiles = true;
                } else if (CommandLineConstants.CACHED_DC.equals(arg)) {
                    cachedDc = true;
                } else if(CommandLineConstants.DEFAULT_JVM.equals(arg)) {
                    defaultJVM = args[++i];
                } else if (CommandLineConstants.DOMAIN_CONFIG.equals(arg)) {
                    domainConfig = args[++i];
                } else if (CommandLineConstants.HOST_CONFIG.equals(arg)) {
                    hostConfig = args[++i];
                } else if (arg.startsWith("-D")) {

                    // set a system property
                    String name, value;
                    int idx = arg.indexOf("=");
                    if (idx == -1) {
                        name = arg.substring(2);
                        value = "true";
                    } else {
                        name = arg.substring(2, idx);
                        value = arg.substring(idx + 1, arg.length());
                    }
                    System.setProperty(name, value);
                    hostSystemProperties.put(name, value);
                } else {
                    System.err.printf("Invalid option '%s'\n", arg);
                    usage();
                    return null;
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.printf("Argument expected for option %s\n", arg);
                usage();
                return null;
            }
        }

        return new HostControllerEnvironment(hostSystemProperties, isRestart,  stdin, stdout, stderr, procName, pmAddress, pmPort, smAddress, smPort, defaultJVM,
                domainConfig, hostConfig, backupDomainFiles, cachedDc);
    }

    private static URL makeURL(String urlspec) throws MalformedURLException {
        urlspec = urlspec.trim();

        URL url;

        try {
            url = new URL(urlspec);
            if (url.getProtocol().equals("file")) {
                // make sure the file is absolute & canonical file url
                File file = new File(url.getFile()).getCanonicalFile();
                url = file.toURI().toURL();
            }
        } catch (Exception e) {
            // make sure we have a absolute & canonical file url
            try {
                File file = new File(urlspec).getCanonicalFile();
                url = file.toURI().toURL();
            } catch (Exception n) {
                throw new MalformedURLException(n.toString());
            }
        }

        return url;
    }
}
