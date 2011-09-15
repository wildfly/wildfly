/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.appclient.subsystem;

import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.server.Bootstrap;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.SystemExiter;
import org.jboss.logmanager.log4j.BridgeRepositorySelector;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.stdio.StdioContext;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.jboss.as.process.Main.getVersionString;

/**
 * The application client entry point
 *
 * @author Stuart Douglas
 */
public final class Main {

    public static void usage() {
        System.out.println("Usage: ./appclient.sh [args...] myear.ear#appClient.jar [client args...]\n");
        System.out.println("where args include:");
        System.out.println("    -D<name>[=<value>]                 Set a system property");
        System.out.println("    -h                                 Display this message and exit");
        System.out.println("    --help                             Display this message and exit");
        System.out.println("    -P=<url>                           Load system properties from the given url");
        System.out.println("    -P <url>                           Load system properties from the given url");
        System.out.println("    --properties=<url>                 Load system properties from the given url");
        System.out.println("    -V                                 Print version and exit");
        System.out.println("    -v                                 Print version and exit");
        System.out.println("    --version                          Print version and exit");
        System.out.println();
    }

    private Main() {
    }

    /**
     * The main method.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        SecurityActions.setSystemProperty("log4j.defaultInitOverride", "true");
        new BridgeRepositorySelector().start();

        // Install JBoss Stdio to avoid any nasty crosstalk.
        StdioContext.install();

        try {
            Module.registerURLStreamHandlerFactoryModule(Module.getBootModuleLoader().loadModule(ModuleIdentifier.create("org.jboss.vfs")));
            final List<String> clientArgs = new ArrayList<String>();
            ServerEnvironment serverEnvironment = determineEnvironment(args, new Properties(SecurityActions.getSystemProperties()), SecurityActions.getSystemEnvironment(), ServerEnvironment.LaunchType.APPCLIENT, clientArgs);
            if (serverEnvironment == null) {
                abort(null);
            } else if(clientArgs.isEmpty()) {
                System.err.println("You must specify the application client to execute");
                usage();
                abort(null);
            } else {

                final String file = clientArgs.get(0);
                final List<String> params = clientArgs.subList(1, clientArgs.size());
                final String deploymentName;
                final String earPath;

                int pos = file.lastIndexOf("#");
                if(pos == -1) {
                    earPath = file;
                    deploymentName = null;
                } else {
                    deploymentName = file.substring(pos + 1);
                    earPath = file.substring(0, pos);
                }

                File realFile = new File(earPath);

                if(!realFile.exists()) {
                    throw new RuntimeException("Could not locate app client deployment " + realFile.getAbsolutePath());
                }

                final Bootstrap bootstrap = Bootstrap.Factory.newInstance();
                final Bootstrap.Configuration configuration = new Bootstrap.Configuration();
                configuration.setServerEnvironment(serverEnvironment);
                configuration.setModuleLoader(Module.getBootModuleLoader());
                configuration.setConfigurationPersister(new ApplicationClientConfigurationPersister(earPath, deploymentName, params));
                bootstrap.bootstrap(configuration, Collections.<ServiceActivator>emptyList()).get();
            }
        } catch (Throwable t) {
            abort(t);
        }
    }

    private static void abort(Throwable t) {
        try {
            if (t != null) {
                t.printStackTrace(System.err);
            }
        } finally {
            SystemExiter.exit(1);
        }
    }

    public static ServerEnvironment determineEnvironment(String[] args, Properties systemProperties, Map<String, String> systemEnvironment, ServerEnvironment.LaunchType launchType, List<String> clientArguments) {
        final int argsLength = args.length;
        String serverConfig = null;
        boolean clientArgs = false;
        for (int i = 0; i < argsLength; i++) {
            final String arg = args[i];
            try {
                if (clientArgs) {
                    clientArguments.add(arg);
                } else if (CommandLineConstants.VERSION.equals(arg) || CommandLineConstants.SHORT_VERSION.equals(arg)
                        || CommandLineConstants.OLD_VERSION.equals(arg) || CommandLineConstants.OLD_SHORT_VERSION.equals(arg)) {
                    System.out.println("JBoss Application Server " + getVersionString());
                    return null;
                } else if (CommandLineConstants.HELP.equals(arg) || CommandLineConstants.SHORT_HELP.equals(arg) || CommandLineConstants.OLD_HELP.equals(arg)) {
                    usage();
                    return null;
                } else if (CommandLineConstants.PROPERTIES.equals(arg) || CommandLineConstants.OLD_PROPERTIES.equals(arg)
                        || CommandLineConstants.SHORT_PROPERTIES.equals(arg)) {
                    // Set system properties from url/file
                    if (!processProperties(arg, args[++i])) {
                        return null;
                    }
                } else if (arg.startsWith(CommandLineConstants.PROPERTIES)) {
                    String urlSpec = parseValue(arg, CommandLineConstants.PROPERTIES);
                    if (urlSpec == null || !processProperties(arg, urlSpec)) {
                        return null;
                    }
                } else if (arg.startsWith(CommandLineConstants.SHORT_PROPERTIES)) {
                    String urlSpec = parseValue(arg, CommandLineConstants.SHORT_PROPERTIES);
                    if (urlSpec == null || !processProperties(arg, urlSpec)) {
                        return null;
                    }
                } else if (arg.startsWith(CommandLineConstants.OLD_PROPERTIES)) {
                    String urlSpec = parseValue(arg, CommandLineConstants.OLD_PROPERTIES);
                    if (urlSpec == null || !processProperties(arg, urlSpec)) {
                        return null;
                    }
                } else if (arg.startsWith(CommandLineConstants.SYS_PROP)) {

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
                    systemProperties.setProperty(name, value);
                    SecurityActions.setSystemProperty(name, value);
                } else {
                    clientArgs = true;
                    clientArguments.add(arg);
                }
            } catch (IndexOutOfBoundsException
                    e) {
                System.err.printf("Argument expected for option %s\n", arg);
                usage();
                return null;
            }
        }

        return new ServerEnvironment(systemProperties, systemEnvironment, serverConfig, launchType);
    }

    private static String parseValue(final String arg, final String key) {
        String value = null;
        int splitPos = key.length();
        if (arg.length() <= splitPos + 1 || arg.charAt(splitPos) != '=') {
            usage();
        } else {
            value = arg.substring(splitPos + 1);
        }
        return value;
    }

    private static boolean processProperties(final String arg, final String urlSpec) {
        URL url = null;
        try {
            url = makeURL(urlSpec);
            Properties props = System.getProperties();
            props.load(url.openConnection().getInputStream());
            return true;
        } catch (MalformedURLException e) {
            System.err.printf("Malformed URL provided for option %s\n", arg);
            usage();
            return false;
        } catch (IOException e) {
            System.err.printf("Unable to load properties from URL %s\n", url);
            usage();
            return false;
        }
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
