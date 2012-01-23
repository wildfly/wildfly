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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import javax.security.sasl.SaslException;

import org.jboss.as.cli.ArgumentValueConverter;
import org.jboss.as.cli.CliConfig;
import org.jboss.as.cli.CliEvent;
import org.jboss.as.cli.CliEventListener;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandCompleter;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandHistory;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandRegistry;
import org.jboss.as.cli.OperationCommand;
import org.jboss.as.cli.SSLConfig;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.batch.BatchedCommand;
import org.jboss.as.cli.batch.impl.DefaultBatchManager;
import org.jboss.as.cli.batch.impl.DefaultBatchedCommand;
import org.jboss.as.cli.handlers.ClearScreenHandler;
import org.jboss.as.cli.handlers.CommandCommandHandler;
import org.jboss.as.cli.handlers.ConnectHandler;
import org.jboss.as.cli.handlers.DeployHandler;
import org.jboss.as.cli.handlers.DeploymentInfoHandler;
import org.jboss.as.cli.handlers.GenericTypeOperationHandler;
import org.jboss.as.cli.handlers.HelpHandler;
import org.jboss.as.cli.handlers.HistoryHandler;
import org.jboss.as.cli.handlers.LsHandler;
import org.jboss.as.cli.handlers.OperationRequestHandler;
import org.jboss.as.cli.handlers.PrefixHandler;
import org.jboss.as.cli.handlers.PrintWorkingNodeHandler;
import org.jboss.as.cli.handlers.QuitHandler;
import org.jboss.as.cli.handlers.ReadAttributeHandler;
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
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.OperationCandidatesProvider;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.PrefixFormatter;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationCandidatesProvider;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestParser;
import org.jboss.as.cli.operation.impl.DefaultPrefixFormatter;
import org.jboss.as.cli.operation.impl.RolloutPlanCompleter;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.sasl.callback.DigestHashCallback;
import org.jboss.sasl.util.HexConverter;

/**
 *
 * @author Alexey Loubyansky
 */
class CommandContextImpl implements CommandContext {

    static boolean isOperation(String line) {
        char firstChar = line.charAt(0);
        return firstChar == '.' || firstChar == ':' || firstChar == '/' || line.startsWith("..") || line.startsWith(".type");
    }

    /** the cli configuration */
    private final CliConfig config;

    private final CommandRegistry cmdRegistry = new CommandRegistry();

    private Console console;

    /** whether the session should be terminated */
    private boolean terminate;

    /** current command line */
    private String cmdLine;
    /** parsed command arguments */
    private DefaultCallbackHandler parsedCmd = new DefaultCallbackHandler(true);

    /** domain or standalone mode */
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
    /** The SSLContext when managed by the CLI */
    private SSLContext sslContext;
    /** The TrustManager in use by the SSLContext, a reference is kept to rejected certificates can be captured. */
    private LazyDelagatingTrustManager trustManager;
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

    private List<CliEventListener> listeners = new ArrayList<CliEventListener>();

    /** the value of this variable will be used as the exit code of the vm, it is reset by every command/operation executed */
    private int exitCode;

    /**
     * Version mode - only used when --version is called from the command line.
     *
     * @throws CliInitializationException
     */
    CommandContextImpl() throws CliInitializationException {
        this.console = null;
        this.operationCandidatesProvider = null;
        this.cmdCompleter = null;
        operationHandler = new OperationRequestHandler();
        initCommands();
        config = CliConfigImpl.load(this);
    }

    /**
     * Default constructor used for both interactive and non-interactive mode.
     *
     */
    CommandContextImpl(String defaultControllerHost, int defaultControllerPort, String username, char[] password,
            boolean initConsole) throws CliInitializationException {
        operationHandler = new OperationRequestHandler();

        this.username = username;
        this.password = password;
        if (defaultControllerHost != null) {
            this.defaultControllerHost = defaultControllerHost;
        }
        if (defaultControllerPort != -1) {
            this.defaultControllerPort = defaultControllerPort;
        }
        initCommands();

        config = CliConfigImpl.load(this);
        initSSLContext();

        if (initConsole) {
            cmdCompleter = new CommandCompleter(cmdRegistry);
            this.console = Console.Factory.getConsole(this);
            console.setUseHistory(true);
            console.setHistoryFile(new File(SecurityActions.getSystemProperty("user.home"), ".jboss-cli-history"));
            console.addCompleter(cmdCompleter);
            this.operationCandidatesProvider = new DefaultOperationCandidatesProvider();
        } else {
            this.cmdCompleter = null;
            this.operationCandidatesProvider = null;
        }
    }

    private void initCommands() {
        cmdRegistry.registerHandler(new PrefixHandler(), "cd", "cn");
        cmdRegistry.registerHandler(new ClearScreenHandler(), "clear", "cls");
        cmdRegistry.registerHandler(new CommandCommandHandler(cmdRegistry), "command");
        cmdRegistry.registerHandler(new ConnectHandler(), "connect");
        cmdRegistry.registerHandler(new HelpHandler(), "help", "h");
        cmdRegistry.registerHandler(new HistoryHandler(), "history");
        cmdRegistry.registerHandler(new LsHandler(), "ls");
        cmdRegistry.registerHandler(new PrintWorkingNodeHandler(), "pwd", "pwn");
        cmdRegistry.registerHandler(new QuitHandler(), "quit", "q", "exit");
        cmdRegistry.registerHandler(new ReadAttributeHandler(this), "read-attribute");
        cmdRegistry.registerHandler(new VersionHandler(), "version");

        // deployment
        cmdRegistry.registerHandler(new DeployHandler(this), "deploy");
        cmdRegistry.registerHandler(new UndeployHandler(this), "undeploy");
        cmdRegistry.registerHandler(new DeploymentInfoHandler(this), "deployment-info");

        // batch commands
        cmdRegistry.registerHandler(new BatchHandler(), "batch");
        cmdRegistry.registerHandler(new BatchDiscardHandler(), "discard-batch");
        cmdRegistry.registerHandler(new BatchListHandler(), "list-batch");
        cmdRegistry.registerHandler(new BatchHoldbackHandler(), "holdback-batch");
        cmdRegistry.registerHandler(new BatchRunHandler(), "run-batch");
        cmdRegistry.registerHandler(new BatchClearHandler(), "clear-batch");
        cmdRegistry.registerHandler(new BatchRemoveLineHandler(), "remove-batch-line");
        cmdRegistry.registerHandler(new BatchMoveLineHandler(), "move-batch-line");
        cmdRegistry.registerHandler(new BatchEditLineHandler(), "edit-batch-line");

        // data-source
        cmdRegistry.registerHandler(new GenericTypeOperationHandler(this, "/subsystem=datasources/data-source", null), "data-source");
        cmdRegistry.registerHandler(new GenericTypeOperationHandler(this, "/subsystem=datasources/xa-data-source", null), "xa-data-source");
        // deprecated and hidden from the tab-completion
        cmdRegistry.registerHandler(new DataSourceAddHandler(this), false, "add-data-source");
        cmdRegistry.registerHandler(new DataSourceModifyHandler(this), false, "modify-data-source");
        cmdRegistry.registerHandler(new DataSourceRemoveHandler(this), false, "remove-data-source");
        cmdRegistry.registerHandler(new XADataSourceAddHandler(this), false, "add-xa-data-source");
        cmdRegistry.registerHandler(new XADataSourceRemoveHandler(this), false, "remove-xa-data-source");
        cmdRegistry.registerHandler(new XADataSourceModifyHandler(this), false, "modify-xa-data-source");

        // JMS
        cmdRegistry.registerHandler(new GenericTypeOperationHandler(this, "/subsystem=messaging/hornetq-server=default/jms-queue", "queue-address"), "jms-queue");
        cmdRegistry.registerHandler(new GenericTypeOperationHandler(this, "/subsystem=messaging/hornetq-server=default/jms-topic", "topic-address"), "jms-topic");
        cmdRegistry.registerHandler(new GenericTypeOperationHandler(this, "/subsystem=messaging/hornetq-server=default/connection-factory", null), "connection-factory");
        // deprecated and hidden from the tab-completion
        cmdRegistry.registerHandler(new JmsQueueAddHandler(this), false, "add-jms-queue");
        cmdRegistry.registerHandler(new JmsQueueRemoveHandler(this), false, "remove-jms-queue");
        cmdRegistry.registerHandler(new JmsTopicAddHandler(this), false, "add-jms-topic");
        cmdRegistry.registerHandler(new JmsTopicRemoveHandler(this), false, "remove-jms-topic");
        cmdRegistry.registerHandler(new JmsCFAddHandler(this), false, "add-jms-cf");
        cmdRegistry.registerHandler(new JmsCFRemoveHandler(this), false, "remove-jms-cf");
        // these are used for the cts setup
        cmdRegistry.registerHandler(new CreateJmsResourceHandler(this), false, "create-jms-resource");
        cmdRegistry.registerHandler(new DeleteJmsResourceHandler(this), false, "delete-jms-resource");

        // rollout plan
        final GenericTypeOperationHandler rolloutPlan = new GenericTypeOperationHandler(this, "/management-client-content=rollout-plans/rollout-plan", null);
        rolloutPlan.addValueConverter("content", ArgumentValueConverter.ROLLOUT_PLAN);
        rolloutPlan.addValueCompleter("content", RolloutPlanCompleter.INSTANCE);
        cmdRegistry.registerHandler(rolloutPlan, "rollout-plan");
    }

    public int getExitCode() {
        return exitCode;
    }

    /**
     * Initialise the SSLContext and associated TrustManager for this CommandContext.
     *
     * If no configuration is specified the default mode of operation will be to use a lazily initialised TrustManager with no
     * KeyManager.
     */
    private void initSSLContext() throws CliInitializationException {
        // If the standard properties have been set don't enable and CLI specific stores.
        if (SecurityActions.getSystemProperty("javax.net.ssl.keyStore") != null
                || SecurityActions.getSystemProperty("javax.net.ssl.trustStore") != null) {
            return;
        }

        KeyManager[] keyManagers = null;
        TrustManager[] trustManagers = null;

        String trustStore = null;
        String trustStorePassword = null;
        boolean modifyTrustStore = true;

        SSLConfig sslConfig = config.getSslConfig();
        if (sslConfig != null) {
            String keyStoreLoc = sslConfig.getKeyStore();
            if (keyStoreLoc != null) {
                char[] keyStorePassword = sslConfig.getKeyStorePassword().toCharArray();

                File keyStoreFile = new File(keyStoreLoc);

                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(keyStoreFile);
                    KeyStore theKeyStore = KeyStore.getInstance("JKS");
                    theKeyStore.load(fis, keyStorePassword);

                    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
                    keyManagerFactory.init(theKeyStore, keyStorePassword);
                    keyManagers = keyManagerFactory.getKeyManagers();
                } catch (IOException e) {
                    throw new CliInitializationException(e);
                } catch (GeneralSecurityException e) {
                    throw new CliInitializationException(e);
                } finally {
                    StreamUtils.safeClose(fis);
                }

            }

            trustStore = sslConfig.getTrustStore();
            trustStorePassword = sslConfig.getTrustStorePassword();
            modifyTrustStore = sslConfig.isModifyTrustStore();
        }

        if (trustStore == null) {
            final String userHome = SecurityActions.getSystemProperty("user.home");
            File trustStoreFile = new File(userHome, ".jboss-cli.truststore");
            trustStore = trustStoreFile.getAbsolutePath();
            trustStorePassword = "cli_truststore"; // Risk of modification but no private keys to be stored in the truststore.
        }

        trustManager = new LazyDelagatingTrustManager(trustStore, trustStorePassword, modifyTrustStore);
        trustManagers = new TrustManager[] { trustManager };

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);

            this.sslContext = sslContext;
        } catch (GeneralSecurityException e) {
            throw new CliInitializationException(e);
        }
    }

    @Override
    public boolean isTerminated() {
        return terminate;
    }

    void processLine(String line) {
        if (line.isEmpty() || line.charAt(0) == '#') {
            return; // ignore comments
        }

        exitCode = 0;
        if (isOperation(line)) {

            ModelNode request;
            try {
                resetArgs(line);
                request = parsedCmd.toOperationRequest(this);
            } catch (CommandFormatException e) {
                error(e.getLocalizedMessage());
                return;
            }

            if (isBatchMode()) {
                StringBuilder op = new StringBuilder();
                op.append(getPrefixFormatter().format(parsedCmd.getAddress()));
                op.append(line.substring(line.indexOf(':')));
                DefaultBatchedCommand batchedCmd = new DefaultBatchedCommand(op.toString(), request);
                Batch batch = getBatchManager().getActiveBatch();
                batch.add(batchedCmd);
                printLine("#" + batch.size() + " " + batchedCmd.getCommand());
            } else {
                set("OP_REQ", request);
                try {
                    operationHandler.handle(this);
                } finally {
                    set("OP_REQ", null);
                }
            }

        } else {
            try {
                resetArgs(line);
            } catch (CommandFormatException e1) {
                error(e1.getLocalizedMessage());
                return;
            }

            final String cmdName = parsedCmd.getOperationName();
            CommandHandler handler = cmdRegistry.getCommandHandler(cmdName.toLowerCase());
            if (handler != null) {
                if (isBatchMode() && handler.isBatchMode()) {
                    if (!(handler instanceof OperationCommand)) {
                        error("The command is not allowed in a batch.");
                    } else {
                        try {
                            ModelNode request = ((OperationCommand) handler).buildRequest(this);
                            BatchedCommand batchedCmd = new DefaultBatchedCommand(line, request);
                            Batch batch = getBatchManager().getActiveBatch();
                            batch.add(batchedCmd);
                            printLine("#" + batch.size() + " " + batchedCmd.getCommand());
                        } catch (CommandFormatException e) {
                            error("Failed to add to batch: " + e.getLocalizedMessage());
                        }
                    }
                } else {
                    try {
                        handler.handle(this);
                    } catch (CommandFormatException e) {
                        error(e.getLocalizedMessage());
                    }
                }

                // TODO this doesn't make sense
                try {
                    resetArgs(null);
                } catch (CommandFormatException e) {
                }
            } else {
                error("Unexpected command '" + line + "'. Type 'help --commands' for the list of supported commands.");
            }
        }
    }

    @Override
    public String getArgumentsString() {
        if (cmdLine != null && parsedCmd.getOperationName() != null) {
            int cmdNameLength = parsedCmd.getOperationName().length();
            if (cmdLine.length() == cmdNameLength) {
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
        if (outputTarget != null) {
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
            console.print(message);
            console.printNewLine();
        } else { // non-interactive mode
            System.out.println(message);
        }
    }

    @Override
    public void error(String message, int code) {
        this.exitCode = code;
        printLine(message);
    }

    @Override
    public void error(String message) {
        error(message, 1);
    }

    private String readLine(String prompt, boolean password, boolean disableHistory) throws IOException {
        if (console == null) {
            console = Console.Factory.getConsole(this);
        }

        boolean useHistory = console.isUseHistory();
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
        if (outputTarget != null) {
            try {
                for (String item : col) {
                    outputTarget.append(item);
                    outputTarget.newLine();
                }
            } catch (IOException e) {
                System.err.println("Failed to print columns '" + col + "' to the console: " + e.getLocalizedMessage());
            }
            return;
        }

        if (console != null) {
            console.printColumns(col);
        } else { // non interactive mode
            for (String item : col) {
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
        if (host == null) {
            host = defaultControllerHost;
        }

        if (port < 0) {
            port = defaultControllerPort;
        }

        boolean retry;
        do {
            retry = false;
            try {
                ModelControllerClient newClient = null;

                CallbackHandler cbh = new AuthenticationCallbackHandler(username, password);
                ModelControllerClient tempClient = ModelControllerClient.Factory.create(host, port, cbh, sslContext);
                switch (initialConnection(tempClient)) {
                    case SUCCESS:
                        newClient = tempClient;
                        break;
                    case CONNECTION_FAILURE:
                        error("The controller is not available at " + host + ":" + port);
                        break;
                    case AUTHENTICATION_FAILURE:
                        error("Unable to authenticate against controller at " + host + ":" + port);
                        break;
                    case SSL_FAILURE:
                        try {
                            retry = handleSSLFailure();
                        } catch (IOException ignored) {
                        }
                        if (retry == false) {
                            error("Unable to negotiate SSL connection with controller at " + host + ":" + port);
                        }
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
                    domainMode = nodeTypes.contains(Util.SERVER_GROUP);
                }
            } catch (UnknownHostException e) {
                error("Failed to resolve host '" + host + "': " + e.getLocalizedMessage());
            }
        } while (retry);
    }

    /**
     * Handle the last SSL failure, prompting the user to accept or reject the certificate of the remote server.
     *
     * @return true if the connection should be retried.
     */
    private boolean handleSSLFailure() throws IOException {
        Certificate[] lastChain;
        if (trustManager == null || (lastChain = trustManager.getLastFailedCertificateChain()) == null) {
            return false;
        }
        error("Unable to connect due to unrecognised server certificate");
        for (Certificate current : lastChain) {
            if (current instanceof X509Certificate) {
                X509Certificate x509Current = (X509Certificate) current;
                Map<String, String> fingerprints = generateFingerprints(x509Current);
                printLine("Subject    - " + x509Current.getSubjectX500Principal().getName());
                printLine("Issuer     - " + x509Current.getIssuerDN().getName());
                printLine("Valid From - " + x509Current.getNotBefore());
                printLine("Valid To   - " + x509Current.getNotAfter());
                for (String alg : fingerprints.keySet()) {
                    printLine(alg + " : " + fingerprints.get(alg));
                }
                printLine("");
            }
        }

        for (;;) {
            String response;
            if (trustManager.isModifyTrustStore()) {
                response = readLine("Accept certificate? [N]o, [T]emporarily, [P]ermenantly : ", false, true);
            } else {
                response = readLine("Accept certificate? [N]o, [T]emporarily : ", false, true);
            }

            if (response != null && response.length() == 1) {
                switch (response.toLowerCase().charAt(0)) {
                    case 'n':
                        return false;
                    case 't':
                        trustManager.storeChainTemporarily(lastChain);
                        return true;
                    case 'p':
                        if (trustManager.isModifyTrustStore()) {
                            trustManager.storeChainPermenantly(lastChain);
                            return true;
                        }

                }
            }
        }
    }

    private static final String[] FINGERPRINT_ALOGRITHMS = new String[] { "MD5", "SHA1" };

    private Map<String, String> generateFingerprints(final X509Certificate cert) throws IOException  {
        Map<String, String> fingerprints = new HashMap<String, String>(FINGERPRINT_ALOGRITHMS.length);
        for (String current : FINGERPRINT_ALOGRITHMS) {
            try {
                fingerprints.put(current, generateFingerPrint(current, cert.getEncoded()));
            } catch (GeneralSecurityException e) {
                throw new IOException("Unable to generate fingerprint", e);
            }
        }

        return fingerprints;
    }

    private String generateFingerPrint(final String algorithm, final byte[] cert) throws GeneralSecurityException {
        StringBuilder sb = new StringBuilder();

        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] digested = md.digest(cert);
        String hex = HexConverter.convertToHexString(digested);
        boolean started = false;
        for (int i = 0; i < hex.length() - 1; i += 2) {
            if (started) {
                sb.append(":");
            } else {
                started = true;
            }
            sb.append(hex.substring(i, i + 2));
        }

        return sb.toString();
    }

    /**
     * Used to make a call to the server to verify that it is possible to connect.
     */
    private ConnectStatus initialConnection(final ModelControllerClient client) {
        try {
            DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
            builder.setOperationName(Util.READ_ATTRIBUTE);
            builder.addProperty(Util.NAME, Util.NAME);

            client.execute(builder.buildRequest());
            // We don't actually care what the response is we just want to be sure the ModelControllerClient
            // does not throw an Exception.
            return ConnectStatus.SUCCESS;
        } catch (Exception e) {
            try {
                Throwable current = e;
                while (current != null) {
                    if (current instanceof SaslException) {
                        return ConnectStatus.AUTHENTICATION_FAILURE;
                    }
                    if (current instanceof SSLException) {
                        return ConnectStatus.SSL_FAILURE;
                    }
                    current = current.getCause();
                }

                // We don't know what happened, most likely a timeout.
                return ConnectStatus.CONNECTION_FAILURE;
            } finally {
                StreamUtils.safeClose(client);
            }
        }
    }

    @Override
    public void disconnectController() {
        if (this.client != null) {
            StreamUtils.safeClose(client);
            // if(loggingEnabled) {
            // printLine("Closed connection to " + this.controllerHost + ':' +
            // this.controllerPort);
            // }
            client = null;
            this.controllerHost = null;
            this.controllerPort = -1;
            domainMode = false;
        }
        promptConnectPart = null;
        notifyListeners(CliEvent.DISCONNECTED);
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
        console.clearScreen();
    }

    String promptConnectPart;

    String getPrompt() {
        StringBuilder buffer = new StringBuilder();
        if (promptConnectPart == null) {
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
        return console.getHistory();
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
        if (cmdLine != null) {
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
        } catch (CommandFormatException e) {
            this.parsedCmd = originalParsedArguments;
            throw e;
        }

        if (isOperation(line)) {
            try {
                ModelNode request = this.parsedCmd.toOperationRequest(this);
                StringBuilder op = new StringBuilder();
                op.append(prefixFormatter.format(parsedCmd.getAddress()));
                op.append(line.substring(line.indexOf(':')));
                return new DefaultBatchedCommand(op.toString(), request);
            } finally {
                this.parsedCmd = originalParsedArguments;
            }
        }

        CommandHandler handler = cmdRegistry.getCommandHandler(parsedCmd.getOperationName());
        if (handler == null) {
            throw new OperationFormatException("No command handler for '" + parsedCmd.getOperationName() + "'.");
        }
        if (!(handler instanceof OperationCommand)) {
            throw new OperationFormatException("The command is not allowed in a batch.");
        }

        try {
            ModelNode request = ((OperationCommand) handler).buildRequest(this);
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

    @Override
    public void addEventListener(CliEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener is null.");
        }
        listeners.add(listener);
    }

    @Override
    public CliConfig getConfig() {
        return config;
    }

    protected void setOutputTarget(String filePath) {
        if (filePath == null) {
            this.outputTarget = null;
            return;
        }
        FileWriter writer;
        try {
            writer = new FileWriter(filePath, false);
        } catch (IOException e) {
            error(e.getLocalizedMessage());
            return;
        }
        this.outputTarget = new BufferedWriter(writer);
    }

    protected void notifyListeners(CliEvent event) {
        for (CliEventListener listener : listeners) {
            listener.cliEvent(event, this);
        }
    }

    void interact(boolean connect) {
        SecurityActions.addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                disconnectController();
            }
        }));

        if (connect) {
            connectController(null, -1);
        } else {
            printLine("You are disconnected at the moment." + " Type 'connect' to connect to the server or"
                    + " 'help' for the list of supported commands.");
        }

        try {
            while (!isTerminated()) {
                final String line = console.readLine(getPrompt());
                if (line == null) {
                    terminateSession();
                } else {
                    processLine(line.trim());
                }
            }
            printLine("");
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            disconnectController();
        }
    }

    private enum ConnectStatus {
        SUCCESS, AUTHENTICATION_FAILURE, SSL_FAILURE, CONNECTION_FAILURE
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
                    error("Unexpected Callback " + current.getClass().getName());
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

    /**
     * A trust manager that by default delegates to a lazily initialised TrustManager, this TrustManager also support both
     * temporarily and permenantly accepting unknown server certificate chains.
     *
     * This class also acts as an agregation of the configuration related to TrustStore handling.
     *
     * It is not intended that Certificate management requests occur if this class is registered to a SSLContext
     * with multiple concurrent clients.
     */
    private class LazyDelagatingTrustManager implements X509TrustManager {

        // Configuration based state set on initialisation.

        private final String trustStore;
        private final String trustStorePassword;
        private final boolean modifyTrustStore;

        private Set<X509Certificate> temporarilyTrusted = new HashSet<X509Certificate>();
        private Certificate[] lastFailedCert;
        private X509TrustManager delegate;

        LazyDelagatingTrustManager(String trustStore, String trustStorePassword, boolean modifyTrustStore) {
            this.trustStore = trustStore;
            this.trustStorePassword = trustStorePassword;
            this.modifyTrustStore = modifyTrustStore;
        }

        /*
         * Methods to allow client interaction for certificate verification.
         */

        boolean isModifyTrustStore() {
            return modifyTrustStore;
        }

        void setFailedCertChain(final Certificate[] chain) {
            this.lastFailedCert = chain;
        }

        Certificate[] getLastFailedCertificateChain() {
            try {
                return lastFailedCert;
            } finally {
                // Only one chance to accept it.
                lastFailedCert = null;
            }
        }

        synchronized void storeChainTemporarily(final Certificate[] chain) {
            for (Certificate current : chain) {
                if (current instanceof X509Certificate) {
                    temporarilyTrusted.add((X509Certificate) current);
                }
            }
            delegate = null; // Triggers a reload on next use.
        }

        synchronized void storeChainPermenantly(final Certificate[] chain) {
            FileInputStream fis = null;
            FileOutputStream fos = null;
            try {
                KeyStore theTrustStore = KeyStore.getInstance("JKS");
                File trustStoreFile = new File(trustStore);
                if (trustStoreFile.exists()) {
                    fis = new FileInputStream(trustStoreFile);
                    theTrustStore.load(fis, trustStorePassword.toCharArray());
                    StreamUtils.safeClose(fis);
                    fis = null;
                } else {
                    theTrustStore.load(null);
                }
                for (Certificate current : chain) {
                    if (current instanceof X509Certificate) {
                        X509Certificate x509Current = (X509Certificate) current;
                        theTrustStore.setCertificateEntry(x509Current.getSubjectX500Principal().getName(), x509Current);
                    }
                }

                fos = new FileOutputStream(trustStoreFile);
                theTrustStore.store(fos, trustStorePassword.toCharArray());

            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Unable to operate on trust store.", e);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to operate on trust store.", e);
            } finally {
                StreamUtils.safeClose(fis);
                StreamUtils.safeClose(fos);
            }

            delegate = null; // Triggers a reload on next use.
        }

        /*
         * Internal Methods
         */

        private synchronized X509TrustManager getDelegate() {
            if (delegate == null) {
                FileInputStream fis = null;
                try {
                    KeyStore theTrustStore = KeyStore.getInstance("JKS");
                    File trustStoreFile = new File(trustStore);
                    if (trustStoreFile.exists()) {
                        fis = new FileInputStream(trustStoreFile);
                        theTrustStore.load(fis, trustStorePassword.toCharArray());
                    } else {
                        theTrustStore.load(null);
                    }
                    for (X509Certificate current : temporarilyTrusted) {
                        theTrustStore.setCertificateEntry(current.getSubjectX500Principal().getName(), current);
                    }
                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
                    trustManagerFactory.init(theTrustStore);
                    TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                    for (TrustManager current : trustManagers) {
                        if (current instanceof X509TrustManager) {
                            delegate = (X509TrustManager) current;
                            break;
                        }
                    }
                } catch (GeneralSecurityException e) {
                    throw new IllegalStateException("Unable to operate on trust store.", e);
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to operate on trust store.", e);
                } finally {
                    StreamUtils.safeClose(fis);
                }
            }
            if (delegate == null) {
                throw new IllegalStateException("Unable to create delegate trust manager.");
            }

            return delegate;
        }

        /*
         * X509TrustManager Methods
         */

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // The CLI is only verifying servers.
            getDelegate().checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                getDelegate().checkServerTrusted(chain, authType);
            } catch (CertificateException ce) {
                setFailedCertChain(chain);
                throw ce;
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return getDelegate().getAcceptedIssuers();
        }

    }
}