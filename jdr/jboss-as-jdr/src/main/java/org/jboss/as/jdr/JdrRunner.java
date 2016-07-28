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
package org.jboss.as.jdr;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.scriptsupport.CLI;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.jdr.commands.JdrCommand;
import org.jboss.as.jdr.commands.JdrEnvironment;
import org.jboss.as.jdr.logger.JdrLogger;
import org.jboss.as.jdr.plugins.JdrPlugin;
import org.jboss.as.jdr.util.JdrZipFile;
import org.jboss.dmr.ModelNode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.jboss.as.cli.CommandContextFactory;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.jdr.logger.JdrLogger.ROOT_LOGGER;

public class JdrRunner implements JdrReportCollector {

    JdrEnvironment env = new JdrEnvironment();
    CommandContext ctx;

    public JdrRunner(boolean serverRunning) {
        this.env.setServerRunning(serverRunning);
    }

    public JdrRunner(CLI cli, String protocol, String host, int port, String user, String pass) {
        this.env.setServerRunning(false);
        this.env.setUsername(user);
        this.env.setPassword(pass);
        this.env.setHost(cli.getCommandContext().getControllerHost());
        this.env.setPort("" + cli.getCommandContext().getControllerPort());
        this.env.setCli(cli);
        try {
            ctx = CommandContextFactory.getInstance().newCommandContext(constructUri(protocol, host, port), user, pass == null ? new char[0] : pass.toCharArray());
            ctx.connectController();
            this.env.setClient(ctx.getModelControllerClient());
        } catch (Exception e) {
            ctx.terminateSession();
            // the server isn't available, carry on
        }
    }

    @Override
    public JdrReport collect() throws OperationFailedException {

        this.env.setProductName(obtainProductName());
        this.env.setProductVersion(obtainProductVersion());

        try {
            this.env.setZip(new JdrZipFile(new JdrEnvironment(this.env)));
        } catch (Exception e) {
            ROOT_LOGGER.error(ROOT_LOGGER.couldNotCreateZipfile(), e);
            throw new OperationFailedException(JdrLogger.ROOT_LOGGER.couldNotCreateZipfile());
        }

        List<JdrCommand> commands = new ArrayList<JdrCommand>();

        ByteArrayOutputStream versionStream = new ByteArrayOutputStream();
        PrintWriter versionWriter = new PrintWriter(new OutputStreamWriter(versionStream));
        versionWriter.println("JDR: " + Namespace.CURRENT.getUriString());

        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("plugins.properties");
            Properties plugins = new Properties();
            plugins.load(is);
            for (String pluginName : plugins.stringPropertyNames()) {
                Class<?> pluginClass = Class.forName(pluginName);
                JdrPlugin plugin = (JdrPlugin) pluginClass.newInstance();
                commands.addAll(plugin.getCommands());
                versionWriter.println(plugin.getPluginId());
            }
            versionWriter.close();
            this.env.getZip().add(new ByteArrayInputStream(versionStream.toByteArray()), "version.txt");

        } catch (Exception e) {
            ROOT_LOGGER.error(ROOT_LOGGER.couldNotConfigureJDR(), e);
            throw new OperationFailedException(ROOT_LOGGER.couldNotConfigureJDR());
        }

        if (commands.size() < 1) {
            ROOT_LOGGER.error(JdrLogger.ROOT_LOGGER.noCommandsToRun());
            throw new OperationFailedException(JdrLogger.ROOT_LOGGER.noCommandsToRun());
        }

        JdrReport report = new JdrReport();
        StringBuilder skips = new StringBuilder();
        report.setStartTime();
        report.setJdrUuid(obtainServerUUID());
        for (JdrCommand command : commands) {
            command.setEnvironment(new JdrEnvironment(this.env));
            try {
                command.execute();
            } catch (Throwable t) {
                String message = "Skipping command " + command.toString();
                ROOT_LOGGER.debugf(message);
                skips.append(message);
                PrintWriter pw = new PrintWriter(new StringWriter());
                t.printStackTrace(pw);
                skips.append(pw.toString());
                pw.close();
            }
        }

        try {
            this.env.getZip().addLog(skips.toString(), "skips.log");
        } catch (Exception e) {
            ROOT_LOGGER.debugf(e, "Could not add skipped commands log to jdr zip file.");
        }

        try {
            this.env.getZip().close();
        } catch (Exception e) {
            ROOT_LOGGER.debugf(e, "Could not close zip file.");
        }

        report.setEndTime();
        report.setLocation(this.env.getZip().name());

        try {
            ctx.terminateSession();
        } catch (Exception e) {
            // idk
        }

        return report;
    }

    public void setJbossHomeDir(String dir) {
        this.env.setJbossHome(dir);
    }

    public void setReportLocationDir(String dir) {
        this.env.setOutputDirectory(dir);
    }

    public void setControllerClient(ModelControllerClient client) {
        this.env.setClient(client);
    }

    public void setHostControllerName(String name) {
        this.env.setHostControllerName(name);
    }

    public void setServerName(String name) {
        this.env.setServerName(name);
    }

    private String obtainServerUUID() throws OperationFailedException {
        try {
            ModelNode operation = Operations.createReadAttributeOperation(new ModelNode().setEmptyList(), UUID);
            operation.get(INCLUDE_RUNTIME).set(true);
            ModelControllerClient client = env.getClient();
            if (client == null) {
                client = env.getCli().getCommandContext().getModelControllerClient();
            }
            ModelNode result = client.execute(operation);
            if (Operations.isSuccessfulOutcome(result)) {
                return Operations.readResult(result).asString();
            }
            return null;
        } catch (IOException ex) {
            return null;
        }
    }

    private String constructUri(final String protocol, final String host, final int port) throws URISyntaxException {
        URI uri = new URI(protocol, null, host, port, null, null, null);
        // String the leading '//' if there is no protocol.
        return protocol == null ? uri.toString().substring(2) : uri.toString();
    }

    private String obtainProductName() {
        try {
            ModelNode operation = Operations.createReadAttributeOperation(new ModelNode().setEmptyList(), PRODUCT_NAME);
            operation.get(INCLUDE_RUNTIME).set(false);
            ModelControllerClient client = env.getClient();

            if (client == null) {
                client = env.getCli().getCommandContext().getModelControllerClient();
            }

            ModelNode result = client.execute(operation);

            if (Operations.isSuccessfulOutcome(result)) {
                return Operations.readResult(result).asString();
            }

            return "undefined";
        } catch (IOException e) {
            // This should not be needed since a product name is always returned, even if it doesn't exist.
            // In that case "undefined" is returned
            return "undefined";
        }
    }

    private String obtainProductVersion() {
        try {
            ModelNode operation = Operations.createReadAttributeOperation(new ModelNode().setEmptyList(), PRODUCT_VERSION);
            operation.get(INCLUDE_RUNTIME).set(false);
            ModelControllerClient client = env.getClient();

            if (client == null) {
                client = env.getCli().getCommandContext().getModelControllerClient();
            }

            ModelNode result = client.execute(operation);

            if (Operations.isSuccessfulOutcome(result)) {
                return Operations.readResult(result).asString();
            }

            return "undefined";
        } catch (IOException e) {
            // This should not be needed since a product version is always returned, even if it doesn't exist.
            // In that case "undefined" is returned
            return "undefined";
        }
    }
}
