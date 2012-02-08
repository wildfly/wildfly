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
package org.jboss.as.demos.domain.interactive.runner;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.domain.DeploymentActionsCompleteBuilder;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlan;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlanResult;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.client.helpers.domain.DomainDeploymentManager;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.ServerIdentity;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.controller.client.helpers.domain.UndeployDeploymentPlanBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.demos.DemoAuthentication;
import org.jboss.as.demos.DomainDeploymentUtils;
import org.jboss.as.demos.fakejndi.FakeJndi;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Demonstration of basic aspects of administering servers via the domain management API.
 *
 * @author Brian Stansberry
 */
public class ExampleRunner implements Runnable {

    private static final PrintStream stdout = System.out;

    private static int addCount;

    private static final Map<String, MainMenu> mainMenuByCmd = new HashMap<String, MainMenu>();
    private static final Map<String, DeploymentActionsMenu> deploymentActionMenuByCmd =
            new HashMap<String, DeploymentActionsMenu>();
    private static final Map<String, DeploymentPlanMenu> deploymentPlanMenuByCmd =
            new HashMap<String, DeploymentPlanMenu>();

    static {
        for (MainMenu cmd : MainMenu.ALL) {
            mainMenuByCmd.put(cmd.getCommand(), cmd);
        }
        for (DeploymentActionsMenu cmd : DeploymentActionsMenu.ALL) {
            deploymentActionMenuByCmd.put(cmd.getCommand(), cmd);
        }
        for (DeploymentPlanMenu cmd : DeploymentPlanMenu.ALL) {
            deploymentPlanMenuByCmd.put(cmd.getCommand(), cmd);
        }
    }

    private final DomainClient client;
    private final Reader stdin;
    private String swingLAF;


    private ExampleRunner(InetAddress address, int port) {
        this.client = DomainClient.Factory.create(address, port, DemoAuthentication.getCallbackHandler());
        this.stdin = new InputStreamReader(System.in);
    }

    @Override
    public void run() {

        try {
            boolean quit = false;
            do {
                quit = mainMenu();
            }
            while (!quit);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        } finally {

            stdout.println("Closing connection to domain controller");

            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        stdout.println("Done");

    }

    private boolean mainMenu() throws Exception {

        do {
            writeMenu(MainMenu.ALL);
            String choice = readStdIn();
            MainMenu cmd = mainMenuByCmd.get(choice.toUpperCase());
            if (cmd == null) {
                stdout.println(choice + " is not a valid selection.\n");
            } else {
                boolean quit = false;
                switch (cmd) {
                    case DOMAIN_CFG: {
                        quit = dumpDomainConfig();
                        break;
                    }
                    case LIST_HCS: {
                        quit = listHostControllers();
                        break;
                    }
                    case HC_CFG: {
                        quit = dumpHostController();
                        break;
                    }
                    case LIST_SERVERS: {
                        quit = listServers();
                        break;
                    }
                    case SERVER_CFG: {
                        quit = dumpServer();
                        break;
                    }
                    case SERVER_STOP: {
                        quit = stopServer();
                        break;
                    }
                    case SERVER_START: {
                        quit = startServer();
                        break;
                    }
                    case SERVER_RESTART: {
                        quit = restartServer();
                        break;
                    }
                    case ADD_SERVER: {
                        quit = addServer();
                        break;
                    }
                    case REMOVE_SERVER: {
                        quit = removeServer();
                        break;
                    }
                    case DEPLOYMENTS: {
                        quit = runDeploymentPlan();
                        break;
                    }
                    case ADD_JMS_QUEUE: {
                        quit = addJmsQueue();
                        break;
                    }
                    case QUIT: {
                        quit = true;
                        break;
                    }
                    default: {
                        stdout.println("Command " + cmd.getCommand() + " is not supported");
                    }
                }

                if (quit)
                    break;
            }
        }
        while (true);

        return true;
    }

    private void writeMenu(EnumSet<? extends MenuItem> menu) {
        stdout.println("\nEnter a selection from the following choices:");
        for (MenuItem item : menu) {
            stdout.println(item.getPrompt());
        }
    }

    private Map<String, Object> writeMenuBody(List<?> choices) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (int i = 0; i < choices.size(); i++) {
            StringBuilder sb = new StringBuilder("[");
            sb.append(i + 1);
            sb.append(']');
            if (i < 9) {
                sb.append("   ");
            } else {
                sb.append("  ");
            }
            Object choice = choices.get(i);
            sb.append(choice);
            result.put(String.valueOf(i + 1), choice);
            stdout.println(sb.toString());
        }
        return result;
    }

    private boolean continuePrompt() throws IOException {
        stdout.println("\nHit Enter to continue or Q to quit:");
        String choice = readStdIn();
        return "Q".equals(choice.toUpperCase());
    }

    private boolean dumpDomainConfig() throws Exception {

        ModelNode op = new ModelNode();
        op.get("operation").set("read-config-as-xml");
        stdout.println(executeForResult(new OperationBuilder(op).build()).asString());
        return continuePrompt();
    }

    private boolean listHostControllers() throws Exception {

        stdout.println("\nReading the list of active host controller s:\n");
        List<String> hostControllers = client.getHostControllerNames();
        for (String hc : hostControllers) {
            stdout.println(hc);
        }
        return continuePrompt();
    }

    private boolean dumpHostController() throws Exception {
        List<String> hostControllers = client.getHostControllerNames();
        if (hostControllers.size() == 0) {
            // this isn't possible :-)
            stdout.println("No host controllers available");
        } else if (hostControllers.size() == 1) {
            writeHostController(hostControllers.get(0));
        } else {
            stdout.println("Choose a Host Controller:");
            Map<String, Object> choices = writeMenuBody(hostControllers);
            stdout.println("[C]   Cancel");
            String choice = readStdIn();
            if (!"C".equals(choice.toUpperCase())) {
                Object hc = choices.get(choice);
                if (hc != null) {
                    writeHostController(hc.toString());
                } else {
                    stdout.println(choice + " is not a valid selection");
                    return dumpHostController();
                }
            }
        }
        return continuePrompt();
    }

    private void writeHostController(String hc) throws Exception {

        ModelNode op = new ModelNode();
        op.get("operation").set("read-config-as-xml");
        op.get("address").add("host", hc);
        stdout.println(executeForResult(new OperationBuilder(op).build()).asString());
    }

    private boolean listServers() throws Exception {
        stdout.println("\nReading the list of configured servers:");
        for (Map.Entry<ServerIdentity, ServerStatus> server : client.getServerStatuses().entrySet()) {
            ServerIdentity id = server.getKey();
            stdout.println("\nServer:\n");
            stdout.println("server name:         " + id.getServerName());
            stdout.println("host controller name: " + id.getHostName());
            stdout.println("server group name:   " + id.getServerGroupName());
            stdout.println("status:              " + server.getValue());
        }
        return continuePrompt();
    }

    private boolean dumpServer() throws Exception {

        ServerIdentity server = chooseServer(ServerStatus.STARTED);
        if (server != null) {
            stdout.println("\nReading runtime configuration for " + server.getServerName() + "\n");

            ModelNode op = new ModelNode();
            op.get("operation").set("read-config-as-xml");
            ModelNode address = op.get("address");
            address.add("host", server.getHostName());
            address.add("server", server.getServerName());
            stdout.println(executeForResult(new OperationBuilder(op).build()).asString());
        }
        return continuePrompt();
    }

    private boolean stopServer() throws Exception {
        ServerIdentity server = chooseServer(ServerStatus.STARTED);
        if (server != null) {
            System.out.println("\nStopping server " + server.getServerName() + "\n");
            ServerStatus status = client.stopServer(server.getHostName(), server.getServerName(), -1, TimeUnit.SECONDS);
            System.out.println("Stop executed. Server status is " + status);
        }
        return continuePrompt();
    }

    private boolean startServer() throws Exception {
        ServerIdentity server = chooseServer(ServerStatus.STOPPED, ServerStatus.DISABLED);
        if (server != null) {
            System.out.println("\nStarting server " + server.getServerName() + "\n");
            ServerStatus status = client.startServer(server.getHostName(), server.getServerName());
            System.out.println("Start executed. Server status is " + status);
        }
        return continuePrompt();
    }

    private boolean restartServer() throws Exception {
        ServerIdentity server = chooseServer(ServerStatus.STARTED);
        if (server != null) {
            System.out.println("\nRestarting server " + server.getServerName() + "\n");
            ServerStatus status = client.restartServer(server.getHostName(), server.getServerName(), -1, TimeUnit.SECONDS);
            System.out.println("Restart executed. Server status is " + status);
        }
        return continuePrompt();
    }

    private boolean addServer() throws Exception {

        addCount++;
        stdout.println("Enter the name of the new server, or [C] to cancel:");
        String serverName = readStdIn();
        if ("C".equals(serverName.toUpperCase())) {
            return continuePrompt();
        }
        String hostController = null;
        List<String> hostControllers = client.getHostControllerNames();
        if (hostControllers.size() == 1) {
            hostController = hostControllers.get(0);
        } else {
            do {
                stdout.println("Choose a Host Controller for the new Server:");
                Map<String, Object> choices = writeMenuBody(hostControllers);
                stdout.println("[C]   Cancel");
                String choice = readStdIn();
                if ("C".equals(choice.toUpperCase())) {
                    return continuePrompt();
                }
                Object obj = choices.get(choice);

                if (obj == null) {
                    stdout.println(choice + " is not a valid selection");
                } else {
                    hostController = obj.toString();
                }
            }
            while (hostController == null);
        }

        String serverGroup = null;
        List<String> serverGroups = getServerGroupNames();
        do {
            stdout.println("Choose a Server Group for the new Server:");
            Map<String, Object> choices = writeMenuBody(serverGroups);
            stdout.println("[C]   Cancel");
            String choice = readStdIn();
            if ("C".equals(choice.toUpperCase())) {
                return continuePrompt();
            }
            Object obj = choices.get(choice);

            if (obj == null) {
                stdout.println(choice + " is not a valid selection");
            } else {
                serverGroup = obj.toString();
            }

        }
        while (serverGroup == null);

        stdout.println("\nCreating new server: '" + serverName + "' on host controller '" + hostController + "' in server group: '" + serverGroup);

        final ModelNode address = new ModelNode();
        address.add(HOST, hostController);
        address.add(SERVER_CONFIG, serverName);

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);
        operation.get(GROUP).set(serverGroup);
        operation.get(SOCKET_BINDING_GROUP).set("standard-sockets");
        operation.get(SOCKET_BINDING_PORT_OFFSET).set(addCount * 500);

        final ModelNode result = executeForResult(operation);

        return continuePrompt();
//
//
//        List<AbstractHostModelUpdate<?>> updates = new ArrayList<AbstractHostModelUpdate<?>>(2);
//        updates.add(new HostServerAdd(serverName, serverGroup));
//        updates.add(HostServerUpdate.create(serverName, new ServerElementSocketBindingGroupUpdate("standard-sockets")));
//        addCount++;
//        updates.add(HostServerUpdate.create(serverName, new ServerElementSocketBindingPortOffsetUpdate(addCount * 1000)));
//        List<HostUpdateResult<?>> results = client.applyHostUpdates(hostController, updates);
//        HostUpdateResult<?> result = results.get(0);
//        System.out.println("Add success: " + result.isSuccess());
//
//        if(result.isSuccess()) {
//            System.out.println("Starting server " + serverName);
//            ServerStatus status = client.startServer(hostController, serverName);
//            System.out.println("Start executed. Server status is " + status);
//        }
//        return continuePrompt();
    }

    private List<String> getServerGroupNames() {

        SortedSet<String> sorted = new TreeSet<String>();
        ModelNode domainModel = getDomainModel();
        if (domainModel.hasDefined("server-group")) {
            sorted.addAll(domainModel.get("server-group").keys());
        }
        return new ArrayList<String>(sorted);
    }

    private ModelNode getDomainModel() {
        ModelNode op = new ModelNode();
        op.get("operation").set("read-resource");
        op.get("recursive").set(true);
        op.get("proxies").set(false);
        return executeForResult(new OperationBuilder(op).build());
    }

    private boolean removeServer() throws Exception {

        ServerIdentity server = chooseServer(ServerStatus.STOPPED, ServerStatus.DISABLED);
        if (server != null) {
            stdout.println("Removing server " + server.getServerName());
            ModelNode op = new ModelNode();
            op.get("operation").set("remove");
            ModelNode address = op.get("address");
            address.add("host", server.getHostName());
            address.add("server-config", server.getServerName());
            boolean success = true;
            try {
                executeForResult(new OperationBuilder(op).build());
            } catch (Exception e) {
                success = false;
            }
            stdout.println("Remove success: " + success);
        }
        return continuePrompt();
    }

    private ServerIdentity chooseServer(ServerStatus valid, ServerStatus... alsoValid) throws IOException {
        ServerIdentity result = null;
        SortedMap<String, ServerIdentity> servers = getValidServers(valid, alsoValid);
        if (servers.size() == 0) {
            StringBuilder sb = new StringBuilder("No servers are in a valid state to perform this operation. Servers must have status ");
            sb.append(valid);
            if (alsoValid != null) {
                for (ServerStatus status : alsoValid) {
                    sb.append(", ");
                    sb.append(status);
                }
            }
            stdout.println(sb.toString());
        } else {
            stdout.println("Choose a Server:");
            Map<String, Object> choices = writeMenuBody(new ArrayList<String>(servers.keySet()));
            stdout.println("[C]   Cancel");
            String choice = readStdIn();
            if (!"C".equals(choice.toUpperCase())) {
                Object hc = choices.get(choice);
                if (hc != null) {
                    result = servers.get(hc.toString());
                } else {
                    stdout.println(choice + " is not a valid selection");
                    result = chooseServer(valid, alsoValid);
                }
            }
        }
        return result;
    }

    private SortedMap<String, ServerIdentity> getValidServers(ServerStatus valid, ServerStatus... alsoValid) {
        Set<ServerStatus> validSet = EnumSet.of(valid, alsoValid);
        SortedMap<String, ServerIdentity> result = new TreeMap<String, ServerIdentity>();
        for (Map.Entry<ServerIdentity, ServerStatus> entry : client.getServerStatuses().entrySet()) {
            if (validSet.contains(entry.getValue())) {
                result.put(entry.getKey().getServerName(), entry.getKey());
            }
        }
        return result;
    }

    private boolean runDeploymentPlan() throws Exception {

        DomainDeploymentManager deploymentManager = client.getDeploymentManager();
        DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
        ModelNode model = getDomainModel();
        DeploymentActionsCompleteBuilder completionBuilder = null;
        String serverGroup = null;
        Set<String> includedGroups = new HashSet<String>();
        do {
            completionBuilder = deploymentSetBuilder(builder, model);
            if (completionBuilder != null) {
                serverGroup = chooseServerGroup(model, includedGroups);
            }
        }
        while (serverGroup == null && completionBuilder != null);

        if (completionBuilder != null) {
            includedGroups.add(serverGroup);
            ServerGroupDeploymentPlanBuilder groupPlanBuilder = completionBuilder.toServerGroup(serverGroup);
            DeploymentPlan plan = completeDeploymentPlan(groupPlanBuilder, model, includedGroups);
            if (plan != null) {
                Future<DeploymentPlanResult> future = deploymentManager.execute(plan);
                writeDeploymentPlanResult(future.get());
            }
        }
        return continuePrompt();
    }

    private String chooseServerGroup(ModelNode model, Set<String> existingGroups) throws IOException {
        List<String> groups = getServerGroupNames();
        groups.removeAll(existingGroups);
        System.out.println(groups + " // " + existingGroups);
        String serverGroup = null;
        do {
            stdout.println("Choose a Server Group:");
            Map<String, Object> choices = writeMenuBody(groups);
            stdout.println("[C]   Cancel");
            String choice = readStdIn();
            if ("C".equals(choice.toUpperCase())) {
                break;
            }
            Object obj = choices.get(choice);

            if (obj == null) {
                stdout.println(choice + " is not a valid selection");
            } else {
                serverGroup = obj.toString();
            }

        }
        while (serverGroup == null);
        return serverGroup;
    }

    private DeploymentActionsCompleteBuilder deploymentSetBuilder(DeploymentPlanBuilder builder, ModelNode model) throws Exception {

        // Vars to track differences between the model and what our actions will have done
        Set<String> addedContent = new HashSet<String>();
        Set<String> deployedContent = new HashSet<String>();
        Set<String> undeployedContent = new HashSet<String>();
        Set<String> removedContent = new HashSet<String>();
        do {
            boolean hasActions = (builder instanceof DeploymentActionsCompleteBuilder);
            writeMenu(hasActions ? DeploymentActionsMenu.ALL : DeploymentActionsMenu.INITIAL);
            String choice = readStdIn();
            DeploymentActionsMenu cmd = deploymentActionMenuByCmd.get(choice.toUpperCase());
            if (cmd == null) {
                stdout.println(choice + " is not a valid selection.\n");
            } else {
                switch (cmd) {
                    case ADD:
                    case ADD_AND_DEPLOY:
                    case ADD_AND_REPLACE: {
                        builder = addContent(builder, addedContent, deployedContent, undeployedContent, removedContent, model, cmd);
                        break;
                    }
                    case DEPLOY: {
                        builder = deployContent(builder, addedContent, deployedContent, undeployedContent, removedContent, model);
                        break;
                    }
                    case REPLACE: {
                        builder = replaceContent(builder, addedContent, deployedContent, undeployedContent, removedContent, model);
                        break;
                    }
                    case UNDEPLOY:
                    case UNDEPLOY_AND_REMOVE: {
                        builder = undeployContent(builder, addedContent, deployedContent, undeployedContent, removedContent, model, cmd);
                        break;
                    }
                    case REMOVE: {
                        builder = removeContent(builder, addedContent, deployedContent, undeployedContent, removedContent, model);
                        break;
                    }
                    case APPLY: {
                        if (hasActions) {
                            return (DeploymentActionsCompleteBuilder) builder;
                        } else {
                            stdout.println(choice + " is not a valid selection.\n");
                        }
                        break;
                    }
                    case CANCEL: {
                        return null;
                    }
                    default: {
                        stdout.println("Command " + cmd.getCommand() + " is not supported");
                    }
                }
            }
        }
        while (true);
    }

    private DeploymentPlanBuilder addContent(DeploymentPlanBuilder builder, Set<String> addedContent,
                                             Set<String> deployedContent, Set<String> undeployedContent, Set<String> removedContent,
                                             ModelNode model, DeploymentActionsMenu cmd) throws Exception {
        File content = chooseFile();
        if (content == null) {
            // User cancelled
            return builder;
        }
        String contentName = content.getName();
        if (cmd != DeploymentActionsMenu.ADD_AND_REPLACE) {

            if (addedContent.contains(contentName) || model.get("deployment").hasDefined(contentName)) {
                stdout.println("ERROR: A deployment with name " + contentName + " already exists in the domain.");
                stdout.println("To replace it with content of the same name, choose:");
                stdout.println(DeploymentActionsMenu.ADD_AND_REPLACE.getPrompt());
                return deploymentPlanCancelPrompt() ? null : builder;
            }
        }
        switch (cmd) {
            case ADD: {
                builder = builder.add(contentName, content);
                break;
            }
            case ADD_AND_DEPLOY: {
                builder = builder.add(contentName, content).andDeploy();
                deployedContent.add(contentName);
                break;
            }
            case ADD_AND_REPLACE: {

                builder = builder.replace(contentName, content);
                ModelNode existing = model.get("deployment", contentName);
                if (deployedContent.contains(contentName) || (existing.isDefined() && existing.get("enabled").asBoolean(true) && !undeployedContent.contains(contentName))) {
                    deployedContent.add(contentName);
                }
                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid command " + cmd);
            }
        }

        addedContent.add(contentName);
        undeployedContent.remove(contentName);
        removedContent.remove(contentName);

        return builder;
    }

    private DeploymentPlanBuilder deployContent(DeploymentPlanBuilder builder, Set<String> addedContent,
                                                Set<String> deployedContent, Set<String> undeployedContent, Set<String> removedContent,
                                                ModelNode domainModel) throws IOException {

        String deployment = chooseUndeployedContent("Choose the content to deploy:", addedContent, deployedContent, undeployedContent, removedContent,
                domainModel);

        if (deployment != null) {
            deployedContent.add(deployment);
            undeployedContent.remove(deployment);
            return builder.deploy(deployment);
        }
        return builder;
    }

    private String chooseUndeployedContent(String message, Set<String> addedContent, Set<String> deployedContent,
                                           Set<String> undeployedContent, Set<String> removedContent, ModelNode domainModel) throws IOException {

        TreeSet<String> deployments = new TreeSet<String>(addedContent);
        // FIXME DomainModel needs to expose all deployments; here we are guessing
        if (domainModel.hasDefined("server-group")) {
            for (Property serverGroupProp : domainModel.get("server-group").asPropertyList()) {
                ModelNode serverGroup = serverGroupProp.getValue();
                if (serverGroup.hasDefined("deployment")) {
                    for (Property deploymentProp : serverGroup.get("deployment").asPropertyList()) {
                        ModelNode deployment = deploymentProp.getValue();
                        if (deployment.hasDefined("enabled") && !deployment.get("enabled").asBoolean()) {
                            deployments.add(deploymentProp.getName());
                        }
                    }
                }
            }
        }

        deployments.removeAll(deployedContent);
        deployments.addAll(undeployedContent);
        deployments.removeAll(removedContent);

        String deployment = chooseDeployment(deployments, message);
        return deployment;
    }

    private String chooseDeployment(TreeSet<String> deployments, String message) throws IOException {
        String deployment = null;
        do {
            stdout.println("Choose a deployment:");
            Map<String, Object> choices = writeMenuBody(new ArrayList<String>(deployments));
            stdout.println("[C]   Cancel");
            String choice = readStdIn();
            if (!"C".equals(choice.toUpperCase())) {
                Object dep = choices.get(choice);
                if (dep != null) {
                    deployment = dep.toString();
                } else {
                    stdout.println(choice + " is not a valid selection");
                }
            } else {
                break;
            }
        }
        while (deployment == null);

        return deployment;
    }

    private DeploymentPlanBuilder replaceContent(DeploymentPlanBuilder builder, Set<String> addedContent,
                                                 Set<String> deployedContent, Set<String> undeployedContent, Set<String> removedContent, ModelNode domainModel) throws IOException {

        String toDeploy = chooseUndeployedContent("Choose the replacement deployment:", removedContent, removedContent, removedContent, removedContent, domainModel);
        if (toDeploy != null) {
            String toReplace = chooseDeployedContent("Choose the deployment to be replaced:", deployedContent, undeployedContent, domainModel);

            if (toReplace != null) {
                return builder.replace(toDeploy, toReplace);
            }
        }
        return builder;
    }

    private String chooseDeployedContent(String message, Set<String> deployedContent, Set<String> undeployedContent, ModelNode domainModel)
            throws IOException {

        TreeSet<String> deployments = new TreeSet<String>(deployedContent);
        // FIXME DomainModel needs to expose all deployments; here we are guessing
        if (domainModel.hasDefined("server-group")) {
            for (Property serverGroupProp : domainModel.get("server-group").asPropertyList()) {
                ModelNode serverGroup = serverGroupProp.getValue();
                if (serverGroup.hasDefined("deployment")) {
                    for (Property deploymentProp : serverGroup.get("deployment").asPropertyList()) {
                        ModelNode deployment = deploymentProp.getValue();
                        if (!deployment.hasDefined("enabled") || deployment.get("enabled").asBoolean()) {
                            deployments.add(deploymentProp.getName());
                        }
                    }
                }
            }
        }
        deployments.removeAll(undeployedContent);

        String deployment = chooseDeployment(deployments, message);
        return deployment;
    }

    private DeploymentPlanBuilder undeployContent(DeploymentPlanBuilder builder, Set<String> addedContent,
                                                  Set<String> deployedContent, Set<String> undeployedContent, Set<String> removedContent,
                                                  ModelNode domainModel, DeploymentActionsMenu cmd) throws IOException {

        String toUndeploy = chooseDeployedContent("Choose the deployment to undeploy:", deployedContent, undeployedContent, domainModel);

        if (toUndeploy != null) {
            UndeployDeploymentPlanBuilder udpb = builder.undeploy(toUndeploy);
            builder = udpb;
            if (cmd == DeploymentActionsMenu.UNDEPLOY_AND_REMOVE) {
                builder = udpb.andRemoveUndeployed();
                removedContent.add(toUndeploy);
                addedContent.remove(toUndeploy);
            }

            undeployedContent.add(toUndeploy);
            deployedContent.remove(toUndeploy);
        }
        return builder;
    }

    private DeploymentPlanBuilder removeContent(DeploymentPlanBuilder builder, Set<String> addedContent,
                                                Set<String> deployedContent, Set<String> undeployedContent, Set<String> removedContent,
                                                ModelNode domainModel) throws IOException {


        String deployment = chooseUndeployedContent("Choose the content to remove:", addedContent, deployedContent, undeployedContent, removedContent,
                domainModel);

        if (deployment != null) {
            removedContent.add(deployment);
            addedContent.remove(deployment);
            return builder.remove(deployment);
        }
        return builder;
    }

    private boolean deploymentPlanCancelPrompt() throws IOException {
        stdout.println("\nHit Enter to continue or C to cancel this deployment plan:");
        String choice = readStdIn();
        return "C".equals(choice.toUpperCase());
    }

    private DeploymentPlan completeDeploymentPlan(ServerGroupDeploymentPlanBuilder groupPlanBuilder, ModelNode model, Set<String> includedGroups) throws IOException {

        do {
            writeMenu(DeploymentPlanMenu.ALL);
            String choice = readStdIn();
            DeploymentPlanMenu cmd = deploymentPlanMenuByCmd.get(choice.toUpperCase());
            if (cmd == null) {
                stdout.println(choice + " is not a valid selection.\n");
            } else {
                switch (cmd) {
                    case ROLLING_TO_SERVERS: {
                        groupPlanBuilder = groupPlanBuilder.rollingToServers();
                        break;
                    }
                    case TO_SERVER_GROUP: {
                        String serverGroup = chooseServerGroup(model, includedGroups);
                        if (serverGroup != null) {
                            groupPlanBuilder = groupPlanBuilder.toServerGroup(serverGroup);
                        }
                        break;
                    }
                    case ROLL_TO_SERVER_GROUP: {
                        String serverGroup = chooseServerGroup(model, includedGroups);
                        if (serverGroup != null) {
                            groupPlanBuilder = groupPlanBuilder.rollingToServerGroup(serverGroup);
                        }
                        break;
                    }
                    case EXECUTE: {
                        return groupPlanBuilder.build();
                    }
                    case CANCEL: {
                        return null;
                    }
                    default: {
                        stdout.println("Command " + cmd.getCommand() + " is not supported");
                    }
                }
            }
        }
        while (true);
    }

    private void writeDeploymentPlanResult(DeploymentPlanResult deploymentPlanResult) {

        stdout.println("executed");

    }

    private File chooseFile() throws IOException {
        initializeSwing();
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Archives", "jar", "war", "sar");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    private synchronized void initializeSwing() {
        if (swingLAF != null)
            return;

        if (System.getProperty("swing.defaultlaf") != null)
            return;

        String sep = File.separator;
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            Properties props = new Properties();
            File file = new File(javaHome + sep + "lib" + sep + "swing.properties");
            if (file.exists()) {
                // InputStream has been buffered in Properties
                // class
                FileInputStream ins = null;
                try {
                    ins = new FileInputStream(file);
                    props.load(ins);
                } catch (IOException ignored) {
                } finally {
                    if (ins != null) {
                        try {
                            ins.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }
            String clazz = props.getProperty("swing.defaultlaf");
            if (clazz != null) {
                try {
                    getClass().getClassLoader().loadClass(clazz);
                    swingLAF = clazz;
                } catch (ClassNotFoundException e) {
                    // ignore; we'll use Metal below
                }
            }
        }

        if (swingLAF == null) {
            // Configure swing to use a L&F that's in classes.jar javax.swing package
            swingLAF = MetalLookAndFeel.class.getName();
            System.setProperty("swing.defaultlaf", swingLAF);
        }

    }

    private boolean addJmsQueue() throws Exception {

        stdout.println("Enter the name for the new queue or [C] to cancel:");
        String queueName = readStdIn();
        if ("C".equals(queueName.toUpperCase())) {
            return continuePrompt();
        }

        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.PROFILE, "default");
        address.add(ModelDescriptionConstants.SUBSYSTEM, "messaging");
        address.add("hornetq-server", "default");
        address.add("jms-queue", queueName);

        final ModelNode queueAddOperation = new ModelNode();
        queueAddOperation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        queueAddOperation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        queueAddOperation.get("entries").add(queueName);

        DomainDeploymentUtils utils = null;
        boolean deployed = false;
        try {
            utils = new DomainDeploymentUtils(client);
            utils.addDeployment("fakejndi.sar", FakeJndi.class.getPackage());
            utils.deploy();

            deployed = true;

            try {
                final ModelNode response = executeForResponse(queueAddOperation);
                // Validate success
                getSuccessResult(queueAddOperation);
                Collection<String> serverGroups = resultToServerGroupIdentitySet(response);
                for (String serverGroup : serverGroups) {
                    System.out.println(serverGroup);
                    //TODO: fixme
                    //exerciseQueueOnServer(queueName, serverGroup);
                }

            } catch (Exception e) {
                System.out.println("failed to execute operation " + queueAddOperation);
                e.printStackTrace();
            }

            return continuePrompt();
        } finally {
            if (deployed && utils != null) {
                utils.undeploy();
            }

        }
    }

    private void exerciseQueueOnServer(final String queueName, final ServerIdentity server) throws Exception {
        stdout.println("Exercising queue " + queueName + " on server " + server.getServerName());

        QueueConnection conn = null;
        QueueSession session = null;
        try {
            QueueConnectionFactory qcf = lookup(server, "RemoteConnectionFactory", QueueConnectionFactory.class);
            Queue queue = lookup(server, queueName, Queue.class);

            conn = qcf.createQueueConnection();
            conn.start();
            session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);

            // Set the async listener
            QueueReceiver recv = session.createReceiver(queue);
            recv.setMessageListener(new MessageListener() {

                @Override
                public void onMessage(Message message) {
                    TextMessage msg = (TextMessage) message;
                    try {
                        stdout.println("---->Received: " + msg.getText());
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });

            QueueSender sender = session.createSender(queue);
            for (int i = 0; i < 10; i++) {
                String s = "Test" + i;
                stdout.println("----> Sending: " + s);
                TextMessage msg = session.createTextMessage(s);
                sender.send(msg);
            }
        } finally {
            try {
                conn.stop();
            } catch (Exception ignore) {
            }
            try {
                session.close();
            } catch (Exception ignore) {
            }
            try {
                conn.close();
            } catch (Exception ignore) {
            }
        }
    }

    private String readStdIn() throws IOException {
        StringBuilder sb = new StringBuilder();
        char c;
        do {
            c = (char) stdin.read();
            if (c == -1)
                return "Q";
            sb.append(c);
        }
        while (stdin.ready());
        return sb.toString().trim();
    }

    private <T> T lookup(ServerIdentity server, String name, Class<T> expected) throws Exception {
        // TODO replace with direct jndi lookup when remote JNDI support is added
        MBeanServerConnection mbeanServer = getMBeanServerConnection(server);
        ObjectName objectName = new ObjectName("jboss:name=test,type=fakejndi");
        Object o = mbeanServer.invoke(objectName, "lookup", new Object[]{name}, new String[]{"java.lang.String"});
        return expected.cast(o);
    }

    private MBeanServerConnection getMBeanServerConnection(ServerIdentity server) throws Exception {
        // Poke the running server directly for its binding config
        final ModelNode address = new ModelNode();
        address.add("host", server.getHostName());
        address.add("server", server.getServerName());
        address.add("socket-binding-group", "standard-sockets");

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get(RECURSIVE).set(true);

        final ModelNode result = executeForResult(operation);
        final int portOffset = result.get(PORT_OFFSET).asInt(0);
        final int port = result.get(SOCKET_BINDING, "jmx-connector-registry", PORT).asInt() + portOffset;

        final String addr = "localhost"; // TODO determine the interface binding

        String url = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", addr, port);
        return JMXConnectorFactory.connect(new JMXServiceURL(url),
                new HashMap<String, Object>()).getMBeanServerConnection();
    }

    private static interface MenuItem {
        String getPrompt();
    }

    private static enum MainMenu implements MenuItem {

        DOMAIN_CFG("1", "Dump Domain Configuration"),
        LIST_HCS("2", "List Host Controllers"),
        HC_CFG("3", "Dump Host Controller Configuration"),
        LIST_SERVERS("4", "List Servers"),
        SERVER_CFG("5", "Dump a Server's Current Runtime Configuration"),
        SERVER_STOP("6", "Stop a Server"),
        SERVER_START("7", "Start a Server"),
        SERVER_RESTART("8", "Restart a Server"),
        ADD_SERVER("9", "Add a Server"),
        REMOVE_SERVER("10", "Remove a Server"),
        ADD_JMS_QUEUE("11", "Add a JMS Queue"),
        DEPLOYMENTS("12", "Create and Execute a Deployment Plan"),
        QUIT("Q", "Quit");

        private static final EnumSet<MainMenu> ALL = EnumSet.allOf(MainMenu.class);

        private final String cmd;
        private final String prompt;

        private MainMenu(String cmd, String message) {
            this.cmd = cmd;
            String spaces = cmd.length() == 1 ? "   " : "  ";
            this.prompt = "[" + cmd + "]" + spaces + message;
        }

        @Override
        public String getPrompt() {
            return prompt;
        }

        public String getCommand() {
            return cmd;
        }
    }

    private static enum DeploymentActionsMenu implements MenuItem {

        ADD("1", "Add new deployment content to the domain"),
        ADD_AND_DEPLOY("2", "Add new deployment content to the domain and deploy it"),
        ADD_AND_REPLACE("3", "Add new content to the domain and use it to replace existing content of the same name"),
        DEPLOY("4", "Deploy previously added content"),
        REPLACE("5", "Deploy previously added content as a replacement for another deployment"),
        UNDEPLOY("6", "Undeploy content"),
        UNDEPLOY_AND_REMOVE("7", "Undeploy content and remove from the domain"),
        REMOVE("8", "Remove previously undeployed content from the domain"),
        APPLY("9", "Apply deployment actions to a server group"),
        CANCEL("C", "Cancel");

        private static final EnumSet<DeploymentActionsMenu> ALL = EnumSet.allOf(DeploymentActionsMenu.class);
        private static final EnumSet<DeploymentActionsMenu> INITIAL = EnumSet.complementOf(EnumSet.of(APPLY));

        private final String cmd;
        private final String prompt;

        private DeploymentActionsMenu(String cmd, String message) {
            this.cmd = cmd;
            String spaces = cmd.length() == 1 ? "   " : "  ";
            this.prompt = "[" + cmd + "]" + spaces + message;
        }

        @Override
        public String getPrompt() {
            return prompt;
        }

        public String getCommand() {
            return cmd;
        }
    }

    private static enum DeploymentPlanMenu implements MenuItem {

        ROLLING_TO_SERVERS("1", "Roll out changes to servers in the group one at a time"),
        TO_SERVER_GROUP("2", "Concurrently apply changes to another server group"),
        ROLL_TO_SERVER_GROUP("3", "Roll out changes to another server group once the current group completes"),
        EXECUTE("4", "Execute the deployment plan"),
        CANCEL("C", "Cancel the entire deployment plan");

        private static final EnumSet<DeploymentPlanMenu> ALL = EnumSet.allOf(DeploymentPlanMenu.class);

        private final String cmd;
        private final String prompt;

        private DeploymentPlanMenu(String cmd, String message) {
            this.cmd = cmd;
            String spaces = cmd.length() == 1 ? "   " : "  ";
            this.prompt = "[" + cmd + "]" + spaces + message;
        }

        @Override
        public String getPrompt() {
            return prompt;
        }

        public String getCommand() {
            return cmd;
        }
    }

    public static void main(String[] args) throws Exception {

        ExampleRunner runner = new ExampleRunner(InetAddress.getByName("localhost"), 9999);
        runner.run();
    }

    private ModelNode executeForResponse(ModelNode op) throws Exception {
        return executeForResponse(new OperationBuilder(op).build());
    }

    private ModelNode executeForResult(ModelNode op) throws Exception {
        return executeForResult(new OperationBuilder(op).build());
    }

    private ModelNode executeForResponse(Operation op) {
        try {
            return client.execute(op);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ModelNode executeForResult(Operation op) {
        ModelNode response = executeForResponse(op);
        return getSuccessResult(response);
    }

    private ModelNode getSuccessResult(ModelNode response) {
        if (response.hasDefined("outcome") && "success".equals(response.get("outcome").asString())) {
            return response.get("result");
        } else if (response.hasDefined("failure-description")) {
            throw new RuntimeException(response.get("failure-description").toString());
        } else if (response.hasDefined("domain-failure-description")) {
            throw new RuntimeException(response.get("domain-failure-description").toString());
        } else if (response.hasDefined("host-failure-descriptions")) {
            throw new RuntimeException(response.get("host-failure-descriptions").toString());
        } else {
            throw new RuntimeException("Operation outcome is " + response);
        }
    }

    /**
     * Get a domain operation result as a list of affected server identities.
     *
     * @param result the operation result
     * @return a collection of affected
     */
    private Collection<String> resultToServerGroupIdentitySet(final ModelNode result) {
        final Collection<String> servers = new ArrayList<String>();
        for (final Property serverGroup : result.get(SERVER_GROUPS).asPropertyList()) {
            servers.add(serverGroup.getName());
        }
        return servers;
    }

}
