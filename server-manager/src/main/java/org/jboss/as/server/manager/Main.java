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

package org.jboss.as.server.manager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;

import org.jboss.as.process.CommandLineConstants;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;
import org.jboss.stdio.LoggingOutputStream;
import org.jboss.stdio.NullInputStream;
import org.jboss.stdio.SimpleStdioContextSelector;
import org.jboss.stdio.StdioContext;

/**
 * The main-class entry point for the server manager process.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Main {

    public static String getVersionString() {
        return "TRUNK SNAPSHOT";
    }

    private static void usage() {
        System.out.println("Usage: ./run.sh [args...]\n");
        System.out.println("where args include:");
        System.out.println("    -D<name>[=<value>]                 Set a system property");
        System.out.println("    -help                              Display this message and exit");
        System.out.println("    -interprocess-pm-address <address> Address of process manager socket");
        System.out.println("    -interprocess-pm-port <port>       Port of process manager socket");
        System.out.println("    -interprocess-name <proc>          Name of this process, used to register the socket with the server in the process manager");
        System.out.println("    -interprocess-sm-address <address> Address this server manager's socket should listen on");
        System.out.println("    -interprocess-sm-port <port>       Port of this server manager's socket  should listen on");
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
    public static void main(String[] args) {

        // Grab copies of our streams.
        final InputStream in = System.in;
        final PrintStream out = System.out;
        final PrintStream err = System.err;

        // Install JBoss Stdio to avoid any nasty crosstalk.
        StdioContext.install();
        final StdioContext context = StdioContext.create(
            new NullInputStream(),
            new LoggingOutputStream(Logger.getLogger("stdout"), Level.INFO),
            new LoggingOutputStream(Logger.getLogger("stderr"), Level.ERROR)
        );
        StdioContext.setStdioContextSelector(new SimpleStdioContextSelector(context));

        create(args, in, out, err);
    }

    Properties props = new Properties(System.getProperties());

    private Main() {
    }

    private static ServerManager create(String[] args, InputStream stdin, PrintStream stdout, PrintStream stderr) {
        Main main = new Main();
        return main.boot(args, stdin, stdout, stderr);
    }

    private ServerManager boot(String[] args, InputStream stdin, PrintStream stdout, PrintStream stderr) {
        ServerManager sm = null;
        try {
            ServerManagerEnvironment config = determineEnvironment(args, props, stdin, stdout, stderr);
            if (config == null) {
                abort(null);
                return null;
            } else {
                sm = new ServerManager(config);
                sm.start();
            }
        } catch (Throwable t) {
            t.printStackTrace(stderr);
            abort(t);
            return null;
        }
        return sm;
    }

    private void abort(Throwable t) {
        try {
            // Inform the process manager that we are shutting down on purpose
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

    public static ServerManagerEnvironment determineEnvironment(String[] args, Properties systemProperties, InputStream stdin, PrintStream stdout, PrintStream stderr) {
        Integer pmPort = null;
        InetAddress pmAddress = null;
        Integer smPort = null;
        InetAddress smAddress = null;
        String procName = null;
        String defaultJVM = null;
        boolean isRestart = false;

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
                        Properties props = System.getProperties();
                        props.load(url.openConnection().getInputStream());
                    } catch (MalformedURLException e) {
                        System.err.printf("Malformed URL provided for option %s\n", arg);
                        usage();
                        return null;
                    } catch (IOException e) {
                        System.err.printf("Unable to load properties from URL %s\n", url);
                        usage();
                        return null;
                    }
                } else if (CommandLineConstants.INTERPROCESS_PM_PORT.equals(arg)) {
                    try {
                        pmPort = Integer.valueOf(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.printf("Value for %s is not an Integer -- %s\n", CommandLineConstants.INTERPROCESS_PM_PORT, args[i]);
                        usage();
                        return null;
                    }
                } else if (CommandLineConstants.INTERPROCESS_PM_ADDRESS.equals(arg)) {
                    try {
                        pmAddress = InetAddress.getByName(args[++i]);
                    } catch (UnknownHostException e) {
                        System.err.printf("Value for %s is not a known host -- %s\n", CommandLineConstants.INTERPROCESS_PM_ADDRESS, args[i]);
                        usage();
                        return null;
                    }
                } else if (CommandLineConstants.INTERPROCESS_SM_PORT.equals(arg)) {
                    try {
                        smPort = Integer.valueOf(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.printf("Value for %s is not an Integer -- %s\n", CommandLineConstants.INTERPROCESS_SM_PORT, args[i]);
                        usage();
                        return null;
                    }
                } else if (CommandLineConstants.INTERPROCESS_SM_ADDRESS.equals(arg)) {
                    try {
                        smAddress = InetAddress.getByName(args[++i]);
                    } catch (UnknownHostException e) {
                        System.err.printf("Value for %s is not a known host -- %s\n", CommandLineConstants.INTERPROCESS_SM_ADDRESS, args[i]);
                        usage();
                        return null;
                    }
                } else if (CommandLineConstants.INTERPROCESS_NAME.equals(arg)) {
                    procName = args[++i];
                } else if (CommandLineConstants.RESTART_SERVER_MANAGER.equals(arg)) {
                    isRestart = true;
                } else if(CommandLineConstants.DEFAULT_JVM.equals(arg)) {
                    defaultJVM = args[++i];
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

        return new ServerManagerEnvironment(systemProperties, isRestart,  stdin, stdout, stderr, procName, pmAddress, pmPort, smAddress, smPort, defaultJVM);
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
