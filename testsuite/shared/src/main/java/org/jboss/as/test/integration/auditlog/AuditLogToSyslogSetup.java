/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.auditlog;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.APP_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT_LOG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FACILITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOGGER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOG_READ_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSLOG_FORMAT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSLOG_HANDLER;

import java.util.List;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.audit.SyslogAuditLogHandler.Facility;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.logging.syslogserver.BlockedSyslogServerEventHandler;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerConfigIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;

/**
 * Abstract parent for Syslog handler setup in AuditLog.
 *
 * @author Josef Cacek
 */
public abstract class AuditLogToSyslogSetup implements ServerSetupTask {

    private static final String SYSLOG_HANDLER_NAME = "audit-test-syslog-handler";
    private static final String SYSLOG_HANDLER_NAME2 = "audit-test-syslog-handler2";
    private static final int SYSLOG_PORT = 9176;

    private static final PathAddress AUDIT_LOG_ADDRESS = PathAddress.pathAddress().append(CORE_SERVICE, MANAGEMENT)
            .append(ACCESS, AUDIT);
    public static final PathAddress AUDIT_LOG_LOGGER_ADDR = AUDIT_LOG_ADDRESS.append(LOGGER, AUDIT_LOG);
    private static final PathAddress AUDIT_SYSLOG_HANDLER_ADDR = AUDIT_LOG_ADDRESS.append(SYSLOG_HANDLER, SYSLOG_HANDLER_NAME);
    static final PathAddress AUDIT_SYSLOG_HANDLER_ADDR2 = AUDIT_LOG_ADDRESS.append(SYSLOG_HANDLER, SYSLOG_HANDLER_NAME2);
    static final PathAddress AUDIT_LOG_LOGGER_SYSLOG_HANDLER_ADDR = AUDIT_LOG_LOGGER_ADDR.append(HANDLER, SYSLOG_HANDLER_NAME);
    static final PathAddress AUDIT_LOG_LOGGER_SYSLOG_HANDLER_ADDR2 = AUDIT_LOG_LOGGER_ADDR
            .append(HANDLER, SYSLOG_HANDLER_NAME2);

    private SyslogServerIF server;

    private static final String FORMATTER = "formatter";
    private static final String JSON_FORMATTER = "json-formatter";

    //Will need some tweaking in EAP
    static final String DEFAULT_APPNAME = "WildFly";

    /**
     * Returns name of syslog protocol used. It should be one of "tcp", "udp", "tls"
     *
     * @return
     */
    protected abstract String getSyslogProtocol();

    /**
     * Returns a new instance of {@link SyslogServerConfigIF}. It's not necessary to specify host or port, these attributes are
     * configured in {@link #setup(ManagementClient, String)} method.
     *
     * @return
     */
    protected abstract SyslogServerConfigIF getSyslogConfig();

    /**
     * Implementation should return a {@link ModelNode} which configures the syslog protocol in auditlog. It's not necessary to
     * specify host or port, these attributes are configured in {@link #setup(ManagementClient, String)} method.
     *
     * @param the address of the handler for which we are configuring a protocol
     * @return
     */
    protected abstract ModelNode addAuditlogSyslogProtocol(PathAddress syslogHandlerAddress);

    /**
     * If protocol configured in {@link #addAuditlogSyslogProtocol()} has additional settings, child classes can override this
     * method for configuration.
     *
     * @param the address of the handler for which we are configuring a protocol
     * @return
     */
    protected List<ModelNode> addProtocolSettings(PathAddress syslogHandlerAddress) {
        return null;
    }

    /**
     * Starts Syslog server and configures syslog handler in the AS.
     *
     * @see org.jboss.as.arquillian.api.ServerSetupTask#setup(org.jboss.as.arquillian.container.ManagementClient,
     *      java.lang.String)
     */
    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        // clear created server instances (TCP/UDP)
        SyslogServer.shutdown();

        // start and set syslog server
        final String host = Utils.getHost(managementClient);
        SyslogServerConfigIF config = getSyslogConfig();
        config.setUseStructuredData(true);
        config.setHost(host);
        config.setPort(SYSLOG_PORT);
        config.addEventHandler(new BlockedSyslogServerEventHandler());
        final String syslogProtocol = getSyslogProtocol();
        server = SyslogServer.createInstance(syslogProtocol, config);
        // start syslog server
        SyslogServer.getThreadedInstance(syslogProtocol);

        // Add the normal syslog handler
        addSyslogHandler(managementClient, AUDIT_SYSLOG_HANDLER_ADDR, host, null, null);

        // Add the syslog handler we will switch to
        addSyslogHandler(managementClient, AUDIT_SYSLOG_HANDLER_ADDR2, host, "TestApp", Facility.LINE_PRINTER);

        // Reference the first audit logger for now
        ModelNode op = Util.createAddOperation(AUDIT_LOG_LOGGER_SYSLOG_HANDLER_ADDR);
        CoreUtils.applyUpdate(op, managementClient.getControllerClient());

        op = Util.getWriteAttributeOperation(AUDIT_LOG_LOGGER_ADDR, LOG_READ_ONLY, false);
        CoreUtils.applyUpdate(op, managementClient.getControllerClient());

    }

    private void addSyslogHandler(ManagementClient managementClient, PathAddress syslogHandlerAddress, String host,
            String appName, Facility facility) throws Exception {
        ModelNode op = createSyslogHandlerAddComposite(syslogHandlerAddress, host, appName, facility);
        CoreUtils.applyUpdate(op, managementClient.getControllerClient());
        List<ModelNode> protocolSettings = addProtocolSettings(syslogHandlerAddress);
        if (protocolSettings != null) {
            CoreUtils.applyUpdates(protocolSettings, managementClient.getControllerClient());
        }

    }

    private ModelNode createSyslogHandlerAddComposite(PathAddress syslogHandlerAddress, String host, String appName,
            Facility facility) {
        final ModelNode compositeOp = new ModelNode();
        compositeOp.get(OP).set(COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();
        ModelNode steps = compositeOp.get(STEPS);
        ModelNode op = Util.createAddOperation(syslogHandlerAddress);
        op.get(FORMATTER).set(JSON_FORMATTER);
        op.get(SYSLOG_FORMAT).set("RFC5424");
        if (appName != null) {
            op.get(APP_NAME).set(appName);
        }
        if (facility != null) {
            op.get(FACILITY).set(facility.toString());
        }
        steps.add(op);
        final ModelNode auditlogSyslogProtocol = addAuditlogSyslogProtocol(syslogHandlerAddress);
        auditlogSyslogProtocol.get(PORT).set(SYSLOG_PORT);
        auditlogSyslogProtocol.get(HOST).set(host);
        steps.add(auditlogSyslogProtocol);
        return compositeOp;
    }

    /**
     * Stops syslog server and removes auditlog configuration.
     *
     * @see org.jboss.as.arquillian.api.ServerSetupTask#tearDown(org.jboss.as.arquillian.container.ManagementClient,
     *      java.lang.String)
     */
    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        // stop syslog server
        SyslogServer.shutdown();
        server.setThread(null);
        server.getConfig().removeAllEventHandlers();

        removeResource(managementClient, AUDIT_LOG_LOGGER_SYSLOG_HANDLER_ADDR);
        removeResource(managementClient, AUDIT_LOG_LOGGER_SYSLOG_HANDLER_ADDR2);
        removeResource(managementClient, AUDIT_SYSLOG_HANDLER_ADDR2);
        removeResource(managementClient, AUDIT_SYSLOG_HANDLER_ADDR);

        CoreUtils.applyUpdate(Util.getWriteAttributeOperation(AUDIT_LOG_LOGGER_ADDR, LOG_READ_ONLY, false),
                managementClient.getControllerClient());
    }

    private void removeResource(ManagementClient managementClient, PathAddress address) throws Exception {
        PathElement element = address.getLastElement();
        PathAddress parentAddress = address.subAddress(0, address.size() - 1);
        ModelNode op = Util.createOperation(READ_CHILDREN_NAMES_OPERATION, parentAddress);
        op.get(CHILD_TYPE).set(element.getKey());
        ModelNode result = managementClient.getControllerClient().execute(op);
        if (result.hasDefined("result") && result.get("result").asList().contains(new ModelNode(element.getValue()))) {
            // It exists so remove it
            op = Util.createRemoveOperation(address);
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            CoreUtils.applyUpdate(op, managementClient.getControllerClient());
        }

    }
}
