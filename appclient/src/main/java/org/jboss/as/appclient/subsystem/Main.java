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
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import javax.xml.namespace.QName;

import org.jboss.as.appclient.logging.AppClientLogger;
import org.jboss.as.appclient.subsystem.parsing.AppClientXml;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.server.Bootstrap;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.SystemExiter;
import org.jboss.as.version.ProductConfig;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.stdio.LoggingOutputStream;
import org.jboss.stdio.NullInputStream;
import org.jboss.stdio.SimpleStdioContextSelector;
import org.jboss.stdio.StdioContext;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * The application client entry point
 *
 * @author Stuart Douglas
 */
public final class Main {
    // Capture System.out and System.err before they are redirected by STDIO
    private static final PrintStream STDOUT = System.out;
    private static final PrintStream STDERR = System.err;


    private static void usage() {
        CommandLineArgumentUsageImpl.printUsage(STDOUT);
    }

    private Main() {
    }

    /**
     * The main method.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {

        if (java.util.logging.LogManager.getLogManager().getClass().getName().equals("org.jboss.logmanager.LogManager")) {
            // Make sure our original stdio is properly captured.
            try {
                Class.forName(org.jboss.logmanager.handlers.ConsoleHandler.class.getName(), true, org.jboss.logmanager.handlers.ConsoleHandler.class.getClassLoader());
            } catch (Throwable ignored) {
            }
            // Install JBoss Stdio to avoid any nasty crosstalk, after command line arguments are processed.
            StdioContext.install();
            final StdioContext context = StdioContext.create(
                    new NullInputStream(),
                    new LoggingOutputStream(org.jboss.logmanager.Logger.getLogger("stdout"), org.jboss.logmanager.Level.INFO),
                    new LoggingOutputStream(org.jboss.logmanager.Logger.getLogger("stderr"), org.jboss.logmanager.Level.ERROR)
            );
            StdioContext.setStdioContextSelector(new SimpleStdioContextSelector(context));
        }

        try {
            Module.registerURLStreamHandlerFactoryModule(Module.getBootModuleLoader().loadModule(ModuleIdentifier.create("org.jboss.vfs")));

            final ParsedOptions options = determineEnvironment(args, new Properties(WildFlySecurityManager.getSystemPropertiesPrivileged()), WildFlySecurityManager.getSystemEnvironmentPrivileged(), ServerEnvironment.LaunchType.APPCLIENT);
            if(options == null) {
                //this happens if --version was specified
                return;
            }
            ServerEnvironment serverEnvironment = options.environment;
            final List<String> clientArgs = options.clientArguments;

            if (clientArgs.isEmpty()) {
                STDERR.println(AppClientLogger.ROOT_LOGGER.appClientNotSpecified());
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
                    throw AppClientLogger.ROOT_LOGGER.cannotFindAppClientFile(realFile.getAbsoluteFile());
                }

                final Bootstrap bootstrap = Bootstrap.Factory.newInstance();
                final Bootstrap.Configuration configuration = new Bootstrap.Configuration(serverEnvironment);
                configuration.setModuleLoader(Module.getBootModuleLoader());
                final ExtensionRegistry extensionRegistry = configuration.getExtensionRegistry();
                final AppClientXml parser = new AppClientXml(Module.getBootModuleLoader(), extensionRegistry);
                final Bootstrap.ConfigurationPersisterFactory configurationPersisterFactory = new Bootstrap.ConfigurationPersisterFactory() {

                    @Override
                    public ExtensibleConfigurationPersister createConfigurationPersister(ServerEnvironment serverEnvironment, ExecutorService executorService) {
                        ApplicationClientConfigurationPersister persister = new ApplicationClientConfigurationPersister(earPath, deploymentName, options.hostUrl,options.propertiesFile, params,
                                serverEnvironment.getServerConfigurationFile().getBootFile(), rootElement, parser);
                        for (Namespace namespace : Namespace.domainValues()) {
                            if (!namespace.equals(Namespace.CURRENT)) {
                                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "server"), parser);
                            }
                        }
                        extensionRegistry.setWriterRegistry(persister);
                        return persister;
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
                t.printStackTrace(STDERR);
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
        boolean hostSet = false;

        for (int i = 0; i < argsLength; i++) {
            final String arg = args[i];
            try {
                if (clientArgs) {
                    clientArguments.add(arg);
                } else if (CommandLineConstants.VERSION.equals(arg) || CommandLineConstants.SHORT_VERSION.equals(arg)
                        || CommandLineConstants.OLD_VERSION.equals(arg) || CommandLineConstants.OLD_SHORT_VERSION.equals(arg)) {
                    productConfig = new ProductConfig(Module.getBootModuleLoader(), WildFlySecurityManager.getPropertyPrivileged(ServerEnvironment.HOME_DIR, null), null);
                    STDOUT.println(productConfig.getPrettyVersionString());
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
                    if(ret.propertiesFile != null) {
                        throw AppClientLogger.ROOT_LOGGER.cannotSpecifyBothHostAndPropertiesFile();
                    }
                    hostSet = true;
                    String urlSpec = args[++i];
                    ret.hostUrl = urlSpec;
                } else if (arg.startsWith(CommandLineConstants.SHORT_HOST)) {
                    if(ret.propertiesFile != null) {
                        throw AppClientLogger.ROOT_LOGGER.cannotSpecifyBothHostAndPropertiesFile();
                    }
                    hostSet = true;
                    String urlSpec = parseValue(arg, CommandLineConstants.SHORT_HOST);
                    ret.hostUrl = urlSpec;
                } else if (arg.startsWith(CommandLineConstants.HOST)) {
                    if(ret.propertiesFile != null) {
                        throw AppClientLogger.ROOT_LOGGER.cannotSpecifyBothHostAndPropertiesFile();
                    }
                    hostSet = true;
                    String urlSpec = parseValue(arg, CommandLineConstants.HOST);
                    ret.hostUrl = urlSpec;
                } else if (arg.startsWith(CommandLineConstants.CONNECTION_PROPERTIES)) {
                    if(hostSet) {
                        throw AppClientLogger.ROOT_LOGGER.cannotSpecifyBothHostAndPropertiesFile();
                    }
                    String fileUrl = parseValue(arg, CommandLineConstants.CONNECTION_PROPERTIES);
                    ret.propertiesFile = fileUrl;
                }else if (arg.startsWith(CommandLineConstants.SYS_PROP)) {

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
                    WildFlySecurityManager.setPropertyPrivileged(name, value);
                } else if (arg.startsWith(CommandLineConstants.APPCLIENT_CONFIG)) {
                    appClientConfig = parseValue(arg, CommandLineConstants.APPCLIENT_CONFIG);
                } else if (CommandLineConstants.SECMGR.equals(arg)) {
                    // ignore the argument as it's allowed, but passed to jboss-modules and not used here
                } else {
                    if (arg.startsWith("-")) {
                        STDOUT.println(AppClientLogger.ROOT_LOGGER.unknownOption(arg));
                        usage();

                        return null;
                    }
                    clientArgs = true;
                    clientArguments.add(arg);
                }
            } catch (IndexOutOfBoundsException e) {
                STDERR.println(AppClientLogger.ROOT_LOGGER.argumentExpected(arg));
                usage();
                return null;
            }
        }

        String hostControllerName = null; // No host controller unless in domain mode.
        productConfig = new ProductConfig(Module.getBootModuleLoader(), WildFlySecurityManager.getPropertyPrivileged(ServerEnvironment.HOME_DIR, null), systemProperties);
        ret.environment = new ServerEnvironment(hostControllerName, systemProperties, systemEnvironment, appClientConfig, null, launchType, null, productConfig);
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
            STDERR.println(AppClientLogger.ROOT_LOGGER.malformedUrl(arg));
            usage();
            return false;
        } catch (IOException e) {
            STDERR.println(AppClientLogger.ROOT_LOGGER.cannotLoadProperties(url));
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
            // make sure we have an absolute & canonical file url
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
        // by default we use http upgrade support of the Undertow web server
        String hostUrl = "http-remoting://localhost:8080";
        String propertiesFile;
    }
}
