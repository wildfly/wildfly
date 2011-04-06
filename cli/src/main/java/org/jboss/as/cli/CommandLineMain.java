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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.cli.handlers.ConnectHandler;
import org.jboss.as.cli.handlers.CreateJmsResourceHandler;
import org.jboss.as.cli.handlers.DeleteJmsResourceHandler;
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
    private static final Set<String> tabCompleteCommands = new HashSet<String>();
    static {
        registerHandler(new HelpHandler(), "help", "h");
        registerHandler(new QuitHandler(), "quit", "q");
        registerHandler(new ConnectHandler(), "connect");
        registerHandler(new PrefixHandler(), "cd", "cn");
        registerHandler(new LsHandler(), "ls");
        registerHandler(new HistoryHandler(), "history");
        registerHandler(new DeployHandler(), "deploy");
        registerHandler(new UndeployHandler(), "undeploy");
        registerHandler(new PrintWorkingNodeHandler(), "pwd", "pwn");
        registerHandler(new CreateJmsResourceHandler(), false, "create-jms-resource");
        registerHandler(new DeleteJmsResourceHandler(), false, "delete-jms-resource");
    }

    private static void registerHandler(CommandHandler handler, String... names) {
        registerHandler(handler, true, names);
    }

    private static void registerHandler(CommandHandler handler, boolean tabComplete, String... names) {
        for(String name : names) {
            CommandHandler previous = handlers.put(name, handler);
            if(previous != null)
                throw new IllegalStateException("Duplicate command name '" + name + "'. Handlers: " + previous + ", " + handler);
        }
        if(tabComplete) {
            tabCompleteCommands.add(names[0]);
        }
    }

    private static final CommandHandler operationHandler = new OperationRequestHandler();

    public static void main(String[] args) throws Exception {

        final jline.ConsoleReader console = initConsoleReader();

        final CommandContextImpl cmdCtx = new CommandContextImpl(console);
        SecurityActions.addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                cmdCtx.disconnectController();
            }
        }));
        OperationRequestCompleter opCompleter = new OperationRequestCompleter(cmdCtx);
        console.addCompletor(new CommandCompleter(tabCompleteCommands, cmdCtx, opCompleter));
        console.addCompletor(opCompleter);

        String fileName = null;
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
            } else if(arg.startsWith("file=")) {
                fileName = arg.substring(5);
            }
        }

        if(connect) {
            cmdCtx.connectController(null, -1);
        } else {
            cmdCtx.printLine("You are disconnected at the moment." +
                " Type 'connect' to connect to the server or" +
                " 'help' for the list of supported commands.");
        }

        if(fileName != null && !fileName.isEmpty()) {
            File f = new File(fileName);
            if(!f.exists()) {
                cmdCtx.printLine("File " + f.getAbsolutePath() + " doesn't exist.");
            } else {
                BufferedReader reader = new BufferedReader(new FileReader(f));
                try {
                    String line = reader.readLine();
                    while(!cmdCtx.terminate && line != null) {
                        processLine(cmdCtx, line.trim());
                        line = reader.readLine();
                    }
                } finally {
                    StreamUtils.safeClose(reader);
                    StreamUtils.safeClose(cmdCtx.client);
                }
                return;
            }
        }

        while (!cmdCtx.terminate) {
            String line = console.readLine(cmdCtx.getPrompt()).trim();
            processLine(cmdCtx, line);
        }
        StreamUtils.safeClose(cmdCtx.client);
    }

    protected static void processLine(final CommandContextImpl cmdCtx, String line) {
        if (line.isEmpty()) {
            return;
        }

        if(isOperation(line)) {
            cmdCtx.cmdArgs = line;
            operationHandler.handle(cmdCtx);

        } else {
            String cmd = line;
            String cmdArgs = null;
            for (int i = 0; i < cmd.length(); ++i) {
                if (Character.isWhitespace(cmd.charAt(i))) {
                    cmdArgs = cmd.substring(i + 1).trim();
                    cmd = cmd.substring(0, i);
                    break;
                }
            }
            cmdCtx.setArgs(cmdArgs);

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

    protected static jline.ConsoleReader initConsoleReader() {

        final String bindingsName;
        final String osName = SecurityActions.getSystemProperty("os.name").toLowerCase();
        if(osName.indexOf("windows") >= 0) {
            bindingsName = "keybindings/jline-windows-bindings.properties";
        } else if(osName.startsWith("mac")) {
            bindingsName = "keybindings/jline-mac-bindings.properties";
        } else {
            bindingsName = "keybindings/jline-default-bindings.properties";
        }

        ClassLoader cl = SecurityActions.getClassLoader(CommandLineMain.class);
        InputStream bindingsIs = cl.getResourceAsStream(bindingsName);
        if(bindingsIs == null) {
            System.err.println("Failed to locate key bindings for OS '" + osName +"': " + bindingsName);
            try {
                return new jline.ConsoleReader();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to initialize console reader", e);
            }
        } else {
            try {
                final InputStream in = new FileInputStream(FileDescriptor.in);
                final Writer out = new PrintWriter(
                        new OutputStreamWriter(System.out,
                                System.getProperty("jline.WindowsTerminal.output.encoding",System.getProperty("file.encoding"))));
                return new jline.ConsoleReader(in, out, bindingsIs);
            } catch(Exception e) {
                throw new IllegalStateException("Failed to initialize console reader", e);
            } finally {
                StreamUtils.safeClose(bindingsIs);
            }
        }
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
        /** command argument switches */
        private Set<String> switches;
        /** named command arguments */
        private Map<String, String> namedArgs;
        /** other command arguments */
        private List<String> argsList;

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

        @Override
        public boolean hasSwitch(String switchName) {
            if(switches == null) {
                parseArgs();
            }
            return switches.contains(switchName);
        }

        @Override
        public String getNamedArgument(String argName) {
            if(namedArgs == null) {
                parseArgs();
            }
            return namedArgs.get(argName);
        }

        @Override
        public List<String> getArguments() {
            if(argsList == null) {
                parseArgs();
            }
            return argsList;
        }

        @Override
        public boolean hasArguments() {
            return cmdArgs != null;
        }

        private void parseArgs() {
            switches = null;
            namedArgs = null;
            argsList = null;
            if (cmdArgs != null) {
                String[] arr = cmdArgs.split("\\s+");
                if (arr.length > 0) {
                    for (int i = 0; i < arr.length; ++i) {
                        String arg = arr[i];
                        if (arg.charAt(0) == '-') {
                            final String switchArg;
                            if (arg.length() > 1 && arg.charAt(1) == '-') {
                                switchArg = arg.substring(2);
                            } else {
                                switchArg = arg.substring(1);
                            }
                            if (switchArg.length() > 0) {
                                if(switches == null) {
                                    switches = new HashSet<String>();
                                }
                                switches.add(switchArg);
                            } else {
                                if(argsList == null) {
                                    argsList = new ArrayList<String>();
                                }
                                argsList.add(arg);
                            }
                        } else {
                            if(argsList == null) {
                                argsList = new ArrayList<String>();
                            }
                            argsList.add(arg);

                            int equalsIndex = arg.indexOf('=');
                            if(equalsIndex > 0) {
                                if(equalsIndex == arg.length() - 1 || arg.indexOf(equalsIndex + 1, '=') < 0) {
                                } else {
                                    final String name = arg.substring(0, equalsIndex - 1).trim();
                                    final String value;
                                    if(equalsIndex == arg.length() - 1) {
                                        value = "";
                                    } else {
                                        value = arg.substring(equalsIndex + 1).trim();
                                    }
                                    if(namedArgs == null) {
                                        namedArgs = new HashMap<String, String>();
                                    }
                                    namedArgs.put(name, value);
                                }
                            }
                        }
                    }
                    if(argsList != null) {
                        argsList = Collections.unmodifiableList(argsList);
                    }
                }
            }

            if(switches == null) {
                switches = Collections.emptySet();
            }
            if(namedArgs == null) {
                namedArgs = Collections.emptyMap();
            }
            if(argsList == null) {
                argsList = Collections.emptyList();
            }
        }

        private void setArgs(String args) {
            cmdArgs = args;
            switches = null;
            namedArgs = null;
            argsList = null;
        }
    }
}
