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

package org.jboss.as.server;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.version.ProductConfig;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.log4j.BridgeRepositorySelector;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.stdio.LoggingOutputStream;
import org.jboss.stdio.NullInputStream;
import org.jboss.stdio.SimpleStdioContextSelector;
import org.jboss.stdio.StdioContext;

/**
 * The main-class entry point for standalone server instances.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author John Bailey
 * @author Brian Stansberry
 * @author Anil Saldhana
 */
public final class Main {

    public static void usage() {
        CommandLineArgument.printUsage(System.out);
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

        // Make sure our original stdio is properly captured.
        try {
            Class.forName(ConsoleHandler.class.getName(), true, ConsoleHandler.class.getClassLoader());
        } catch (Throwable ignored) {
        }

        // Install JBoss Stdio to avoid any nasty crosstalk.
        StdioContext.install();
        final StdioContext context = StdioContext.create(
            new NullInputStream(),
            new LoggingOutputStream(Logger.getLogger("stdout"), Level.INFO),
            new LoggingOutputStream(Logger.getLogger("stderr"), Level.ERROR)
        );
        StdioContext.setStdioContextSelector(new SimpleStdioContextSelector(context));

        try {
            Module.registerURLStreamHandlerFactoryModule(Module.getBootModuleLoader().loadModule(ModuleIdentifier.create("org.jboss.vfs")));
            ServerEnvironment serverEnvironment = determineEnvironment(args, new Properties(SecurityActions.getSystemProperties()), SecurityActions.getSystemEnvironment(), ServerEnvironment.LaunchType.STANDALONE);
            if (serverEnvironment == null) {
                abort(null);
            } else {
                final Bootstrap bootstrap = Bootstrap.Factory.newInstance();
                final Bootstrap.Configuration configuration = new Bootstrap.Configuration(serverEnvironment);
                configuration.setModuleLoader(Module.getBootModuleLoader());
                bootstrap.bootstrap(configuration, Collections.<ServiceActivator>emptyList()).get();
                return;
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

    public static ServerEnvironment determineEnvironment(String[] args, Properties systemProperties, Map<String, String> systemEnvironment, ServerEnvironment.LaunchType launchType) {
        final int argsLength = args.length;
        String serverConfig = null;
        RunningMode runningMode = RunningMode.NORMAL;
        ProductConfig productConfig;
        for (int i = 0; i < argsLength; i++) {
            final String arg = args[i];
            try {
                if (CommandLineConstants.VERSION.equals(arg) || CommandLineConstants.SHORT_VERSION.equals(arg)
                        || CommandLineConstants.OLD_VERSION.equals(arg) || CommandLineConstants.OLD_SHORT_VERSION.equals(arg)) {
                    productConfig = new ProductConfig(Module.getBootModuleLoader(), SecurityActions.getSystemProperty(ServerEnvironment.HOME_DIR));
                    System.out.println(productConfig.getPrettyVersionString());
                    return null;
                } else if (CommandLineConstants.HELP.equals(arg) || CommandLineConstants.SHORT_HELP.equals(arg) || CommandLineConstants.OLD_HELP.equals(arg)) {
                    usage();
                    return null;
                } else if (CommandLineConstants.SERVER_CONFIG.equals(arg) || CommandLineConstants.SHORT_SERVER_CONFIG.equals(arg)
                        || CommandLineConstants.OLD_SERVER_CONFIG.equals(arg)) {
                    serverConfig = args[++i];
                } else if (arg.startsWith(CommandLineConstants.SERVER_CONFIG)) {
                    serverConfig = parseValue(arg, CommandLineConstants.SERVER_CONFIG);
                    if (serverConfig == null) {
                        return null;
                    }
                } else if (arg.startsWith(CommandLineConstants.SHORT_SERVER_CONFIG)) {
                    serverConfig = parseValue(arg, CommandLineConstants.SHORT_SERVER_CONFIG);
                    if (serverConfig == null) {
                        return null;
                    }
                } else if (arg.startsWith(CommandLineConstants.OLD_SERVER_CONFIG)) {
                    serverConfig = parseValue(arg, CommandLineConstants.OLD_SERVER_CONFIG);
                    if (serverConfig == null) {
                        return null;
                    }
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
                }  else if (arg.startsWith(CommandLineConstants.OLD_PROPERTIES)) {
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
                } else if (arg.startsWith(CommandLineConstants.PUBLIC_BIND_ADDRESS)) {

                    int idx = arg.indexOf('=');
                    if (idx == arg.length() - 1) {
                        System.err.printf(ServerMessages.MESSAGES.noArgValue(arg));
                        usage();
                        return null;
                    }
                    String value = idx > -1 ? arg.substring(idx + 1) : args[++i];

                    String propertyName = null;
                    if (idx < 0) {
                        // -b xxx -bmanagement xxx
                        propertyName = arg.length() == 2 ? ServerEnvironment.JBOSS_BIND_ADDRESS : ServerEnvironment.JBOSS_BIND_ADDRESS_PREFIX + arg.substring(2);
                    } else if (idx == 2) {
                        // -b=xxx
                        propertyName = ServerEnvironment.JBOSS_BIND_ADDRESS;
                    } else {
                        // -bmanagement=xxx
                        propertyName =  ServerEnvironment.JBOSS_BIND_ADDRESS_PREFIX + arg.substring(2, idx);
                    }
                    systemProperties.setProperty(propertyName, value);
                    SecurityActions.setSystemProperty(propertyName, value);
                } else if (arg.startsWith(CommandLineConstants.DEFAULT_MULTICAST_ADDRESS)) {

                    int idx = arg.indexOf('=');
                    if (idx == arg.length() - 1) {
                        System.err.printf(ServerMessages.MESSAGES.valueExpectedForCommandLineOption(arg));
                        System.err.println();
                        usage();
                        return null;
                    }
                    String value = idx > -1 ? arg.substring(idx + 1) : args[++i];

                    systemProperties.setProperty(ServerEnvironment.JBOSS_DEFAULT_MULTICAST_ADDRESS, value);
                    SecurityActions.setSystemProperty(ServerEnvironment.JBOSS_DEFAULT_MULTICAST_ADDRESS, value);
                } else if (CommandLineConstants.ADMIN_ONLY.equals(arg)) {
                    runningMode = RunningMode.ADMIN_ONLY;
                } else if (arg.startsWith(CommandLineConstants.SECURITY_PROP)) {
                    //Value can be a comma separated key value pair
                    //Drop the first 2 characters
                    String token = arg.substring(2);
                    processSecurityProperties(token);
                } else {
                    System.err.printf(ServerMessages.MESSAGES.invalidCommandLineOption(arg));
                    usage();
                    return null;
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.printf(ServerMessages.MESSAGES.valueExpectedForCommandLineOption(arg));
                usage();
                return null;
            }
        }

        String hostControllerName = null; // No host controller unless in domain mode.
        productConfig = new ProductConfig(Module.getBootModuleLoader(), SecurityActions.getSystemProperty(ServerEnvironment.HOME_DIR));
        return new ServerEnvironment(hostControllerName, systemProperties, systemEnvironment, serverConfig, launchType, runningMode, productConfig);
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
             System.err.printf(ServerMessages.MESSAGES.malformedCommandLineURL(urlSpec, arg));
             System.err.println();
             usage();
             return false;
         } catch (IOException e) {
             System.err.printf(ServerMessages.MESSAGES.unableToLoadProperties(url));
             System.err.println();
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

    private static void processSecurityProperties(String secProperties){
        StringTokenizer tokens = new StringTokenizer(secProperties, ",");
        while(tokens != null && tokens.hasMoreTokens()){
            String token = tokens.nextToken();

            int idx = token.indexOf('=');
            if (idx == token.length() - 1) {
                System.err.printf(ServerMessages.MESSAGES.valueExpectedForCommandLineOption(secProperties));
                System.err.println();
                usage();
                return;
            }
            String value = token.substring(idx + 1);
            String key = token.substring(0, idx);
            SecurityActions.setSecurityProperty(key, value);
        }
    }
}