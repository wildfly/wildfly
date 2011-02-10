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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.InetAddress;
import java.util.ArrayList;
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
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.domain.client.api.DomainClient;
import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.domain.client.api.deployment.DeploymentPlan;
import org.jboss.as.domain.client.api.deployment.DeploymentPlanBuilder;
import org.jboss.as.domain.client.api.deployment.DeploymentPlanResult;
import org.jboss.as.domain.client.api.deployment.DeploymentSetActionsCompleteBuilder;
import org.jboss.as.domain.client.api.deployment.DomainDeploymentManager;
import org.jboss.as.domain.client.api.deployment.ServerGroupDeploymentPlanBuilder;
import org.jboss.as.domain.client.api.deployment.UndeployDeploymentPlanBuilder;
import org.jboss.as.model.DeploymentUnitElement;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.ServerGroupDeploymentElement;
import org.jboss.as.model.ServerGroupElement;
import org.jboss.as.model.ServerModel;
import org.jboss.staxmapper.XMLContentWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;

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
        this.client = DomainClient.Factory.create(address, port);
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
        }
        catch (Exception e) {
            e.printStackTrace(System.out);
        }
        finally {

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
            }
            else {
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
                    // Disabled for now
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
            }
            else {
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
        DomainModel dm = client.getDomainModel();
        stdout.println(writeModel("domain", dm));
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

    private boolean dumpHostController()  throws Exception {
        List<String> hostControllers = client.getHostControllerNames();
        if (hostControllers.size() == 0) {
            // this isn't possible :-)
            stdout.println("No host controllers available");
        }
        else if (hostControllers.size() == 1) {
            writeHostController(hostControllers.get(0));
        }
        else {
            stdout.println("Choose a Host Controller:");
            Map<String, Object> choices = writeMenuBody(hostControllers);
            stdout.println("[C]   Cancel");
            String choice = readStdIn();
            if (!"C".equals(choice.toUpperCase())) {
                Object hc = choices.get(choice);
                if (hc != null) {
                    writeHostController(hc.toString());
                }
                else {
                    stdout.println(choice + " is not a valid selection");
                    return dumpHostController();
                }
            }
        }
        return continuePrompt();
    }

    private void writeHostController(String hc) throws Exception {
        stdout.println("\nReading host configuration for host controller " + hc + "\n");
        stdout.println(writeModel("host", client.getHostModel(hc)));
    }

    private boolean listServers() throws Exception {
        stdout.println("\nReading the list of configured servers:");
        for(Map.Entry<ServerIdentity, ServerStatus> server : client.getServerStatuses().entrySet()) {
            ServerIdentity id = server.getKey();
            stdout.println("\nServer:\n");
            stdout.println("server name:         " + id.getServerName());
            stdout.println("host controller name: " + id.getHostName());
            stdout.println("server group name:   " + id.getServerGroupName());
            stdout.println("status:              " + server.getValue());
        }
        return continuePrompt();
    }

    private boolean dumpServer()  throws Exception {
        ServerIdentity server = chooseServer(ServerStatus.STARTED);
        if (server != null) {
            stdout.println("\nReading runtime configuration for " + server.getServerName() + "\n");
            ServerModel hc = client.getServerModel(server.getHostName(), server.getServerName());
            if (hc == null) {
                stdout.println("ERROR: server model is null");
            }
            else {
                stdout.println(writeModel("server", hc));
            }
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

        // THIS DOES NOT CURRENTLY WORK
        throw new UnsupportedOperationException("Convert to detyped API");
//        addCount++;
//        stdout.println("Enter the name of the new server, or [C] to cancel:");
//        String serverName = readStdIn();
//        if ("C".equals(serverName.toUpperCase()))
//            return continuePrompt();
//
//        String hostController = null;
//        List<String> hostControllers = client.getHostControllerNames();
//        if (hostControllers.size() == 1) {
//            hostController = hostControllers.get(0);
//        }
//        else {
//            do {
//                stdout.println("Choose a Host Controller for the new Server:");
//                Map<String, Object> choices = writeMenuBody(hostControllers);
//                stdout.println("[C]   Cancel");
//                String choice = readStdIn();
//                if ("C".equals(choice.toUpperCase())) {
//                    return continuePrompt();
//                }
//                Object obj = choices.get(choice);
//
//                if (obj == null) {
//                    stdout.println(choice + " is not a valid selection");
//                }
//                else {
//                    hostController = obj.toString();
//                }
//            }
//            while (hostController == null);
//        }
//
//        String serverGroup = null;
//        List<String> serverGroups = getServerGroupNames();
//        do {
//            stdout.println("Choose a Server Group for the new Server:");
//            Map<String, Object> choices = writeMenuBody(serverGroups);
//            stdout.println("[C]   Cancel");
//            String choice = readStdIn();
//            if ("C".equals(choice.toUpperCase())) {
//                return continuePrompt();
//            }
//            Object obj = choices.get(choice);
//
//            if (obj == null) {
//                stdout.println(choice + " is not a valid selection");
//            }
//            else {
//                serverGroup = obj.toString();
//            }
//
//        }
//        while (serverGroup == null);
//
//        stdout.println("\nCreating new server: " + serverName + " on host controller " + hostController + " in server group: " + serverGroup);
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
        SortedSet<String> sorted = new TreeSet<String>(client.getDomainModel().getServerGroupNames());
        return new ArrayList<String>(sorted);
    }

    private boolean removeServer() throws Exception {

        // THIS DOES NOT CURRENTLY WORK
        throw new UnsupportedOperationException("Convert to detyped API");
//        ServerIdentity server = chooseServer(ServerStatus.STOPPED, ServerStatus.DISABLED);
//        if (server != null) {
//            stdout.println("Removing server " + server.getServerName());
//            List<AbstractHostModelUpdate<?>> updates = new ArrayList<AbstractHostModelUpdate<?>>(1);
//            updates.add(new HostServerRemove(server.getServerName()));
//
//            List<HostUpdateResult<?>>results = client.applyHostUpdates(server.getHostName(), updates);
//            HostUpdateResult<?> result = results.get(0);
//            stdout.println("Remove success: " + result.isSuccess());
//        }
//        return continuePrompt();
    }

    private ServerIdentity chooseServer(ServerStatus valid, ServerStatus...alsoValid) throws IOException {
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
        }
        else {
            stdout.println("Choose a Server:");
            Map<String, Object> choices = writeMenuBody(new ArrayList<String>(servers.keySet()));
            stdout.println("[C]   Cancel");
            String choice = readStdIn();
            if (!"C".equals(choice.toUpperCase())) {
                Object hc = choices.get(choice);
                if (hc != null) {
                    result = servers.get(hc.toString());
                }
                else {
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
        DomainModel model = client.getDomainModel();
        DeploymentSetActionsCompleteBuilder completionBuilder = null;
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

    private String chooseServerGroup(DomainModel model, Set<String> existingGroups) throws IOException {
        TreeSet<String> groups = new TreeSet<String>(model.getServerGroupNames());
        groups.removeAll(existingGroups);
        List<String> serverGroups = new ArrayList<String>(groups);
        String serverGroup = null;
        do {
            stdout.println("Choose a Server Group for the new Server:");
            Map<String, Object> choices = writeMenuBody(serverGroups);
            stdout.println("[C]   Cancel");
            String choice = readStdIn();
            if ("C".equals(choice.toUpperCase())) {
                break;
            }
            Object obj = choices.get(choice);

            if (obj == null) {
                stdout.println(choice + " is not a valid selection");
            }
            else {
                serverGroup = obj.toString();
            }

        }
        while (serverGroup == null);
        return serverGroup;
    }

    private DeploymentSetActionsCompleteBuilder deploymentSetBuilder(DeploymentPlanBuilder builder, DomainModel model) throws Exception {

        // Vars to track differences between the model and what our actions will have done
        Set<String> addedContent = new HashSet<String>();
        Set<String> deployedContent = new HashSet<String>();
        Set<String> undeployedContent = new HashSet<String>();
        Set<String> removedContent = new HashSet<String>();
        do {
            boolean hasActions = (builder instanceof DeploymentSetActionsCompleteBuilder);
            writeMenu(hasActions ? DeploymentActionsMenu.ALL : DeploymentActionsMenu.INITIAL);
            String choice = readStdIn();
            DeploymentActionsMenu cmd = deploymentActionMenuByCmd.get(choice.toUpperCase());
            if (cmd == null) {
                stdout.println(choice + " is not a valid selection.\n");
            }
            else {
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
                    case UNDEPLOY_AND_REMOVE:{
                        builder = undeployContent(builder, addedContent, deployedContent, undeployedContent, removedContent, model, cmd);
                        break;
                    }
                    case REMOVE: {
                        builder = removeContent(builder, addedContent, deployedContent, undeployedContent, removedContent, model);
                        break;
                    }
                    case APPLY: {
                        if (hasActions) {
                            return (DeploymentSetActionsCompleteBuilder) builder;
                        }
                        else {
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
            DomainModel model, DeploymentActionsMenu cmd) throws Exception {
        File content = chooseFile();
        if (content == null) {
            // User cancelled
            return builder;
        }
        String contentName = content.getName();
        if (cmd != DeploymentActionsMenu.ADD_AND_REPLACE) {
            if (addedContent.contains(contentName) || model.getDeployment(contentName) != null) {
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
                DeploymentUnitElement existing = model.getDeployment(contentName);
                if (deployedContent.contains(contentName) || (existing != null && existing.isStart() && !undeployedContent.contains(contentName))) {
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
            DomainModel model) throws IOException {

        String deployment = chooseUndeployedContent("Choose the content to deploy:", addedContent, deployedContent, undeployedContent, removedContent,
                model);

        if (deployment != null) {
            deployedContent.add(deployment);
            undeployedContent.remove(deployment);
            return  builder.deploy(deployment);
        }
        return builder;
    }

    private String chooseUndeployedContent(String message, Set<String> addedContent, Set<String> deployedContent,
            Set<String> undeployedContent, Set<String> removedContent, DomainModel model) throws IOException {
        TreeSet<String> deployments = new TreeSet<String>(addedContent);
        // FIXME DomainModel needs to expose all deployments; here we are guessing
        for (String sgn : model.getServerGroupNames()) {
            ServerGroupElement sge = model.getServerGroup(sgn);
            for (ServerGroupDeploymentElement sgde : sge.getDeployments()) {
                if (!sgde.isStart()) {
                    deployments.add(sgde.getUniqueName());
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
                }
                else {
                    stdout.println(choice + " is not a valid selection");
                }
            }
            else {
                break;
            }
        }
        while (deployment == null);

        return deployment;
    }

    private DeploymentPlanBuilder replaceContent(DeploymentPlanBuilder builder, Set<String> addedContent,
            Set<String> deployedContent, Set<String> undeployedContent, Set<String> removedContent, DomainModel model) throws IOException {

        String toDeploy = chooseUndeployedContent("Choose the replacement deployment:", removedContent, removedContent, removedContent, removedContent, model);
        if (toDeploy != null) {
            String toReplace = chooseDeployedContent("Choose the deployment to be replaced:", deployedContent, undeployedContent, model);

            if (toReplace != null) {
                return  builder.replace(toDeploy, toReplace);
            }
        }
        return builder;
    }

    private String chooseDeployedContent(String message, Set<String> deployedContent, Set<String> undeployedContent, DomainModel model)
            throws IOException {
        TreeSet<String> deployments = new TreeSet<String>(deployedContent);
        // FIXME DomainModel needs to expose all deployments; here we are guessing
        for (String sgn : model.getServerGroupNames()) {
            ServerGroupElement sge = model.getServerGroup(sgn);
            for (ServerGroupDeploymentElement sgde : sge.getDeployments()) {
                if (sgde.isStart()) {
                    deployments.add(sgde.getUniqueName());
                }
            }
        }
        deployments.removeAll(undeployedContent);

        String deployment = chooseDeployment(deployments, message);
        return deployment;
    }

    private DeploymentPlanBuilder undeployContent(DeploymentPlanBuilder builder, Set<String> addedContent,
            Set<String> deployedContent, Set<String> undeployedContent, Set<String> removedContent,
            DomainModel model, DeploymentActionsMenu cmd) throws IOException {

        String toUndeploy = chooseDeployedContent("Choose the deployment to undeploy:", deployedContent, undeployedContent, model);

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
            DomainModel model) throws IOException {


        String deployment = chooseUndeployedContent("Choose the content to remove:", addedContent, deployedContent, undeployedContent, removedContent,
                model);

        if (deployment != null) {
            removedContent.add(deployment);
            addedContent.remove(deployment);
            return  builder.remove(deployment);
        }
        return builder;
    }

    private boolean deploymentPlanCancelPrompt() throws IOException {
        stdout.println("\nHit Enter to continue or C to cancel this deployment plan:");
        String choice = readStdIn();
        return "C".equals(choice.toUpperCase());
    }

    private DeploymentPlan completeDeploymentPlan(ServerGroupDeploymentPlanBuilder groupPlanBuilder, DomainModel model, Set<String> includedGroups) throws IOException {

        do {
            writeMenu(DeploymentPlanMenu.ALL);
            String choice = readStdIn();
            DeploymentPlanMenu cmd = deploymentPlanMenuByCmd.get(choice.toUpperCase());
            if (cmd == null) {
                stdout.println(choice + " is not a valid selection.\n");
            }
            else {
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
        if(returnVal == JFileChooser.APPROVE_OPTION) {
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
                }
                catch (IOException ignored) {}
                finally {
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

        // THIS DOES NOT CURRENTLY WORK
        throw new UnsupportedOperationException("Convert to detyped API");
//        stdout.println("Enter the name for the new queue or [C] to cancel:");
//        String queueName = readStdIn();
//        if ("C".equals(queueName.toUpperCase()))
//            return continuePrompt();
//
//        DomainDeploymentUtils utils = null;
//        boolean actionsApplied = false;
//        boolean deployed = false;
//        try {
//            utils = new DomainDeploymentUtils(client);
//            utils.addDeployment("fakejndi.sar", FakeJndi.class.getPackage());
//            utils.deploy();
//
//            deployed = true;
//
//            final JMSQueueAdd queueAdd = new JMSQueueAdd(queueName);
//            queueAdd.setBindings(Collections.singleton(queueName));
//
//            DomainSubsystemUpdate<JMSSubsystemElement, Void> domainUpdate = DomainSubsystemUpdate.create("messaging", queueAdd);
//            List<AbstractDomainModelUpdate<?>> list = Collections.<AbstractDomainModelUpdate<?>>singletonList(domainUpdate);
//            List<DomainUpdateResult<?>> results = client.applyUpdates(list);
//            DomainUpdateResult<?> result = results.get(0);
//            stdout.println(result + (result != null ? String.valueOf(result.isSuccess()) + String.valueOf(result.getServerResults().size()) : ""));
//            if (result.getDomainFailure() == null && result.getHostFailures().size() == 0) {
//                actionsApplied = true;
//            }
//            if (result.isSuccess()) {
//                for (ServerIdentity server : result.getServerResults().keySet()) {
//                    exerciseQueueOnServer(queueName, server);
//                }
//            }
//            else if (result.getDomainFailure() != null) {
//                stdout.println("Queue addition failed on the domain controller");
//                result.getDomainFailure().printStackTrace(stdout);
//            }
//            else if (result.getHostFailures().size() > 0) {
//                for (Map.Entry<String, UpdateFailedException> entry : result.getHostFailures().entrySet()) {
//                    stdout.println("\nQueue addition failed on Host Controller " + entry.getKey());
//                    entry.getValue().printStackTrace(stdout);
//                }
//            }
//            else if (result.getServerFailures().size() > 0) {
//
//                for (Map.Entry<ServerIdentity, Throwable> entry : result.getServerFailures().entrySet()) {
//                    stdout.println("\nQueue addition failed on Server " + entry.getKey().getServerName());
//                    entry.getValue().printStackTrace(stdout);
//                }
//            }
//            else if (result.getServerCancellations().size() > 0) {
//                for (ServerIdentity server : result.getServerCancellations()) {
//                    stdout.println("\nQueue addition was cancelled on Server " + server.getServerName());
//                }
//            }
//
//            return continuePrompt();
//        }
//        finally {
//            if (deployed) {
//                utils.undeploy();
//            }
//            if (utils != null)
//                utils.close();
//            if(actionsApplied) {
//                // Remove the queue using the management API
//                final JMSQueueRemove queueRemove = new JMSQueueRemove(queueName);
//                DomainSubsystemUpdate<JMSSubsystemElement, Void> domainUpdate = DomainSubsystemUpdate.create("messaging", queueRemove);
//                client.applyUpdates(Collections.<AbstractDomainModelUpdate<?>>singletonList(domainUpdate));
//            }
//        }
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
                    TextMessage msg = (TextMessage)message;
                    try {
                        stdout.println("---->Received: " + msg.getText());
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });

            QueueSender sender = session.createSender(queue);
            for (int i = 0 ; i < 10 ; i++) {
                String s = "Test" + i;
                stdout.println("----> Sending: " +s );
                TextMessage msg = session.createTextMessage(s);
                sender.send(msg);
            }
        }
        finally {
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
        Object o = mbeanServer.invoke(objectName, "lookup", new Object[] {name}, new String[] {"java.lang.String"});
        return expected.cast(o);
    }

    private MBeanServerConnection getMBeanServerConnection(ServerIdentity server) throws Exception {

        // THIS DOES NOT CURRENTLY WORK
        throw new UnsupportedOperationException("Convert to detyped API");
//        // FIXME we need an API to get the actual address and port used by a server given
//        // the socket binding name. So then we'd get the socket binding name
//        // from the subsystem config and then find the InetSocketAddress from the server using that API
//        DomainModel domainModel = client.getDomainModel();
//        ServerElement se = client.getHostModel(server.getHostName()).getServer(server.getServerName());
//        String socketBindingGroupName = se.getSocketBindingGroupName();
//        int offset = se.getSocketBindingPortOffset();
//        if (socketBindingGroupName == null) {
//            ServerGroupElement sge = domainModel.getServerGroup(se.getServerGroup());
//            socketBindingGroupName = sge.getSocketBindingGroupName();
//        }
//        SocketBindingGroupElement sbge = domainModel.getSocketBindingGroup(socketBindingGroupName);
//        String address = null;
//        int port = -1;
//        for (SocketBindingElement sbe : sbge.getSocketBindings()) {
//            // TODO deal with fact this socket could be in an included group
//            if ("jmx-connector-registry".equals(sbe.getName())) {
//                address = sbe.getInterfaceName();
//                port = sbe.getPort() + offset;
//                break;
//            }
//        }
//        InetAddress addr = null;
//
//        try {
//            addr = InetAddress.getByName(address);
//            address = addr.getHostAddress();
//        } catch (UnknownHostException e) {
//            address = "localhost";
//        }
//
//        String url = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", address, port);
//        return JMXConnectorFactory.connect(new JMXServiceURL(url),
//                new HashMap<String, Object>()).getMBeanServerConnection();
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
        DEPLOYMENTS("11", "Create and Execute a Deployment Plan"),
        // NOT CURRENTLY WORKING
        ADD_JMS_QUEUE("12", "Add a JMS Queue"),
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

    private static String writeModel(final String element, final XMLContentWriter content) throws Exception, FactoryConfigurationError {
        final XMLMapper mapper = XMLMapper.Factory.create();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final BufferedOutputStream bos = new BufferedOutputStream(baos);
        final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(bos);
        try {
            mapper.deparseDocument(new RootElementWriter(element, content), writer);
        }
        catch (XMLStreamException e) {
            // Dump some diagnostics
            stdout.println("XML Content that was written prior to exception:");
            stdout.println(writer.toString());
            throw e;
        }
        finally {
            writer.close();
            bos.close();
        }
        return new String(baos.toByteArray());
    }

    private static class RootElementWriter implements XMLContentWriter {

        private final String element;
        private final XMLContentWriter content;

        RootElementWriter(final String element, final XMLContentWriter content) {
            this.element = element;
            this.content = content;
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
            streamWriter.writeStartDocument();
            streamWriter.writeStartElement(element);
            content.writeContent(streamWriter);
            streamWriter.writeEndDocument();
        }

    }

}
