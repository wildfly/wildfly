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

package org.jboss.as.server;

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
import org.jboss.as.process.SystemExiter;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.log4j.BridgeRepositorySelector;
import org.jboss.stdio.LoggingOutputStream;
import org.jboss.stdio.NullInputStream;
import org.jboss.stdio.SimpleStdioContextSelector;
import org.jboss.stdio.StdioContext;

/**
 * The main-class entry point for standalone server instances.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author John Bailey
 */
public final class Main {

    /**
     * The main method.
     *
     * @param args the command-line arguments
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

        // TODO: privileged block
        System.setProperty("log4j.defaultInitOverride", "true");
        new BridgeRepositorySelector().start();

        create(args, in, out, err);
    }

    private static AbstractServer create(String[] args, InputStream stdin, PrintStream stdout, PrintStream stderr) {
        Main main = new Main();
        return main.boot(args, stdin, stdout, stderr);
    }

    // TODO: privileged block
    Properties props = new Properties(System.getProperties());

    private Main() {
    }

    private AbstractServer boot(final String[] args, InputStream stdin, PrintStream stdout, PrintStream stderr) {
        AbstractServer server = null;
        try {
            ServerEnvironment config = determineEnvironment(args, props, stdin, stdout, stderr);
            if (config == null) {
                abort(null);
            } else {
                if(config.isStandalone()) {
                    server = new StandaloneServer(config);
                } else {
                    server = new Server(config);
                }
                // Start the server.
                server.start();
                return server;
            }
        } catch (Throwable t) {
            abort(t);
        }
        return null;
    }

    private void abort(Throwable t) {
        if (t != null) {
            t.printStackTrace(System.err);
        }
        try {
            // Inform the process manager that we are shutting down on purpose
            // so it doesn't try to respawn us
            // FIXME implement shutdown()
            throw new UnsupportedOperationException("implement me");
        } finally {
            SystemExiter.exit(1);
        }
    }

    public static ServerEnvironment determineEnvironment(String[] args, Properties systemProperties, InputStream stdin, PrintStream stdout, PrintStream stderr) {
        Integer pmPort = null;
        InetAddress pmAddress = null;
        String procName = null;
        Integer smPort = null;
        InetAddress smAddress = null;
        boolean standalone = false;
        final int argsLength = args.length;
        for (int i = 0; i < argsLength; i++) {
            final String arg = args[i];
            try {
                if (CommandLineConstants.PROPERTIES.equals(arg) || "-P".equals(arg)) {
                    // Set system properties from url/file
                    URL url = null;
                    try {
                        url = makeURL(args[++i]);
                        Properties props = System.getProperties();
                        props.load(url.openConnection().getInputStream());
                    } catch (MalformedURLException e) {
                        System.err.printf("Malformed URL provided for option %s\n", arg);
                        return null;
                    } catch (IOException e) {
                        System.err.printf("Unable to load properties from URL %s\n", url);
                        return null;
                    }
                } else if (CommandLineConstants.INTERPROCESS_PM_PORT.equals(arg)) {
                    try {
                        pmPort = Integer.valueOf(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.printf("Value for %s is not an Integer -- %s\n", CommandLineConstants.INTERPROCESS_PM_PORT, args[i]);
                        return null;
                    }
                } else if (CommandLineConstants.INTERPROCESS_PM_ADDRESS.equals(arg)) {
                    try {
                        pmAddress = InetAddress.getByName(args[++i]);
                    } catch (UnknownHostException e) {
                        System.err.printf("Value for %s is not a known host -- %s\n", CommandLineConstants.INTERPROCESS_PM_ADDRESS, args[i]);
                        return null;
                    }
                } else if (CommandLineConstants.INTERPROCESS_NAME.equals(arg)){
                    procName = args[++i];
                } else if (CommandLineConstants.INTERPROCESS_SM_PORT.equals(arg)) {
                    try {
                        smPort = Integer.valueOf(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.printf("Value for %s is not an Integer -- %s\n", CommandLineConstants.INTERPROCESS_SM_PORT, args[i]);
                        return null;
                    }
                } else if (CommandLineConstants.INTERPROCESS_SM_ADDRESS.equals(arg)) {
                    try {
                        smAddress = InetAddress.getByName(args[++i]);
                    } catch (UnknownHostException e) {
                        System.err.printf("Value for %s is not a known host -- %s\n", CommandLineConstants.INTERPROCESS_SM_ADDRESS, args[i]);
                        return null;
                    }
                }else if (arg.equals(CommandLineConstants.STANDALONE)) {
                    // Start in standalone mode
                    standalone = true;
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
                    return null;
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.printf("Argument expected for option %s\n", arg);
                return null;
            }
        }

        return new ServerEnvironment(systemProperties, stdin, stdout, stderr, procName, pmAddress, pmPort, smAddress, smPort, standalone);
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
