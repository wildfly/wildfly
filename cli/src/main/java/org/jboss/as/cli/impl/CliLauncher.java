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
import java.io.FileReader;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.gui.GuiMain;
import org.jboss.as.cli.handlers.VersionHandler;
import org.jboss.as.protocol.StreamUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class CliLauncher {

    public static void main(String[] args) throws Exception {
        boolean gui = false;
        try {
            String argError = null;
            String[] commands = null;
            File file = null;
            boolean connect = false;
            String defaultControllerHost = null;
            int defaultControllerPort = -1;
            boolean version = false;
            String username = null;
            char[] password = null;
            for(String arg : args) {
                if(arg.startsWith("--controller=") || arg.startsWith("controller=")) {
                    final String value;
                    if(arg.startsWith("--")) {
                        value = arg.substring(13);
                    } else {
                        value = arg.substring(11);
                    }
                    String portStr = null;
                    int colonIndex = value.indexOf(':');
                    if(colonIndex < 0) {
                        // default port
                        defaultControllerHost = value;
                    } else if(colonIndex == 0) {
                        // default host
                        portStr = value.substring(1);
                    } else {
                        defaultControllerHost = value.substring(0, colonIndex);
                        portStr = value.substring(colonIndex + 1);
                    }

                    if(portStr != null) {
                        int port = -1;
                        try {
                            port = Integer.parseInt(portStr);
                            if(port < 0) {
                                argError = "The port must be a valid non-negative integer: '" + args + "'";
                            } else {
                                defaultControllerPort = port;
                            }
                        } catch(NumberFormatException e) {
                            argError = "The port must be a valid non-negative integer: '" + arg + "'";
                        }
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
                    commands = value.split(",+");
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
                    commands = new String[]{value};
                } else if (arg.startsWith("--user=")) {
                    username = arg.startsWith("--") ? arg.substring(7) : arg.substring(5);
                } else if (arg.startsWith("--password=")) {
                    password = (arg.startsWith("--") ? arg.substring(11) : arg.substring(9)).toCharArray();
                } else if (arg.equals("--help") || arg.equals("-h")) {
                    commands = new String[]{"help"};
                } else {
                    // assume it's commands
                    if(file != null) {
                        argError = "Only one of '--file', '--commands' or '--command' can appear as the argument at a time.";
                        break;
                    }
                    if(commands != null) {
                        argError = "Duplicate argument '--command'/'--commands'.";
                        break;
                    }
                    commands = arg.split(",+");
                }
            }

            if(argError != null) {
                System.err.println(argError);
                return;
            }

            if(version) {
                final CommandContextImpl cmdCtx = new CommandContextImpl();
                VersionHandler.INSTANCE.handle(cmdCtx);
                return;
            }

            if(file != null) {
                processFile(file, defaultControllerHost, defaultControllerPort, connect, username, password);
                return;
            }

            if(commands != null) {
                processCommands(commands, defaultControllerHost, defaultControllerPort, connect, username, password);
                return;
            }

            if (gui) {
                processGui(defaultControllerHost, defaultControllerPort, connect, username, password);
                return;
            }

            // Interactive mode

            final CommandContextImpl cmdCtx = new CommandContextImpl(defaultControllerHost, defaultControllerPort, username, password, true);
            cmdCtx.interact(connect);
        } catch(Throwable t) {
            t.printStackTrace();
        } finally {
            if (!gui) System.exit(0);
        }
        System.exit(0);
    }

    private static void processGui(String defaultControllerHost, int defaultControllerPort, final boolean connect, final String username, final char[] password) throws CliInitializationException {

        final CommandContextImpl cmdCtx = new CommandContextImpl(defaultControllerHost, defaultControllerPort, username, password, false);
        SecurityActions.addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (!cmdCtx.isTerminated()) {
                    cmdCtx.terminateSession();
                }
                cmdCtx.disconnectController();
            }
        }));

        if(connect) {
            cmdCtx.connectController(null, -1);
        }

        try {
            new GuiMain(cmdCtx);
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

    private static void processCommands(String[] commands, String defaultControllerHost, int defaultControllerPort, final boolean connect, final String username, final char[] password) throws CliInitializationException {

        final CommandContextImpl cmdCtx = new CommandContextImpl(defaultControllerHost, defaultControllerPort, username, password, false);
        SecurityActions.addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                cmdCtx.disconnectController();
            }
        }));

        if(connect) {
            cmdCtx.connectController(null, -1);
        }

        try {
            for (int i = 0; i < commands.length && !cmdCtx.isTerminated(); ++i) {
                cmdCtx.processLine(commands[i]);
            }
        } catch(Throwable t) {
            t.printStackTrace();
        } finally {
            if (!cmdCtx.isTerminated()) {
                cmdCtx.terminateSession();
            }
            cmdCtx.disconnectController();
        }
    }

    private static void processFile(File file, String defaultControllerHost, int defaultControllerPort, final boolean connect, final String username, final char[] password) throws CliInitializationException {

        final CommandContextImpl cmdCtx = new CommandContextImpl(defaultControllerHost, defaultControllerPort, username, password, false);
        SecurityActions.addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                cmdCtx.disconnectController();
            }
        }));

        if(connect) {
            cmdCtx.connectController(null, -1);
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (!cmdCtx.isTerminated() && line != null) {
                cmdCtx.processLine(line.trim());
                line = reader.readLine();
            }
        } catch (Throwable e) {
            cmdCtx.printLine("Failed to process file '" + file.getAbsolutePath() + "'");
            e.printStackTrace();
        } finally {
            StreamUtils.safeClose(reader);
            if (!cmdCtx.isTerminated()) {
                cmdCtx.terminateSession();
            }
            cmdCtx.disconnectController();
        }
    }
}
