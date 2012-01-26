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

import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import static org.jboss.as.process.Main.usage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.interfaces.InetAddressUtil;
import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.process.ExitCodes;
import org.jboss.as.process.protocol.StreamUtils;
import org.jboss.as.version.ProductConfig;
import org.jboss.logging.MDC;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.modules.Module;
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
    /**
     * The main method.
     *
     * @param args the command-line arguments
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
            System.err.println(MESSAGES.failedToReadAuthenticationKey(e));
            System.exit(1);
            return;
        }

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
        try {
            final HostControllerEnvironment config = determineEnvironment(args, stdin, stdout, stderr);
            if (config == null) {
                abort(null);
                return null;
            } else {
                try {
                    final HostControllerBootstrap hc = new HostControllerBootstrap(config, authCode);
                    hc.bootstrap();
                    return hc;
                } catch(Throwable t) {
                    abort(t);
                    return null;
                }
            }
        } catch (Throwable t) {
            abort(t, ExitCodes.HOST_CONTROLLER_ABORT_EXIT_CODE);
            return null;
        }
    }

    private void abort(Throwable t) {
        abort(t, 1);
    }

    private void abort(Throwable t, int exitCode) {
        try {
            if (t != null) {
                t.printStackTrace();
            } else {
                // Inform the process controller that we are shutting down on purpose
                // so it doesn't try to respawn us
                exitCode = ExitCodes.HOST_CONTROLLER_ABORT_EXIT_CODE;
            }

        } finally {
            SystemExiter.exit(exitCode);
        }
    }

    public static HostControllerEnvironment determineEnvironment(String[] args, InputStream stdin, PrintStream stdout, PrintStream stderr) {
        Integer pmPort = null;
        InetAddress pmAddress = null;
        final PCSocketConfig pcSocketConfig = new PCSocketConfig();
        String defaultJVM = null;
        boolean isRestart = false;
        boolean backupDomainFiles = false;
        boolean cachedDc = false;
        String domainConfig = null;
        String hostConfig = null;
        RunningMode initialRunningMode = RunningMode.NORMAL;
        Map<String, String> hostSystemProperties = new HashMap<String, String>();
        ProductConfig productConfig;


        final int argsLength = args.length;
        for (int i = 0; i < argsLength; i++) {
            final String arg = args[i];

            try {
                if (CommandLineConstants.VERSION.equals(arg) || CommandLineConstants.SHORT_VERSION.equals(arg)
                        || CommandLineConstants.OLD_VERSION.equals(arg) || CommandLineConstants.OLD_SHORT_VERSION.equals(arg)) {
                    productConfig = new ProductConfig(Module.getBootModuleLoader(), SecurityActions.getSystemProperty(HostControllerEnvironment.HOME_DIR));
                    System.out.println(productConfig.getPrettyVersionString());
                    return null;
                } else if (CommandLineConstants.HELP.equals(arg) || CommandLineConstants.SHORT_HELP.equals(arg) || CommandLineConstants.OLD_HELP.equals(arg)) {
                    usage();
                    return null;
                } else if (CommandLineConstants.PROPERTIES.equals(arg) || CommandLineConstants.OLD_PROPERTIES.equals(arg)
                        || CommandLineConstants.SHORT_PROPERTIES.equals(arg)) {
                    // Set system properties from url/file
                    if (!processProperties(arg, args[++i], hostSystemProperties)) {
                        return null;
                    }
                } else if (arg.startsWith(CommandLineConstants.PROPERTIES)) {
                    String urlSpec = parseValue(arg, CommandLineConstants.PROPERTIES);
                    if (urlSpec == null || !processProperties(arg, urlSpec, hostSystemProperties)) {
                        return null;
                    }
                } else if (arg.startsWith(CommandLineConstants.SHORT_PROPERTIES)) {
                    String urlSpec = parseValue(arg, CommandLineConstants.SHORT_PROPERTIES);
                    if (urlSpec == null || !processProperties(arg, urlSpec, hostSystemProperties)) {
                        return null;
                    }
                }  else if (arg.startsWith(CommandLineConstants.OLD_PROPERTIES)) {
                    String urlSpec = parseValue(arg, CommandLineConstants.OLD_PROPERTIES);
                    if (urlSpec == null || !processProperties(arg, urlSpec, hostSystemProperties)) {
                        return null;
                    }
                } else if (CommandLineConstants.PROCESS_CONTROLLER_BIND_PORT.equals(arg)) {
                    final String port = args[++i];
                    try {
                        pmPort = Integer.valueOf(port);
                    } catch (NumberFormatException e) {
                        System.err.println(MESSAGES.invalidValue(CommandLineConstants.PROCESS_CONTROLLER_BIND_PORT, "Integer", port));
                        usage();
                        return null;
                    }
                } else if (arg.startsWith(CommandLineConstants.PROCESS_CONTROLLER_BIND_PORT)) {
                    String val = parseValue(arg, CommandLineConstants.PROCESS_CONTROLLER_BIND_PORT);
                    if (val == null) {
                        return null;
                    }
                    final Integer port = parsePort(val, CommandLineConstants.PROCESS_CONTROLLER_BIND_PORT);
                    if (port == null) {
                        return null;
                    }
                    pmPort = port;
                } else if (CommandLineConstants.PROCESS_CONTROLLER_BIND_ADDR.equals(arg)) {
                    final String addr = args[++i];
                    try {
                        pmAddress = InetAddress.getByName(addr);
                    } catch (UnknownHostException e) {
                        System.err.println(MESSAGES.unknownHostValue(CommandLineConstants.PROCESS_CONTROLLER_BIND_ADDR, addr));
                        usage();
                        return null;
                    }
                } else if (arg.startsWith(CommandLineConstants.PROCESS_CONTROLLER_BIND_ADDR)) {
                    final String val = parseValue(arg, CommandLineConstants.PROCESS_CONTROLLER_BIND_ADDR);
                    if (val == null) {
                        return null;
                    }
                    final InetAddress addr = parseAddress(val, arg);
                    if (addr == null) {
                        return null;
                    }
                    pmAddress = addr;
                } else if (pcSocketConfig.processPCSocketConfigArgument(arg, args, i)) {
                    if (pcSocketConfig.isParseFailed()) {
                        return null;
                    }
                    i += pcSocketConfig.getArgIncrement();
                } else if (CommandLineConstants.RESTART_HOST_CONTROLLER.equals(arg)) {
                    isRestart = true;
                } else if (CommandLineConstants.BACKUP_DC.equals(arg) || CommandLineConstants.OLD_BACKUP_DC.equals(arg)) {
                    backupDomainFiles = true;
                } else if (CommandLineConstants.CACHED_DC.equals(arg) || CommandLineConstants.OLD_CACHED_DC.equals(arg)) {
                    cachedDc = true;
                } else if(CommandLineConstants.DEFAULT_JVM.equals(arg) || CommandLineConstants.OLD_DEFAULT_JVM.equals(arg)) {
                    defaultJVM = args[++i];
                } else if (CommandLineConstants.DOMAIN_CONFIG.equals(arg)
                        || CommandLineConstants.SHORT_DOMAIN_CONFIG.equals(arg)
                        || CommandLineConstants.OLD_DOMAIN_CONFIG.equals(arg)) {
                    domainConfig = args[++i];
                } else if (arg.startsWith(CommandLineConstants.DOMAIN_CONFIG)) {
                    String val = parseValue(arg, CommandLineConstants.DOMAIN_CONFIG);
                    if (val == null) {
                        return null;
                    }
                    domainConfig = val;
                } else if (arg.startsWith(CommandLineConstants.SHORT_DOMAIN_CONFIG)) {
                    String val = parseValue(arg, CommandLineConstants.SHORT_DOMAIN_CONFIG);
                    if (val == null) {
                        return null;
                    }
                    domainConfig = val;
                } else if (arg.startsWith(CommandLineConstants.OLD_DOMAIN_CONFIG)) {
                    String val = parseValue(arg, CommandLineConstants.OLD_DOMAIN_CONFIG);
                    if (val == null) {
                        return null;
                    }
                    domainConfig = val;
                } else if (CommandLineConstants.HOST_CONFIG.equals(arg) || CommandLineConstants.OLD_HOST_CONFIG.equals(arg)) {
                    hostConfig = args[++i];
                } else if (arg.startsWith(CommandLineConstants.HOST_CONFIG)) {
                    String val = parseValue(arg, CommandLineConstants.HOST_CONFIG);
                    if (val == null) {
                        return null;
                    }
                    hostConfig = val;
                } else if (arg.startsWith(CommandLineConstants.OLD_HOST_CONFIG)) {
                    String val = parseValue(arg, CommandLineConstants.OLD_HOST_CONFIG);
                    if (val == null) {
                        return null;
                    }
                    hostConfig = val;

                } else if (arg.startsWith(CommandLineConstants.MASTER_ADDRESS)) {

                    int idx = arg.indexOf('=');
                    if (idx == arg.length() - 1) {
                        System.err.println(MESSAGES.argumentExpected(arg));
                        usage();
                        return null;
                    }
                    String value = idx > -1 ? arg.substring(idx + 1) : args[++i];

                    hostSystemProperties.put(HostControllerEnvironment.JBOSS_DOMAIN_MASTER_ADDRESS, value);
                    SecurityActions.setSystemProperty(HostControllerEnvironment.JBOSS_DOMAIN_MASTER_ADDRESS, value);

                } else if (arg.startsWith(CommandLineConstants.MASTER_PORT)) {

                    int idx = arg.indexOf('=');
                    if (idx == arg.length() - 1) {
                        System.err.println(MESSAGES.argumentExpected(arg));
                        usage();
                        return null;
                    }
                    String value = idx > -1 ? arg.substring(idx + 1) : args[++i];
                    final Integer port = parsePort(value, CommandLineConstants.MASTER_PORT);
                    if (port == null) {
                        return null;
                    }

                    hostSystemProperties.put(HostControllerEnvironment.JBOSS_DOMAIN_MASTER_PORT, value);
                    SecurityActions.setSystemProperty(HostControllerEnvironment.JBOSS_DOMAIN_MASTER_PORT, value);

                } else if (CommandLineConstants.ADMIN_ONLY.equals(arg)) {
                    initialRunningMode = RunningMode.ADMIN_ONLY;
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
                    SecurityActions.setSystemProperty(name, value);
                    hostSystemProperties.put(name, value);
                } else if (arg.startsWith(CommandLineConstants.PUBLIC_BIND_ADDRESS)) {

                    int idx = arg.indexOf('=');
                    if (idx == arg.length() - 1) {
                        System.err.println(MESSAGES.argumentExpected(arg));
                        usage();
                        return null;
                    }
                    String value = idx > -1 ? arg.substring(idx + 1) : args[++i];

                    String propertyName = null;
                    if (idx < 0) {
                        // -b xxx -bmanagement xxx
                        propertyName = arg.length() == 2 ? HostControllerEnvironment.JBOSS_BIND_ADDRESS : HostControllerEnvironment.JBOSS_BIND_ADDRESS_PREFIX + arg.substring(2);
                    } else if (idx == 2) {
                        // -b=xxx
                        propertyName = HostControllerEnvironment.JBOSS_BIND_ADDRESS;
                    } else {
                        // -bmanagement=xxx
                        propertyName =  HostControllerEnvironment.JBOSS_BIND_ADDRESS_PREFIX + arg.substring(2, idx);
                    }
                    hostSystemProperties.put(propertyName, value);
                    SecurityActions.setSystemProperty(propertyName, value);
                } else if (arg.startsWith(CommandLineConstants.DEFAULT_MULTICAST_ADDRESS)) {

                    int idx = arg.indexOf('=');
                    if (idx == arg.length() - 1) {
                        System.err.println(MESSAGES.argumentExpected(arg));
                        usage();
                        return null;
                    }
                    String value = idx > -1 ? arg.substring(idx + 1) : args[++i];

                    hostSystemProperties.put(HostControllerEnvironment.JBOSS_DEFAULT_MULTICAST_ADDRESS, value);
                    SecurityActions.setSystemProperty(HostControllerEnvironment.JBOSS_DEFAULT_MULTICAST_ADDRESS, value);
                } else {
                    System.err.println(MESSAGES.invalidOption(arg));
                    usage();
                    return null;
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.println(MESSAGES.argumentExpected(arg));
                usage();
                return null;
            }
        }

        productConfig = new ProductConfig(Module.getBootModuleLoader(), SecurityActions.getSystemProperty(HostControllerEnvironment.HOME_DIR));
        return new HostControllerEnvironment(hostSystemProperties, isRestart,  stdin, stdout, stderr, pmAddress, pmPort,
                pcSocketConfig.getBindAddress(), pcSocketConfig.getBindPort(), defaultJVM,
                domainConfig, hostConfig, initialRunningMode, backupDomainFiles, cachedDc, productConfig);
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

    private static boolean processProperties(final String arg, final String urlSpec, Map<String, String> hostSystemProperties) {
         URL url = null;
         try {
             url = makeURL(urlSpec);
             Properties props = new Properties();
             props.load(url.openConnection().getInputStream());

             SecurityActions.getSystemProperties().putAll(props);
             for (Map.Entry<Object, Object> entry : props.entrySet()) {
                 hostSystemProperties.put((String)entry.getKey(), (String)entry.getValue());
             }
             return true;
         } catch (MalformedURLException e) {
             System.err.println(MESSAGES.malformedUrl(arg));
             usage();
             return false;
         } catch (IOException e) {
             System.err.println(MESSAGES.unableToLoadProperties(url));
             usage();
             return false;
         }
    }

    private static Integer parsePort(final String value, final String key) {
         try {
             return Integer.valueOf(value);
         } catch (NumberFormatException e) {
             System.err.println(MESSAGES.invalidValue(key, "Integer", value));
             usage();
             return null;
         }
    }

    private static InetAddress parseAddress(final String value, final String key) {
        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException e) {
            System.err.println(MESSAGES.unknownHostValue(key, value));
            usage();
            return null;
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

    private static class PCSocketConfig {
        private final String defaultBindAddress;
        private InetAddress bindAddress;
        private int bindPort = 0;
        private int argIncrement = 0;
        private boolean parseFailed;
        private final UnknownHostException uhe;

        private PCSocketConfig() {
            boolean preferIPv6 = Boolean.valueOf(SecurityActions.getSystemProperty("java.net.preferIPv6Addresses", "false"));
            this.defaultBindAddress = preferIPv6 ? "::1" : "127.0.0.1";
            UnknownHostException toCache = null;
            try {
                bindAddress = InetAddress.getByName(defaultBindAddress);
            } catch (UnknownHostException e) {
                try {
                    bindAddress = InetAddressUtil.getLocalHost();
                } catch (UnknownHostException uhe) {
                    toCache = uhe;
                }
            }
            uhe = toCache;
        }

        private InetAddress getBindAddress() {
            if (bindAddress == null) {
                throw MESSAGES.cannotObtainValidDefaultAddress(uhe, defaultBindAddress, CommandLineConstants.INTERPROCESS_HC_ADDRESS);
            }
            return bindAddress;
        }

        private int getBindPort() {
            return bindPort;
        }

        private int getArgIncrement() {
            return argIncrement;
        }

        private boolean isParseFailed() {
            return parseFailed;
        }

        private boolean processPCSocketConfigArgument(final String arg, final String[] args, final int index) {
            boolean isPCSocketArg = true;

            argIncrement = 0;

            if (CommandLineConstants.INTERPROCESS_HC_ADDRESS.equals(arg) || CommandLineConstants.OLD_INTERPROCESS_HC_ADDRESS.equals(arg)) {
                setBindAddress(arg, args[index +1]);
                argIncrement = 1;
            } else if (arg.startsWith(CommandLineConstants.INTERPROCESS_HC_ADDRESS)) {
                String addr = parseValue(arg, CommandLineConstants.INTERPROCESS_HC_ADDRESS);
                if (addr == null) {
                    parseFailed = true;
                } else {
                    setBindAddress(arg, addr);
                }
            } else if (arg.startsWith(CommandLineConstants.OLD_INTERPROCESS_HC_ADDRESS)) {
                String addr = parseValue(arg, CommandLineConstants.OLD_INTERPROCESS_HC_ADDRESS);
                if (addr == null) {
                    parseFailed = true;
                } else {
                    setBindAddress(arg, addr);
                }
            } else if (CommandLineConstants.INTERPROCESS_HC_PORT.equals(arg) || CommandLineConstants.OLD_INTERPROCESS_HC_PORT.equals(arg)) {
                bindPort = Integer.parseInt(args[index + 1]);
                argIncrement = 1;
            } else if (arg.startsWith(CommandLineConstants.INTERPROCESS_HC_PORT)) {
                String port = parseValue(arg, CommandLineConstants.INTERPROCESS_HC_PORT);
                if (port == null) {
                    parseFailed = true;
                } else {
                    bindPort = Integer.parseInt(port);
                }
            } else if (arg.startsWith(CommandLineConstants.OLD_INTERPROCESS_HC_PORT)) {
                String port = parseValue(arg, CommandLineConstants.OLD_INTERPROCESS_HC_PORT);
                if (port == null) {
                    parseFailed = true;
                } else {
                    bindPort = Integer.parseInt(port);
                }
            } else {
                isPCSocketArg = false;
            }

            return isPCSocketArg;
        }

        private void setBindAddress(String key, String value) {
            try {
                bindAddress = InetAddress.getByName(value);
            } catch (UnknownHostException e) {
                parseFailed = true;
                System.out.println(MESSAGES.invalidValue(key, "InetAddress", value));
                usage();
            }
        }
    }
}
