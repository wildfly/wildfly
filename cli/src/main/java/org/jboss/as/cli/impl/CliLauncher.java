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
package org.jboss.as.cli.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.gui.GuiMain;
import org.jboss.as.cli.handlers.VersionHandler;
import org.jboss.as.protocol.StreamUtils;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 *
 * @author Alexey Loubyansky
 */
public class CliLauncher {

    public static void main(String[] args) throws Exception {
        int exitCode = 0;
        CommandContext cmdCtx = null;
        boolean gui = false;
        try {
            String argError = null;
            List<String> commands = null;
            File file = null;
            boolean connect = false;
            String defaultController = null;
            boolean version = false;
            String username = null;
            char[] password = null;
            boolean noLocalAuth = false;
            int connectionTimeout = -1;

            for(String arg : args) {
                if(arg.startsWith("--controller=") || arg.startsWith("controller=")) {
                    if(arg.startsWith("--")) {
                        defaultController = arg.substring(13);
                    } else {
                        defaultController = arg.substring(11);
                    }
                } else if("--connect".equals(arg) || "-c".equals(arg)) {
                    connect = true;
                } else if("--version".equals(arg)) {
                    version = true;
                } else if ("--gui".equals(arg)) {
                    gui = true;
                } else if(arg.startsWith("--file=") || arg.startsWith("file=")) {
                    if(file != null) {
                        argError = "Duplicate argument '--file'.";
                        break;
                    }
                    if(commands != null) {
                        argError = "Only one of '--file', '--commands' or '--command' can appear as the argument at a time.";
                        break;
                    }

                    final String fileName = arg.startsWith("--") ? arg.substring(7) : arg.substring(5);
                    if(!fileName.isEmpty()) {
                        file = new File(fileName);
                        if(!file.exists()) {
                            argError = "File " + file.getAbsolutePath() + " doesn't exist.";
                            break;
                        }
                    } else {
                        argError = "Argument '--file' is missing value.";
                        break;
                    }
                } else if(arg.startsWith("--commands=") || arg.startsWith("commands=")) {
                    if(file != null) {
                        argError = "Only one of '--file', '--commands' or '--command' can appear as the argument at a time.";
                        break;
                    }
                    if(commands != null) {
                        argError = "Duplicate argument '--command'/'--commands'.";
                        break;
                    }
                    final String value = arg.startsWith("--") ? arg.substring(11) : arg.substring(9);
                    commands = Util.splitCommands(value);
                } else if(arg.startsWith("--command=") || arg.startsWith("command=")) {
                    if(file != null) {
                        argError = "Only one of '--file', '--commands' or '--command' can appear as the argument at a time.";
                        break;
                    }
                    if(commands != null) {
                        argError = "Duplicate argument '--command'/'--commands'.";
                        break;
                    }
                    final String value = arg.startsWith("--") ? arg.substring(10) : arg.substring(8);
                    commands = Collections.singletonList(value);
                } else if (arg.startsWith("--user=")) {
                    username = arg.startsWith("--") ? arg.substring(7) : arg.substring(5);
                    noLocalAuth = true;
                } else if (arg.startsWith("--password=")) {
                    password = (arg.startsWith("--") ? arg.substring(11) : arg.substring(9)).toCharArray();
                } else if (arg.equals("--no-local-auth")) {
                    noLocalAuth = true;
                } else if (arg.startsWith("--timeout=")) {
                    if (connectionTimeout > 0) {
                        argError = "Duplicate argument '--timeout'";
                        break;
                    }
                    final String value = arg.substring(10);
                    try {
                        connectionTimeout = Integer.parseInt(value);
                    } catch (final NumberFormatException e) {
                        //
                    }
                    if (connectionTimeout <= 0) {
                        argError = "The timeout must be a valid positive integer: '" + value + "'";
                    }
                } else if (arg.equals("--help") || arg.equals("-h")) {
                    commands = Collections.singletonList("help");
                } else if (arg.startsWith("--properties=")) {
                    final String value  = arg.substring(13);
                    final File propertiesFile = new File(value);
                    if(!propertiesFile.exists()) {
                        argError = "File doesn't exist: " + propertiesFile.getAbsolutePath();
                        break;
                    }
                    final Properties props = new Properties();
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(propertiesFile);
                        props.load(fis);
                    } catch(FileNotFoundException e) {
                        argError = e.getLocalizedMessage();
                        break;
                    } catch(java.io.IOException e) {
                        argError = "Failed to load properties from " + propertiesFile.getAbsolutePath() + ": " + e.getLocalizedMessage();
                        break;
                    } finally {
                        if(fis != null) {
                            try {
                                fis.close();
                            } catch(java.io.IOException e) {
                            }
                        }
                    }
                    for(Object prop : props.keySet()) {
                        WildFlySecurityManager.setPropertyPrivileged((String) prop, (String) props.get(prop));
                    }
                } else if(!(arg.startsWith("-D") || arg.equals("-XX:"))) {// skip system properties and jvm options
                    // assume it's commands
                    if(file != null) {
                        argError = "Only one of '--file', '--commands' or '--command' can appear as the argument at a time: " + arg;
                        break;
                    }
                    if(commands != null) {
                        argError = "Duplicate argument '--command'/'--commands'.";
                        break;
                    }
                    commands = Util.splitCommands(arg);
                }
            }

            if(argError != null) {
                System.err.println(argError);
                exitCode = 1;
                return;
            }

            if(version) {
                cmdCtx = initCommandContext(defaultController, username, password, noLocalAuth, false, connect, connectionTimeout);
                VersionHandler.INSTANCE.handle(cmdCtx);
                return;
            }

            if(file != null) {
                cmdCtx = initCommandContext(defaultController, username, password, noLocalAuth, false, connect, connectionTimeout);
                processFile(file, cmdCtx);
                return;
            }

            if(commands != null) {
                cmdCtx = initCommandContext(defaultController, username, password, noLocalAuth, false, connect, connectionTimeout);
                processCommands(commands, cmdCtx);
                return;
            }

            if (gui) {
                cmdCtx = initCommandContext(defaultController, username, password, noLocalAuth, false, true, connectionTimeout);
                processGui(cmdCtx);
                return;
            }

            // Interactive mode
            cmdCtx = initCommandContext(defaultController, username, password, noLocalAuth, true, connect, connectionTimeout);
            cmdCtx.interact();
        } catch(Throwable t) {
            t.printStackTrace();
            exitCode = 1;
        } finally {
            if(cmdCtx != null) {
                cmdCtx.terminateSession();
                if(cmdCtx.getExitCode() != 0) {
                    exitCode = cmdCtx.getExitCode();
                }
            }
            if (!gui) {
                System.exit(exitCode);
            }
        }
        System.exit(exitCode);
    }

    private static CommandContext initCommandContext(String defaultController, String username, char[] password, boolean disableLocalAuth, boolean initConsole, boolean connect, final int connectionTimeout) throws CliInitializationException {
        final CommandContext cmdCtx = CommandContextFactory.getInstance().newCommandContext(defaultController, username, password, disableLocalAuth, initConsole, connectionTimeout);
        if(connect) {
            try {
                cmdCtx.connectController();
            } catch (CommandLineException e) {
                throw new CliInitializationException("Failed to connect to the controller", e);
            }
        }
        return cmdCtx;
    }

    private static void processGui(final CommandContext cmdCtx) {
        try {
            GuiMain.start(cmdCtx);
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

    private static void processCommands(List<String> commands, CommandContext cmdCtx) {
        int i = 0;
        while (cmdCtx.getExitCode() == 0 && i < commands.size() && !cmdCtx.isTerminated()) {
            cmdCtx.handleSafe(commands.get(i));
            ++i;
        }
    }

    private static void processFile(File file, final CommandContext cmdCtx) {

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (cmdCtx.getExitCode() == 0 && !cmdCtx.isTerminated() && line != null) {
                cmdCtx.handleSafe(line.trim());
                line = reader.readLine();
            }
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to process file '" + file.getAbsolutePath() + "'", e);
        } finally {
            StreamUtils.safeClose(reader);
        }
    }

    private static final String JBOSS_CLI_RC_PROPERTY = "jboss.cli.rc";
    private static final String CURRENT_WORKING_DIRECTORY = "user.dir";
    private static final String JBOSS_CLI_RC_FILE = ".jbossclirc";

    static void runcom(CommandContext ctx) throws CliInitializationException {
        File jbossCliRcFile = null;
        // system property first
        String jbossCliRc = WildFlySecurityManager.getPropertyPrivileged(JBOSS_CLI_RC_PROPERTY, null);
        if(jbossCliRc == null) {
            // current dir second
            String dir = WildFlySecurityManager.getPropertyPrivileged(CURRENT_WORKING_DIRECTORY, null);
            File f = new File(dir, JBOSS_CLI_RC_FILE);
            if(!f.exists()) {
                // WildFly home bin dir third
                dir = WildFlySecurityManager.getEnvPropertyPrivileged("JBOSS_HOME", null);
                if(dir != null) {
                    f = new File(dir + File.separatorChar + "bin", JBOSS_CLI_RC_FILE);
                    if(f.exists()) {
                        jbossCliRcFile = f;
                    }
                }
            } else {
                jbossCliRcFile = f;
            }
        } else {
            jbossCliRcFile = new File(jbossCliRc);
            if(!jbossCliRcFile.exists()) {
                throw new CliInitializationException("Property " + JBOSS_CLI_RC_PROPERTY +
                        " points to a file that doesn't exist: " + jbossCliRcFile.getAbsolutePath());
            }
        }

        if(jbossCliRcFile != null) {
            processFile(jbossCliRcFile, ctx);
            if(ctx.getExitCode() != 0) {
                throw new CliInitializationException("Failed to process " + jbossCliRcFile.getAbsoluteFile());
            }
        }
    }
}
