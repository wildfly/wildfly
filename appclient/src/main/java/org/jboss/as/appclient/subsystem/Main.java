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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import javax.xml.namespace.QName;

import org.jboss.as.appclient.subsystem.parsing.AppClientXml;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.server.Bootstrap;
import org.jboss.as.version.ProductConfig;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.SystemExiter;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.log4j.BridgeRepositorySelector;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.stdio.StdioContext;

import static org.jboss.as.appclient.logging.AppClientMessages.MESSAGES;

/**
 * The application client entry point
 *
 * @author Stuart Douglas
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

        try {
            Module.registerURLStreamHandlerFactoryModule(Module.getBootModuleLoader().loadModule(ModuleIdentifier.create("org.jboss.vfs")));

            final ParsedOptions options = determineEnvironment(args, new Properties(SecurityActions.getSystemProperties()), SecurityActions.getSystemEnvironment(), ServerEnvironment.LaunchType.APPCLIENT);
            ServerEnvironment serverEnvironment = options.environment;
            final List<String> clientArgs = options.clientArguments;

            if (clientArgs.isEmpty()) {
                System.err.println(MESSAGES.appClientNotSpecified());
                usage();
                abort(null);
            } else {

                final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "server");
                final String file = clientArgs.get(0);
                final List<String> params = clientArgs.subList(1, clientArgs.size());
                final String deploymentName;
                final String earPath;

                int pos = file.lastIndexOf("#");
                if (pos == -1) {
                    earPath = file;
                    deploymentName = null;
                } else {
                    deploymentName = file.substring(pos + 1);
                    earPath = file.substring(0, pos);
                }

                File realFile = new File(earPath);

                if (!realFile.exists()) {
                    throw MESSAGES.cannotFindAppClient(realFile.getAbsoluteFile());
                }

                final AppClientXml parser = new AppClientXml(Module.getBootModuleLoader());
                final Bootstrap bootstrap = Bootstrap.Factory.newInstance();
                final Bootstrap.Configuration configuration = new Bootstrap.Configuration();
                configuration.setServerEnvironment(serverEnvironment);
                configuration.setModuleLoader(Module.getBootModuleLoader());
                final Bootstrap.ConfigurationPersisterFactory configurationPersisterFactory = new Bootstrap.ConfigurationPersisterFactory() {

                    @Override
                    public ExtensibleConfigurationPersister createConfigurationPersister(ServerEnvironment serverEnvironment, ExecutorService executorService) {
                        return new ApplicationClientConfigurationPersister(earPath, deploymentName, options.hostUrl, params,
                                serverEnvironment.getServerConfigurationFile().getBootFile(), rootElement, parser);
                    }
                };
                configuration.setConfigurationPersisterFactory(configurationPersisterFactory);
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

    public static ParsedOptions determineEnvironment(String[] args, Properties systemProperties, Map<String, String> systemEnvironment, ServerEnvironment.LaunchType launchType) {
        List<String> clientArguments = new ArrayList<String>();
        ParsedOptions ret = new ParsedOptions();
        ret.clientArguments = clientArguments;
        final int argsLength = args.length;
        String appClientConfig = "appclient.xml";
        boolean clientArgs = false;
        ProductConfig productConfig;

        for (int i = 0; i < argsLength; i++) {
            final String arg = args[i];
            try {
                if (clientArgs) {
                    clientArguments.add(arg);
                } else if (CommandLineConstants.VERSION.equals(arg) || CommandLineConstants.SHORT_VERSION.equals(arg)
                        || CommandLineConstants.OLD_VERSION.equals(arg) || CommandLineConstants.OLD_SHORT_VERSION.equals(arg)) {
                    productConfig = new ProductConfig(Module.getBootModuleLoader(), SecurityActions.getSystemProperty(ServerEnvironment.HOME_DIR));
                    System.out.println(productConfig.getPrettyVersionString());
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
                } else if (arg.equals(CommandLineConstants.SHORT_HOST) || arg.equals(CommandLineConstants.HOST)) {
                    String urlSpec = args[++i];
                    ret.hostUrl = urlSpec;
                } else if (arg.startsWith(CommandLineConstants.SHORT_HOST)) {
                    String urlSpec = parseValue(arg, CommandLineConstants.SHORT_HOST);
                    ret.hostUrl = urlSpec;
                } else if (arg.startsWith(CommandLineConstants.HOST)) {
                    String urlSpec = parseValue(arg, CommandLineConstants.HOST);
                    ret.hostUrl = urlSpec;
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
                } else if (arg.startsWith(CommandLineConstants.APPCLIENT_CONFIG)) {
                    appClientConfig = parseValue(arg, CommandLineConstants.APPCLIENT_CONFIG);
                } else {
                    if (arg.startsWith("-")) {
                        System.out.println(MESSAGES.unknownOption(arg));
                        usage();

                        return null;
                    }
                    clientArgs = true;
                    clientArguments.add(arg);
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.println(MESSAGES.argumentExpected(arg));
                usage();
                return null;
            }
        }

        String hostControllerName = null; // No host controller unless in domain mode.
        productConfig = new ProductConfig(Module.getBootModuleLoader(), SecurityActions.getSystemProperty(ServerEnvironment.HOME_DIR));
        ret.environment = new ServerEnvironment(hostControllerName, systemProperties, systemEnvironment, appClientConfig, launchType, null, productConfig);
        return ret;
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
            System.err.println(MESSAGES.malformedUrl(arg));
            usage();
            return false;
        } catch (IOException e) {
            System.err.println(MESSAGES.cannotLoadProperties(url));
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

    private static final class ParsedOptions {
        ServerEnvironment environment;
        List<String> clientArguments;
        String hostUrl = "remote://localhost:4447";
    }
}
