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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.APP_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FACILITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.logging.syslogserver.BlockedSyslogServerEventHandler;
import org.jboss.as.test.integration.logging.syslogserver.Rfc5424SyslogEvent;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;

/**
 * Test that syslog-handler logs in Audit Log.
 *
 * @author: Ondrej Lukas
 * @author: Josef Cacek
 */
public abstract class AuditLogToSyslogTestCase {

    private static final int ADJUSTED_SECOND = TimeoutUtil.adjust(1000);

    @ContainerResource
    private ManagementClient managementClient;

    protected static SyslogServerIF server;

    private List<Long> properties = new ArrayList<Long>();

    /**
     * Tests following steps in a test syslog server.
     * <ol>
     * <li>throw auditable event with auditlog disabled - check no message came to the syslog</li>
     * <li>enable the auditlog (it's auditable event itself) - check the message in syslog</li>
     * <li>throw auditable event with auditlog enabled - check the mesage in syslog</li>
     * <li>disable and check auditlog (it's auditable event itself) - check the message in syslog</li>
     * <li>check auditable event with auditlog disabled - check no message came to the syslog</li>
     * </ol>
     *
     * @throws Exception
     */
    @Test
    public void testAuditLoggingToSyslog() throws Exception {
        final BlockingQueue<SyslogServerEventIF> queue = BlockedSyslogServerEventHandler.getQueue();
        queue.clear();

        SyslogServerEventIF syslogEvent = null;
        makeOneLog();
        syslogEvent = queue.poll(1 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
        Assert.assertNull("No message was expected in the syslog", syslogEvent);

        try {
            setAuditlogEnabled(true);
            syslogEvent = queue.poll(5 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
            Assert.assertNotNull("Enabling audit log wasn't logged into the syslog", syslogEvent);
            Assert.assertEquals(1, syslogEvent.getFacility());
            assertAppName(AuditLogToSyslogSetup.DEFAULT_APPNAME, syslogEvent);

            makeOneLog();
            syslogEvent = queue.poll(5 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
            Assert.assertNotNull("Auditable event was't logged into the syslog", syslogEvent);
            Assert.assertEquals(1, syslogEvent.getFacility());
            assertAppName(AuditLogToSyslogSetup.DEFAULT_APPNAME, syslogEvent);

            //remove handler
            Utils.applyUpdate(Util.createRemoveOperation(AuditLogToSyslogSetup.AUDIT_LOG_LOGGER_SYSLOG_HANDLER_ADDR), managementClient.getControllerClient());
            syslogEvent = queue.poll(5 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
            Assert.assertNotNull("Auditable event was't logged into the syslog", syslogEvent);
            Assert.assertEquals(1, syslogEvent.getFacility());
            assertAppName(AuditLogToSyslogSetup.DEFAULT_APPNAME, syslogEvent);

            //add other handler which has another appname and facility = LINE_PRINTER (6)
            Utils.applyUpdate(Util.createAddOperation(AuditLogToSyslogSetup.AUDIT_LOG_LOGGER_SYSLOG_HANDLER_ADDR2), managementClient.getControllerClient());
            syslogEvent = queue.poll(5 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
            Assert.assertNotNull("Auditable event was't logged into the syslog", syslogEvent);
            Assert.assertEquals(6, syslogEvent.getFacility());
            assertAppName("TestApp", syslogEvent);

            //Change other handler app name
            Utils.applyUpdate(Util.getWriteAttributeOperation(AuditLogToSyslogSetup.AUDIT_SYSLOG_HANDLER_ADDR2, APP_NAME, new ModelNode("Stuff")), managementClient.getControllerClient());
            syslogEvent = queue.poll(5 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
            Assert.assertNotNull("Auditable event was't logged into the syslog", syslogEvent);
            Assert.assertEquals(6, syslogEvent.getFacility());
            assertAppName("Stuff", syslogEvent);

            //Reset other handler app name
            Utils.applyUpdate(Util.getWriteAttributeOperation(AuditLogToSyslogSetup.AUDIT_SYSLOG_HANDLER_ADDR2, APP_NAME, new ModelNode()), managementClient.getControllerClient());
            syslogEvent = queue.poll(5 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
            Assert.assertNotNull("Auditable event was't logged into the syslog", syslogEvent);
            Assert.assertEquals(6, syslogEvent.getFacility());
            assertAppName(AuditLogToSyslogSetup.DEFAULT_APPNAME, syslogEvent);

            //Change other handler facility = LOCAL_USE_0 (16)
            Utils.applyUpdate(Util.getWriteAttributeOperation(AuditLogToSyslogSetup.AUDIT_SYSLOG_HANDLER_ADDR2, FACILITY, new ModelNode("LOCAL_USE_0")), managementClient.getControllerClient());
            syslogEvent = queue.poll(5 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
            Assert.assertNotNull("Auditable event was't logged into the syslog", syslogEvent);
            Assert.assertEquals(16, syslogEvent.getFacility());
            assertAppName(AuditLogToSyslogSetup.DEFAULT_APPNAME, syslogEvent);

            //Reset other handler facility
            Utils.applyUpdate(Util.getWriteAttributeOperation(AuditLogToSyslogSetup.AUDIT_SYSLOG_HANDLER_ADDR2, FACILITY, new ModelNode()), managementClient.getControllerClient());
            syslogEvent = queue.poll(5 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
            Assert.assertNotNull("Auditable event was't logged into the syslog", syslogEvent);
            Assert.assertEquals(1, syslogEvent.getFacility());
            assertAppName(AuditLogToSyslogSetup.DEFAULT_APPNAME, syslogEvent);

        } finally {
            setAuditlogEnabled(false);
        }
        syslogEvent = queue.poll(5 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
        Assert.assertNotNull("Disabling audit log wasn't logged into the syslog", syslogEvent);
        makeOneLog();
        syslogEvent = queue.poll(1 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
        Assert.assertNull("No message was expected in the syslog", syslogEvent);

        for (Long property : properties) {
            Utils.applyUpdate(
                    Util.createRemoveOperation(PathAddress.pathAddress().append(SYSTEM_PROPERTY, Long.toString(property))),
                    managementClient.getControllerClient());
        }
        properties.clear();
    }

    void assertAppName(String expected, SyslogServerEventIF syslogEvent) {
        Rfc5424SyslogEvent event = (Rfc5424SyslogEvent)syslogEvent;
        Assert.assertEquals(expected, event.getAppName());
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
     * Throws auditable event. This implemetation writes a system-property to an AS configuration
     *
     * @throws Exception
     */
    protected void makeOneLog() throws Exception {
        long timeStamp = System.currentTimeMillis();
        properties.add(Long.valueOf(timeStamp));
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress().append(SYSTEM_PROPERTY, Long.toString(timeStamp)));
        op.get(NAME).set(NAME);
        op.get(VALUE).set("someValue");
        Utils.applyUpdate(op, managementClient.getControllerClient());
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

}
