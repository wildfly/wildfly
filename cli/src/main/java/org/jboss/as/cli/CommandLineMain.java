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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import javax.security.sasl.SaslException;

import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.batch.BatchedCommand;
import org.jboss.as.cli.batch.impl.DefaultBatchManager;
import org.jboss.as.cli.batch.impl.DefaultBatchedCommand;
import org.jboss.as.cli.handlers.ClearScreenHandler;
import org.jboss.as.cli.handlers.CommandCommandHandler;
import org.jboss.as.cli.handlers.ConnectHandler;
import org.jboss.as.cli.handlers.DeployHandler;
import org.jboss.as.cli.handlers.GenericTypeOperationHandler;
import org.jboss.as.cli.handlers.HelpHandler;
import org.jboss.as.cli.handlers.HistoryHandler;
import org.jboss.as.cli.handlers.LsHandler;
import org.jboss.as.cli.handlers.OperationRequestHandler;
import org.jboss.as.cli.handlers.PrefixHandler;
import org.jboss.as.cli.handlers.PrintWorkingNodeHandler;
import org.jboss.as.cli.handlers.QuitHandler;
import org.jboss.as.cli.handlers.UndeployHandler;
import org.jboss.as.cli.handlers.VersionHandler;
import org.jboss.as.cli.handlers.batch.BatchClearHandler;
import org.jboss.as.cli.handlers.batch.BatchDiscardHandler;
import org.jboss.as.cli.handlers.batch.BatchEditLineHandler;
import org.jboss.as.cli.handlers.batch.BatchHandler;
import org.jboss.as.cli.handlers.batch.BatchHoldbackHandler;
import org.jboss.as.cli.handlers.batch.BatchListHandler;
import org.jboss.as.cli.handlers.batch.BatchMoveLineHandler;
import org.jboss.as.cli.handlers.batch.BatchRemoveLineHandler;
import org.jboss.as.cli.handlers.batch.BatchRunHandler;
import org.jboss.as.cli.handlers.jca.DataSourceAddHandler;
import org.jboss.as.cli.handlers.jca.DataSourceModifyHandler;
import org.jboss.as.cli.handlers.jca.DataSourceRemoveHandler;
import org.jboss.as.cli.handlers.jca.XADataSourceAddHandler;
import org.jboss.as.cli.handlers.jca.XADataSourceModifyHandler;
import org.jboss.as.cli.handlers.jca.XADataSourceRemoveHandler;
import org.jboss.as.cli.handlers.jms.CreateJmsResourceHandler;
import org.jboss.as.cli.handlers.jms.DeleteJmsResourceHandler;
import org.jboss.as.cli.handlers.jms.JmsCFAddHandler;
import org.jboss.as.cli.handlers.jms.JmsCFRemoveHandler;
import org.jboss.as.cli.handlers.jms.JmsQueueAddHandler;
import org.jboss.as.cli.handlers.jms.JmsQueueRemoveHandler;
import org.jboss.as.cli.handlers.jms.JmsTopicAddHandler;
import org.jboss.as.cli.handlers.jms.JmsTopicRemoveHandler;
import org.jboss.as.cli.operation.OperationCandidatesProvider;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.PrefixFormatter;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationCandidatesProvider;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestParser;
import org.jboss.as.cli.operation.impl.DefaultPrefixFormatter;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.sasl.JBossSaslProvider;
import org.jboss.sasl.callback.DigestHashCallback;

/**
*
* @author Alexey Loubyansky
*/
public class CommandLineMain {

    private static final CommandRegistry cmdRegistry = new CommandRegistry();
    static {
        cmdRegistry.registerHandler(new HelpHandler(), "help", "h");
        cmdRegistry.registerHandler(new QuitHandler(), "quit", "q", "exit");
        cmdRegistry.registerHandler(new ConnectHandler(), "connect");
        cmdRegistry.registerHandler(new PrefixHandler(), "cd", "cn");
        cmdRegistry.registerHandler(new ClearScreenHandler(), "clear", "cls");
        cmdRegistry.registerHandler(new LsHandler(), "ls");
        cmdRegistry.registerHandler(new HistoryHandler(), "history");
        cmdRegistry.registerHandler(new DeployHandler(), "deploy");
        cmdRegistry.registerHandler(new UndeployHandler(), "undeploy");
        cmdRegistry.registerHandler(new PrintWorkingNodeHandler(), "pwd", "pwn");

        cmdRegistry.registerHandler(new BatchHandler(), "batch");
        cmdRegistry.registerHandler(new BatchDiscardHandler(), "discard-batch");
        cmdRegistry.registerHandler(new BatchListHandler(), "list-batch");
        cmdRegistry.registerHandler(new BatchHoldbackHandler(), "holdback-batch");
        cmdRegistry.registerHandler(new BatchRunHandler(), "run-batch");
        cmdRegistry.registerHandler(new BatchClearHandler(), "clear-batch");
        cmdRegistry.registerHandler(new BatchRemoveLineHandler(), "remove-batch-line");
        cmdRegistry.registerHandler(new BatchMoveLineHandler(), "move-batch-line");
        cmdRegistry.registerHandler(new BatchEditLineHandler(), "edit-batch-line");

        cmdRegistry.registerHandler(new VersionHandler(), "version");

        cmdRegistry.registerHandler(new CommandCommandHandler(cmdRegistry), "command");

        // data-source
        cmdRegistry.registerHandler(new GenericTypeOperationHandler("/subsystem=datasources/data-source", "jndi-name"), "data-source");
        cmdRegistry.registerHandler(new GenericTypeOperationHandler("/subsystem=datasources/xa-data-source", "jndi-name"), "xa-data-source");
        // supported but hidden from the tab-completion
        cmdRegistry.registerHandler(new DataSourceAddHandler(), false, "add-data-source");
        cmdRegistry.registerHandler(new DataSourceModifyHandler(), false, "modify-data-source");
        cmdRegistry.registerHandler(new DataSourceRemoveHandler(), false, "remove-data-source");
        cmdRegistry.registerHandler(new XADataSourceAddHandler(), false, "add-xa-data-source");
        cmdRegistry.registerHandler(new XADataSourceRemoveHandler(), false, "remove-xa-data-source");
        cmdRegistry.registerHandler(new XADataSourceModifyHandler(), false, "modify-xa-data-source");

        // JMS
        cmdRegistry.registerHandler(new GenericTypeOperationHandler("/subsystem=messaging/hornetq-server=default/jms-queue", "queue-address"), "jms-queue");
        cmdRegistry.registerHandler(new GenericTypeOperationHandler("/subsystem=messaging/hornetq-server=default/jms-topic", "topic-address"), "jms-topic");
        cmdRegistry.registerHandler(new GenericTypeOperationHandler("/subsystem=messaging/hornetq-server=default/connection-factory", null), "connection-factory");
        // supported but hidden from the tab-completion
        cmdRegistry.registerHandler(new JmsQueueAddHandler(), false, "add-jms-queue");
        cmdRegistry.registerHandler(new JmsQueueRemoveHandler(), false, "remove-jms-queue");
        cmdRegistry.registerHandler(new JmsTopicAddHandler(), false, "add-jms-topic");
        cmdRegistry.registerHandler(new JmsTopicRemoveHandler(), false, "remove-jms-topic");
        cmdRegistry.registerHandler(new JmsCFAddHandler(), false, "add-jms-cf");
        cmdRegistry.registerHandler(new JmsCFRemoveHandler(), false, "remove-jms-cf");
        // these are used for the cts setup
        cmdRegistry.registerHandler(new CreateJmsResourceHandler(), false, "create-jms-resource");
        cmdRegistry.registerHandler(new DeleteJmsResourceHandler(), false, "delete-jms-resource");
    }

    public static void main(String[] args) throws Exception {
        try {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    return Security.insertProviderAt(new JBossSaslProvider(), 1);
                }
            });

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

            // Interactive mode

            final jline.ConsoleReader console = initConsoleReader();
            final CommandContextImpl cmdCtx = new CommandContextImpl(console);
            SecurityActions.addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    cmdCtx.disconnectController();
                }
            }));
            console.addCompletor(cmdCtx.cmdCompleter);

            cmdCtx.username = username;
            cmdCtx.password = password;
            if(defaultControllerHost != null) {
                cmdCtx.defaultControllerHost = defaultControllerHost;
            }
            if(defaultControllerPort != -1) {
                cmdCtx.defaultControllerPort = defaultControllerPort;
            }

            if(connect) {
                cmdCtx.connectController(null, -1);
            } else {
                cmdCtx.printLine("You are disconnected at the moment." +
                    " Type 'connect' to connect to the server or" +
                    " 'help' for the list of supported commands.");
            }

            try {
                while (!cmdCtx.terminate) {
                    final String line = console.readLine(cmdCtx.getPrompt());
                    if(line == null) {
                        cmdCtx.terminateSession();
                    } else {
                        processLine(cmdCtx, line.trim());
                    }
                }
            } catch(Throwable t) {
                t.printStackTrace();
            } finally {
                cmdCtx.disconnectController();
            }
        } finally {
            System.exit(0);
        }
        System.exit(0);
    }

    private static void processCommands(String[] commands, String defaultControllerHost, int defaultControllerPort, final boolean connect, final String username, final char[] password) {

        final CommandContextImpl cmdCtx = new CommandContextImpl();
        SecurityActions.addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                cmdCtx.disconnectController();
            }
        }));

        cmdCtx.username = username;
        cmdCtx.password = password;
        if (defaultControllerHost != null) {
            cmdCtx.defaultControllerHost = defaultControllerHost;
        }
        if(defaultControllerPort != -1) {
            cmdCtx.defaultControllerPort = defaultControllerPort;
        }

        if(connect) {
            cmdCtx.connectController(null, -1);
        }

        try {
            for (int i = 0; i < commands.length && !cmdCtx.terminate; ++i) {
                processLine(cmdCtx, commands[i]);
            }
        } catch(Throwable t) {
            t.printStackTrace();
        } finally {
            if (!cmdCtx.terminate) {
                cmdCtx.terminateSession();
            }
            cmdCtx.disconnectController();
        }
    }

    private static void processFile(File file, String defaultControllerHost, int defaultControllerPort, final boolean connect, final String username, final char[] password) {

        final CommandContextImpl cmdCtx = new CommandContextImpl();
        SecurityActions.addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                cmdCtx.disconnectController();
            }
        }));

        cmdCtx.username = username;
        cmdCtx.password = password;
        if (defaultControllerHost != null) {
            cmdCtx.defaultControllerHost = defaultControllerHost;
        }
        if(defaultControllerPort != -1) {
            cmdCtx.defaultControllerPort = defaultControllerPort;
        }

        if(connect) {
            cmdCtx.connectController(null, -1);
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (!cmdCtx.terminate && line != null) {
                processLine(cmdCtx, line.trim());
                line = reader.readLine();
            }
        } catch (Throwable e) {
            cmdCtx.printLine("Failed to process file '" + file.getAbsolutePath() + "'");
            e.printStackTrace();
        } finally {
            StreamUtils.safeClose(reader);
            if (!cmdCtx.terminate) {
                cmdCtx.terminateSession();
            }
            cmdCtx.disconnectController();
        }
    }

    protected static void processLine(final CommandContextImpl cmdCtx, String line) {
        if (line.isEmpty()) {
            return;
        }
        if (line.charAt(0) == '#') {
            return; // ignore comments
        }
        if(isOperation(line)) {

            ModelNode request;
            try {
                cmdCtx.resetArgs(line);
                request = cmdCtx.parsedCmd.toOperationRequest();
            } catch (CommandFormatException e) {
                cmdCtx.printLine(e.getLocalizedMessage());
                return;
            }

            if(cmdCtx.isBatchMode()) {
                StringBuilder op = new StringBuilder();
                op.append(cmdCtx.getPrefixFormatter().format(cmdCtx.parsedCmd.getAddress()));
                op.append(line.substring(line.indexOf(':')));
                DefaultBatchedCommand batchedCmd = new DefaultBatchedCommand(op.toString(), request);
                Batch batch = cmdCtx.getBatchManager().getActiveBatch();
                batch.add(batchedCmd);
                cmdCtx.printLine("#" + batch.size() + " " + batchedCmd.getCommand());
            } else {
                cmdCtx.set("OP_REQ", request);
                try {
                    cmdCtx.operationHandler.handle(cmdCtx);
                } finally {
                    cmdCtx.set("OP_REQ", null);
                }
            }

        } else {
            try {
                cmdCtx.resetArgs(line);
            } catch (CommandFormatException e1) {
                cmdCtx.printLine(e1.getLocalizedMessage());
                return;
            }

            final String cmdName = cmdCtx.parsedCmd.getOperationName();
            CommandHandler handler = cmdRegistry.getCommandHandler(cmdName.toLowerCase());
            if(handler != null) {
                if(cmdCtx.isBatchMode() && handler.isBatchMode()) {
                    if(!(handler instanceof OperationCommand)) {
                        cmdCtx.printLine("The command is not allowed in a batch.");
                    } else {
                        try {
                            ModelNode request = ((OperationCommand)handler).buildRequest(cmdCtx);
                            BatchedCommand batchedCmd = new DefaultBatchedCommand(line, request);
                            Batch batch = cmdCtx.getBatchManager().getActiveBatch();
                            batch.add(batchedCmd);
                            cmdCtx.printLine("#" + batch.size() + " " + batchedCmd.getCommand());
                        } catch (CommandFormatException e) {
                            cmdCtx.printLine("Failed to add to batch: " + e.getLocalizedMessage());
                        }
                    }
                } else {
                    try {
                        handler.handle(cmdCtx);
                    } catch (CommandFormatException e) {
                        cmdCtx.printLine(e.getLocalizedMessage());
                    }
                }

                // TODO this doesn't make sense
                try {
                    cmdCtx.resetArgs(null);
                } catch (CommandFormatException e) {
                }
            } else {
                cmdCtx.printLine("Unexpected command '" + line
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
                String encoding = SecurityActions.getSystemProperty("jline.WindowsTerminal.output.encoding");
                if(encoding == null) {
                    encoding = SecurityActions.getSystemProperty("file.encoding");
                }
                final Writer out = new PrintWriter(new OutputStreamWriter(System.out, encoding));
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

    static class CommandContextImpl implements CommandContext {

        private jline.ConsoleReader console;
        private final CommandHistory history;

        /** whether the session should be terminated*/
        private boolean terminate;

        /** current command line */
        private String cmdLine;
        /** parsed command arguments */
        private DefaultCallbackHandler parsedCmd = new DefaultCallbackHandler(true);

        /** domain or standalone mode*/
        private boolean domainMode;
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
        /** the command line specified username */
        private String username;
        /** the command line specified password */
        private char[] password;
        /** various key/value pairs */
        private Map<String, Object> map = new HashMap<String, Object>();
        /** operation request address prefix */
        private final OperationRequestAddress prefix = new DefaultOperationRequestAddress();
        /** the prefix formatter */
        private final PrefixFormatter prefixFormatter = new DefaultPrefixFormatter();
        /** provider of operation request candidates for tab-completion */
        private final OperationCandidatesProvider operationCandidatesProvider;
        /** operation request handler */
        private final OperationRequestHandler operationHandler;
        /** batches */
        private BatchManager batchManager = new DefaultBatchManager();
        /** the default command completer */
        private final CommandCompleter cmdCompleter;

        /** output target */
        private BufferedWriter outputTarget;

        /**
         * Non-interactive mode
         */
        private CommandContextImpl() {
            this.console = null;
            this.history = null;
            this.operationCandidatesProvider = null;
            this.cmdCompleter = null;
            operationHandler = new OperationRequestHandler();
        }

        /**
         * Interactive mode
         */
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
            operationCandidatesProvider = new DefaultOperationCandidatesProvider();

            operationHandler = new OperationRequestHandler();

            cmdCompleter = new CommandCompleter(cmdRegistry, this);
        }

        @Override
        public String getArgumentsString() {
            if(cmdLine != null && parsedCmd.getOperationName() != null) {
                int cmdNameLength = parsedCmd.getOperationName().length();
                if(cmdLine.length() == cmdNameLength) {
                    return null;
                } else {
                    return cmdLine.substring(cmdNameLength + 1);
                }
            }
            return null;
        }

        @Override
        public void terminateSession() {
            terminate = true;
        }

        @Override
        public void printLine(String message) {
            if(outputTarget != null) {
                try {
                    outputTarget.append(message);
                    outputTarget.newLine();
                    outputTarget.flush();
                } catch (IOException e) {
                    System.err.println("Failed to print '" + message + "' to the output target: " + e.getLocalizedMessage());
                }
                return;
            }

            if (console != null) {
                try {
                    console.printString(message);
                    console.printNewline();
                } catch (IOException e) {
                    System.err.println("Failed to print '" + message + "' to the console: " + e.getLocalizedMessage());
                }
            } else { // non-interactive mode
                System.out.println(message);
            }
        }

        private String readLine(String prompt, boolean password, boolean disableHistory) throws IOException {
            if (console == null) {
                console = initConsoleReader();
            }

            boolean useHistory = console.getUseHistory();
            if (useHistory && disableHistory) {
                console.setUseHistory(false);
            }
            try {
                if (password) {
                    return console.readLine(prompt, (char) 0x00);
                } else {
                    return console.readLine(prompt);
                }

            } finally {
                if (disableHistory && useHistory) {
                    console.setUseHistory(true);
                }
            }
        }

        @Override
        public void printColumns(Collection<String> col) {
            if(outputTarget != null) {
                try {
                    for(String item : col) {
                        outputTarget.append(item);
                        outputTarget.newLine();
                    }
                } catch (IOException e) {
                    System.err.println("Failed to print columns '" + col + "' to the console: " + e.getLocalizedMessage());
                }
                return;
            }

            if (console != null) {
                try {
                    console.printColumns(col);
                } catch (IOException e) {
                    System.err.println("Failed to print columns '" + col + "' to the console: " + e.getLocalizedMessage());
                }
            } else { // non interactive mode
                for(String item : col) {
                    System.out.println(item);
                }
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
        public CommandLineParser getCommandLineParser() {
            return DefaultOperationRequestParser.INSTANCE;
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
                ModelControllerClient newClient = null;

                CallbackHandler cbh = new AuthenticationCallbackHandler(username, password);
                ModelControllerClient tempClient = ModelControllerClient.Factory.create(host, port, cbh);
                switch (initialConnection(tempClient)) {
                    case SUCCESS:
                        newClient = tempClient;
                        break;
                    case CONNECTION_FAILURE:
                        printLine("The controller is not available at " + host + ":" + port);
                        break;
                    case AUTHENTICATION_FAILURE:
                        printLine("Unable to authenticate against controller at " + host + ":" + port);
                        break;
                }

                if (newClient != null) {
                    if (this.client != null) {
                        disconnectController();
                    }

                    client = newClient;
                    this.controllerHost = host;
                    this.controllerPort = port;

                    List<String> nodeTypes = Util.getNodeTypes(newClient, new DefaultOperationRequestAddress());
                    domainMode = nodeTypes.contains("server-group");
                }
            } catch (UnknownHostException e) {
                printLine("Failed to resolve host '" + host + "': " + e.getLocalizedMessage());
            }
        }

        /**
         * Used to make a call to the server to verify that it is possible to connect.
         */
        private ConnectStatus initialConnection(final ModelControllerClient client) {
            try {
                DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
                builder.setOperationName("read-attribute");
                builder.addProperty("name", "name");

                client.execute(builder.buildRequest());
                // We don't actually care what the response is we just want to be sure the ModelControllerClient
                // does not throw an Exception.
                return ConnectStatus.SUCCESS;
            } catch (Exception e) {
                boolean authenticationFailure = false;

                Throwable current = e;
                while (current != null && authenticationFailure == false) {
                    if (current instanceof SaslException) {
                        authenticationFailure = true;
                    }
                    current = current.getCause();
                }

                StreamUtils.safeClose(client);
                return authenticationFailure ? ConnectStatus.AUTHENTICATION_FAILURE : ConnectStatus.CONNECTION_FAILURE;
            }
        }

        @Override
        public void disconnectController() {
            if(this.client != null) {
                StreamUtils.safeClose(client);
                client = null;
                this.controllerHost = null;
                this.controllerPort = -1;
                domainMode = false;
            }
            promptConnectPart = null;
        }

        @Override
        public String getControllerHost() {
            return controllerHost;
        }

        @Override
        public int getControllerPort() {
            return controllerPort;
        }

        @Override
        public void clearScreen() {
            try {
                console.setDefaultPrompt("");// it has to be reset apparently because otherwise it'll be printed twice
                console.clearScreen();
            } catch (IOException e) {
                printLine(e.getLocalizedMessage());
            }
        }

        String promptConnectPart;

        String getPrompt() {
            StringBuilder buffer = new StringBuilder();
            if(promptConnectPart == null) {
                buffer.append('[');
                if (controllerHost != null) {
                    if (domainMode) {
                        buffer.append("domain@");
                    } else {
                        buffer.append("standalone@");
                    }
                    buffer.append(controllerHost).append(':').append(controllerPort).append(' ');
                    promptConnectPart = buffer.toString();
                } else {
                    buffer.append("disconnected ");
                }
            } else {
                buffer.append(promptConnectPart);
            }

            if (prefix.isEmpty()) {
                buffer.append('/');
            } else {
                buffer.append(prefix.getNodeType());
                final String nodeName = prefix.getNodeName();
                if (nodeName != null) {
                    buffer.append('=').append(nodeName);
                }
            }

            if (isBatchMode()) {
                buffer.append(" #");
            }
            buffer.append("] ");
            return buffer.toString();
        }

        @Override
        public CommandHistory getHistory() {
            return history;
        }

        @Override
        public String getDefaultControllerHost() {
            return defaultControllerHost;
        }

        @Override
        public int getDefaultControllerPort() {
            return defaultControllerPort;
        }

        private void resetArgs(String cmdLine) throws CommandFormatException {
            if(cmdLine != null) {
                parsedCmd.parse(prefix, cmdLine);
                setOutputTarget(parsedCmd.getOutputTarget());
            }
            this.cmdLine = cmdLine;
        }

        @Override
        public boolean isBatchMode() {
            return batchManager.isBatchActive();
        }

        @Override
        public BatchManager getBatchManager() {
            return batchManager;
        }

        private final DefaultCallbackHandler tmpBatched = new DefaultCallbackHandler();
        @Override
        public BatchedCommand toBatchedCommand(String line) throws CommandFormatException {

            if (line.isEmpty()) {
                throw new IllegalArgumentException("Null command line.");
            }

            final DefaultCallbackHandler originalParsedArguments = this.parsedCmd;
            try {
                this.parsedCmd = tmpBatched;
                resetArgs(line);
            } catch(CommandFormatException e) {
                this.parsedCmd = originalParsedArguments;
                throw e;
            }

            if(isOperation(line)) {
                try {
                    ModelNode request = this.parsedCmd.toOperationRequest();
                    StringBuilder op = new StringBuilder();
                    op.append(prefixFormatter.format(parsedCmd.getAddress()));
                    op.append(line.substring(line.indexOf(':')));
                    return new DefaultBatchedCommand(op.toString(), request);
                } finally {
                    this.parsedCmd = originalParsedArguments;
                }
            }

            CommandHandler handler = cmdRegistry.getCommandHandler(parsedCmd.getOperationName());
            if(handler == null) {
                throw new OperationFormatException("No command handler for '" + parsedCmd.getOperationName() + "'.");
            }
            if(!(handler instanceof OperationCommand)) {
                throw new OperationFormatException("The command is not allowed in a batch.");
            }

            try {
                ModelNode request = ((OperationCommand)handler).buildRequest(this);
                return new DefaultBatchedCommand(line, request);
            } finally {
                this.parsedCmd = originalParsedArguments;
            }
        }

        @Override
        public CommandLineCompleter getDefaultCommandCompleter() {
            return cmdCompleter;
        }

        @Override
        public ParsedCommandLine getParsedCommandLine() {
            return parsedCmd;
        }

        @Override
        public boolean isDomainMode() {
            return domainMode;
        }

        protected void setOutputTarget(String filePath) {
            if(filePath == null) {
                this.outputTarget = null;
                return;
            }
            FileWriter writer;
            try {
                writer = new FileWriter(filePath, false);
            } catch (IOException e) {
                printLine(e.getLocalizedMessage());
                return;
            }
            this.outputTarget = new BufferedWriter(writer);
        }

        private enum ConnectStatus {
            SUCCESS, AUTHENTICATION_FAILURE, CONNECTION_FAILURE
        }

        private class AuthenticationCallbackHandler implements CallbackHandler {

            // After the CLI has connected the physical connection may be re-established numerous times.
            // for this reason we cache the entered values to allow for re-use without pestering the end
            // user.

            private String realm = null;
            private boolean realmShown = false;

            private String username;
            private char[] password;
            private String digest;

            private AuthenticationCallbackHandler(String username, char[] password) {
                // A local cache is used for scenarios where no values are specified on the command line
                // and the user wishes to use the connect command to establish a new connection.
                this.username = username;
                this.password = password;
            }

            private AuthenticationCallbackHandler(String username, String digest) {
                this.username = username;
                this.digest = digest;
            }

            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                // Special case for anonymous authentication to avoid prompting user for their name.
                if (callbacks.length == 1 && callbacks[0] instanceof NameCallback) {
                    ((NameCallback) callbacks[0]).setName("anonymous CLI user");
                    return;
                }

                for (Callback current : callbacks) {
                    if (current instanceof RealmCallback) {
                        RealmCallback rcb = (RealmCallback) current;
                        String defaultText = rcb.getDefaultText();
                        realm = defaultText;
                        rcb.setText(defaultText); // For now just use the realm suggested.
                    } else if (current instanceof RealmChoiceCallback) {
                        throw new UnsupportedCallbackException(current, "Realm choice not currently supported.");
                    } else if (current instanceof NameCallback) {
                        NameCallback ncb = (NameCallback) current;
                        if (username == null) {
                            showRealm();
                            username = readLine("Username: ", false, true);
                            if (username == null || username.length() == 0) {
                                throw new SaslException("No username supplied.");
                            }
                        }
                        ncb.setName(username);
                    } else if (current instanceof PasswordCallback && digest == null) {
                        // If a digest had been set support for PasswordCallback is disabled.
                        PasswordCallback pcb = (PasswordCallback) current;
                        if (password == null) {
                            showRealm();
                            String temp = readLine("Password: ", true, false);
                            if (temp != null) {
                                password = temp.toCharArray();
                            }
                        }
                        pcb.setPassword(password);
                    } else if (current instanceof DigestHashCallback && digest != null) {
                        // We don't support an interactive use of this callback so it must have been set in advance.
                        DigestHashCallback dhc = (DigestHashCallback) current;
                        dhc.setHexHash(digest);
                    } else {
                        printLine("Unexpected Callback " + current.getClass().getName());
                        throw new UnsupportedCallbackException(current);
                    }
                }
            }

            private void showRealm() {
                if (realmShown == false && realm != null) {
                    realmShown = true;
                    printLine("Authenticating against security realm: " + realm);
                }
            }

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
    }
}
