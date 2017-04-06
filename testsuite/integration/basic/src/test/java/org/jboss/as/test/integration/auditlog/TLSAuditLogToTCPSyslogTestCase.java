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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TLS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.logging.syslogserver.BlockedSyslogServerEventHandler;
import org.jboss.as.test.integration.logging.syslogserver.TCPSyslogServerConfig;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.productivity.java.syslog4j.SyslogConstants;
import org.productivity.java.syslog4j.server.SyslogServerConfigIF;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;

/**
 * Tests that plain TCP messages are not sent when TLS syslog-handler is selected in Audit Log settings. <br>
 * Regression test for WFCORE-190.
 * 
 * @author: Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(TLSAuditLogToTCPSyslogTestCase.AuditLogToTCPSyslogTestCaseSetup.class)
public class TLSAuditLogToTCPSyslogTestCase {

    private static final int ADJUSTED_SECOND = TimeoutUtil.adjust(1000);

    @ContainerResource
    private ManagementClient managementClient;

    protected static SyslogServerIF server;

    private List<Long> properties = new ArrayList<Long>();

    @Test
    public void testAuditLoggingToSyslog() throws Exception {
        final BlockingQueue<SyslogServerEventIF> queue = BlockedSyslogServerEventHandler.getQueue();
        queue.clear();

        SyslogServerEventIF syslogEvent = null;
        try {
            setAuditlogEnabled(true);
            // enabling audit-log is auditable event
            syslogEvent = queue.poll(1 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
            // but we don't expect a message in TCP syslog server
            Assert.assertNull("No message was expected in the syslog, because TCP syslog server is used", syslogEvent);
        } finally {
            setAuditlogEnabled(false);
        }

        for (Long property : properties) {
            Utils.applyUpdate(
                    Util.createRemoveOperation(PathAddress.pathAddress().append(SYSTEM_PROPERTY, Long.toString(property))),
                    managementClient.getControllerClient());
        }
        properties.clear();
    }

    /**
     * Creates a dummy deployment.
     *
     * @return
     */
    @Deployment
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        return war;
    }

    /**
     * Enables/disables the auditlog.
     *
     * @throws Exception
     */
    private void setAuditlogEnabled(boolean value) throws Exception {
        ModelNode op = Util.getWriteAttributeOperation(AuditLogToSyslogSetup.AUDIT_LOG_LOGGER_ADDR, ENABLED, value);
        Utils.applyUpdate(op, managementClient.getControllerClient());
    }

    /**
     * {@link org.jboss.as.arquillian.api.ServerSetupTask} implementation which configures syslog server and auditlog-to-syslog
     * handler for this test.
     */
    static class AuditLogToTCPSyslogTestCaseSetup extends AuditLogToSyslogSetup {

        @Override
        protected String getSyslogProtocol() {
            return SyslogConstants.TCP;
        }

        @Override
        protected SyslogServerConfigIF getSyslogConfig() {
            return new TCPSyslogServerConfig();
        }

        @Override
        protected ModelNode addAuditlogSyslogProtocol(PathAddress syslogHandlerAddress) {
            ModelNode op = Util.createAddOperation(syslogHandlerAddress.append(PROTOCOL, TLS));
            op.get("message-transfer").set("OCTET_COUNTING");
            return op;
        }

    }

}
