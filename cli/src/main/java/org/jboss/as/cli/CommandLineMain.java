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
package org.jboss.as.cli;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.cli.handlers.ConnectHandler;
import org.jboss.as.cli.handlers.PrintWorkingNodeHandler;
import org.jboss.as.cli.handlers.DeployHandler;
import org.jboss.as.cli.handlers.HelpHandler;
import org.jboss.as.cli.handlers.HistoryHandler;
import org.jboss.as.cli.handlers.LsHandler;
import org.jboss.as.cli.handlers.OperationRequestHandler;
import org.jboss.as.cli.handlers.PrefixHandler;
import org.jboss.as.cli.handlers.QuitHandler;
import org.jboss.as.cli.handlers.UndeployHandler;
import org.jboss.as.cli.operation.OperationCandidatesProvider;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.OperationRequestParser;
import org.jboss.as.cli.operation.PrefixFormatter;
import org.jboss.as.cli.operation.impl.DefaultOperationCandidatesProvider;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestParser;
import org.jboss.as.cli.operation.impl.DefaultPrefixFormatter;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.StreamUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class CommandLineMain {

    private static final Map<String, CommandHandler> handlers = new HashMap<String, CommandHandler>();
    static {
        registerHandler(new HelpHandler(), "help", "h");
        registerHandler(new QuitHandler(), "quit", "q");
        registerHandler(new ConnectHandler(), "connect");
        registerHandler(new PrefixHandler(), "cd", "cn");
        registerHandler(new LsHandler(), "ls");
        registerHandler(new HistoryHandler(), "history");
        registerHandler(new DeployHandler(), "deploy");
        registerHandler(new UndeployHandler(), "undeploy");
        registerHandler(new PrintWorkingNodeHandler(), "pwn", "pwd");
    }

    private static void registerHandler(CommandHandler handler, String... names) {
        for(String name : names) {
            CommandHandler previous = handlers.put(name, handler);
            if(previous != null)
                throw new IllegalStateException("Duplicate command name '" + name + "'. Handlers: " + previous + ", " + handler);
        }
    }

    private static final CommandHandler operationHandler = new OperationRequestHandler();

    public static void main(String[] args) throws Exception {

        final jline.ConsoleReader console = new jline.ConsoleReader();

        final CommandContextImpl cmdCtx = new CommandContextImpl(console);
        SecurityActions.addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                cmdCtx.disconnectController();
            }
        }));
        OperationRequestCompleter opCompleter = new OperationRequestCompleter(cmdCtx);
        console.addCompletor(new CommandCompleter(handlers.keySet(), opCompleter));
        console.addCompletor(opCompleter);

        boolean connect = false;
        for(String arg : args) {
            if(arg.startsWith("controller=")) {
                String value = arg.substring(11);
                String portStr = null;
                int colonIndex = value.indexOf(':');
                if(colonIndex < 0) {
                    // default port
                    cmdCtx.defaultControllerHost = value;
                } else if(colonIndex == 0) {
                    // default host
                    portStr = value.substring(1);
                } else {
                    cmdCtx.defaultControllerHost = value.substring(0, colonIndex);
                    portStr = value.substring(colonIndex + 1);
                }

                if(portStr != null) {
                    int port = -1;
                    try {
                        port = Integer.parseInt(portStr);
                        if(port < 0) {
                            cmdCtx.printLine("The port must be a valid non-negative integer: '" + args + "'");
                        } else {
                            cmdCtx.defaultControllerPort = port;
                        }
                    } catch(NumberFormatException e) {
                        cmdCtx.printLine("The port must be a valid non-negative integer: '" + arg + "'");
                    }
                }
            } else if("--connect".equals(arg)) {
                connect = true;
            }
        }

        if(connect) {
            cmdCtx.connectController(null, -1);
        } else {
            cmdCtx.printLine("You are disconnected at the moment." +
                " Type 'connect' to connect to the server or" +
                " 'help' for the list of supported commands.");
        }

        while (!cmdCtx.terminate) {
            String line = console.readLine(cmdCtx.getPrompt()).trim();

            if (line.isEmpty()) {
                // cmdCtx.log("Type /help for the list of supported commands.");
                continue;
            }

            if(isOperation(line)) {
                cmdCtx.cmdArgs = line;
                operationHandler.handle(cmdCtx);

            } else {
                String cmd = line;
                cmdCtx.cmdArgs = null;
                for (int i = 0; i < cmd.length(); ++i) {
                    if (Character.isWhitespace(cmd.charAt(i))) {
                        cmdCtx.cmdArgs = cmd.substring(i + 1).trim();
                        cmd = cmd.substring(0, i);
                        break;
                    }
                }

                CommandHandler handler = handlers.get(cmd.toLowerCase());
                if (handler != null) {
                    handler.handle(cmdCtx);
                } else {
                    cmdCtx.printLine("Unexpected command '"
                            + line
                            + "'. Type 'help' for the list of supported commands.");
                }
            }
        }
        StreamUtils.safeClose(cmdCtx.client);
    }

    private static boolean isOperation(String line) {
        char firstChar = line.charAt(0);
        return firstChar == '.' || firstChar == ':' || firstChar == '/' || line.startsWith("..") || line.startsWith(".type");
    }

    private static class CommandContextImpl implements CommandContext {

        private final jline.ConsoleReader console;
        private final CommandHistory history;

        /** whether the session should be terminated*/
        private boolean terminate;
        /** current command's arguments */
        private String cmdArgs;
        /** the controller client */
        private ModelControllerClient client;
        /** the default controller host */
        private String defaultControllerHost = "localhost";
        /** the default controller port */
        private int defaultControllerPort = 9999;
        /** the host of the controller */
        private String controllerHost;
        /** the port of the controller */
        private int controllerPort = -1;
        /** various key/value pairs */
        private Map<String, Object> map = new HashMap<String, Object>();
        /** operation request parser */
        private final OperationRequestParser parser = new DefaultOperationRequestParser();
        /** operation request address prefix */
        private final OperationRequestAddress prefix = new DefaultOperationRequestAddress();
        /** the prefix formatter */
        private final PrefixFormatter prefixFormatter = new DefaultPrefixFormatter();
        /** provider of operation request candidates for tab-completion */
        private final OperationCandidatesProvider operationCandidatesProvider;

        private CommandContextImpl(jline.ConsoleReader console) {
            this.console = console;

            console.setUseHistory(true);
            String userHome = SecurityActions.getSystemProperty("user.home");
            File historyFile = new File(userHome, ".jboss-cli-history");
            try {
                console.getHistory().setHistoryFile(historyFile);
            } catch (IOException e) {
                System.err.println("Failed to setup the history file " + historyFile.getAbsolutePath() + ": " + e.getLocalizedMessage());
            }

            this.history = new HistoryImpl();
            operationCandidatesProvider = new DefaultOperationCandidatesProvider(this);
        }

        @Override
        public String getCommandArguments() {
            return cmdArgs;
        }

        @Override
        public void terminateSession() {
            terminate = true;
        }

        @Override
        public void printLine(String message) {
            try {
                console.printString(message);
                console.printNewline();
            } catch (IOException e) {
                System.err.println("Failed to print '" + message + "' to the console: " + e.getLocalizedMessage());
            }
        }

        @Override
        public void printColumns(Collection<String> col) {
            try {
                console.printColumns(col);
            } catch (IOException e) {
                System.err.println("Failed to print columns '" + col + "' to the console: " + e.getLocalizedMessage());
            }
        }

        @Override
        public void set(String key, Object value) {
            map.put(key, value);
        }

        @Override
        public Object get(String key) {
            return map.get(key);
        }

        @Override
        public ModelControllerClient getModelControllerClient() {
            return client;
        }

        @Override
        public OperationRequestParser getOperationRequestParser() {
            return parser;
        }

        @Override
        public OperationRequestAddress getPrefix() {
            return prefix;
        }

        @Override
        public PrefixFormatter getPrefixFormatter() {

            return prefixFormatter;
        }

        @Override
        public OperationCandidatesProvider getOperationCandidatesProvider() {
            return operationCandidatesProvider;
        }

        @Override
        public void connectController(String host, int port) {
            if(host == null) {
                host = defaultControllerHost;
            }

            if(port < 0) {
                port = defaultControllerPort;
            }

            try {
                ModelControllerClient newClient = ModelControllerClient.Factory.create(host, port);
                if(this.client != null) {
                    disconnectController();
                }
                printLine("Connected to " + host + ":" + port);
                client = newClient;
                this.controllerHost = host;
                this.controllerPort = port;
            } catch (UnknownHostException e) {
                printLine("Failed to resolve host '" + host + "': " + e.getLocalizedMessage());
            }
        }

        @Override
        public void disconnectController() {
            if(this.client != null) {
                StreamUtils.safeClose(client);
                printLine("Closed connection to " + this.controllerHost + ':' + this.controllerPort);
                client = null;
                this.controllerHost = null;
                this.controllerPort = -1;
            }
        }

        @Override
        public String getControllerHost() {
            return controllerHost;
        }

        @Override
        public int getControllerPort() {
            return controllerPort;
        }

        String getPrompt() {
            StringBuilder buffer = new StringBuilder();
            buffer.append('[');
            if(controllerHost != null) {
                buffer.append(controllerHost)
                .append(':')
                .append(controllerPort)
                .append(' ');
            } else {
                buffer.append("disconnected ");
            }

            if(prefix.isEmpty()) {
                buffer.append('/');
            } else {
                final String nodeName = prefix.getNodeName();
                if(nodeName == null) {
                    buffer.append(prefix.getNodeType());
                } else {
                    buffer.append(nodeName);
                }
            }
            buffer.append("] ");
            return buffer.toString();
        }

        @Override
        public CommandHistory getHistory() {
            return history;
        }

        private class HistoryImpl implements CommandHistory {

            @SuppressWarnings("unchecked")
            @Override
            public List<String> asList() {
                return console.getHistory().getHistoryList();
            }

            @Override
            public boolean isUseHistory() {
                return console.getUseHistory();
            }

            @Override
            public void setUseHistory(boolean useHistory) {
                console.setUseHistory(useHistory);
            }

            @Override
            public void clear() {
                console.getHistory().clear();
            }
        }

        @Override
        public String getDefaultControllerHost() {
            return defaultControllerHost;
        }

        @Override
        public int getDefaultControllerPort() {
            return defaultControllerPort;
        }
    }
}
