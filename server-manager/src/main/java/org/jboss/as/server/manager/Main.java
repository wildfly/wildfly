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
        System.out.println("    -D<name>[=<value>]              Set a system property");
        System.out.println("    -help                           Display this message and exit");
        System.out.println("    -interprocess-address <address> address of socket on which this process should listen for communication from child processes");
        System.out.println("    -interprocess-port <port>       port of socket on which this process should listen for communication from child processes");
        System.out.println("    -P  <url>                       Load system properties from the given url");
        System.out.println("    -properties <url>               Load system properties from the given url");
        System.out.println("    -version                        Print version and exit\n");
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

        Main main = new Main();
        main.boot(args, in, out, err);
    }

    Properties props = new Properties(System.getProperties());

    private Main() {
    }

    private void boot(String[] args, InputStream stdin, PrintStream stdout, PrintStream stderr) {

        ServerManager sm = null;
        try {
            ServerManagerEnvironment config = determineEnvironment(args, stdin, stdout, stderr);
            if (config == null) {
                abort(null);
                return;
            } else {
                sm = new ServerManager(config);
                sm.start();
            }
        } catch (Throwable t) {
            t.printStackTrace(stderr);
            abort(t);
            return;
        }
        
        // We are now past the point where a failure should result in a
        // shutdown() call; i.e. the ServerManager should be running and
        // capable of handling external input
        try {
            sm.startServers();
        }
        catch (RuntimeException e) {
            e.printStackTrace(stderr);
            throw e;
        }
        catch (Error e) {
            e.printStackTrace(stderr);
            throw e;
        }
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
            System.exit(1);
        }
    }

    private ServerManagerEnvironment determineEnvironment(String[] args, InputStream stdin, PrintStream stdout, PrintStream stderr) {
        Integer pmPort = null;
        InetAddress pmAddress = null;

        final int argsLength = args.length;
        for (int i = 0; i < argsLength; i++) {
            final String arg = args[i];
            stderr.println(arg);
            try {
                if ("-version".equals(arg)) {
                    System.out.println("JBoss Application Server " + getVersionString());
                    return null;
                } else if ("-help".equals(arg)) {
                    usage();
                    return null;
                } else if ("-properties".equals(arg) || "-P".equals(arg)) {
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
                } else if ("-interprocess-port".equals(arg)) {
                    try {
                        pmPort = Integer.valueOf(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.printf("Value for -interprocess-port is not an Integer -- %s\n", args[i]);
                        usage();
                        return null;
                    }
                } else if ("-interprocess-address".equals(arg)) {
                    try {
                        pmAddress = InetAddress.getByName(args[++i]);
                    } catch (UnknownHostException e) {
                        System.err.printf("Value for -interprocess-address is not a known host -- %s\n", args[i]);
                        usage();
                        return null;
                    }
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

        return new ServerManagerEnvironment(props, stdin, stdout, stderr, pmAddress, pmPort);
    }

    private URL makeURL(String urlspec) throws MalformedURLException {
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
