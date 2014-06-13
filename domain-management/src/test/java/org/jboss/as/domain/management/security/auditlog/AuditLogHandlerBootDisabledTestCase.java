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

package org.jboss.as.domain.management.security.auditlog;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;

import java.io.File;
import java.net.InetAddress;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.management.audit.AuditLogLoggerResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Don't use core-model test for this. It does not support runtime, and more importantly for backwards compatibility the audit logger cannot be used
 *
 * @author: Kabir Khan
 */
public class AuditLogHandlerBootDisabledTestCase extends AbstractAuditLogHandlerTestCase {
    public AuditLogHandlerBootDisabledTestCase() {
        super(false, true);
    }

    @Test
    public void testAuditLoggerBootUp() throws Exception {
        File file = new File(logDir, "test-file.log");
        Assert.assertFalse(file.exists());
    }

    @Test
    public void testEnableAndDisableAuditLogger() throws Exception {
        File file = new File(logDir, "test-file.log");
        Assert.assertFalse(file.exists());

        ModelNode op = createAuditLogWriteAttributeOperation(AuditLogLoggerResourceDefinition.ENABLED.getName(), true);
        executeForResult(op);
        List<ModelNode> records = readFile(file, 1);
        List<ModelNode> ops = checkBootRecordHeader(records.get(0), 1, "core", false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        readFile(file, 2);

        op = createAuditLogWriteAttributeOperation(AuditLogLoggerResourceDefinition.ENABLED.getName(), false);
        executeForResult(op);
        records = readFile(file, 3);
        ops = checkBootRecordHeader(records.get(2), 1, "core", false, false, true);
        checkOpsEqual(op, ops.get(0));
    }

    @Test
    public void testAddRemoveDisabledSyslogHandler() throws Exception {
        //No need to start any syslog server, since audit logging is disabled
        executeForResult(createAddSyslogHandlerUdpOperation("test-syslog", "test-formatter", InetAddress.getByName("localhost"), 6666, null, 0));

        executeForResult(createAddHandlerReferenceOperation("test-syslog"));
        executeForResult(createRemoveHandlerReferenceOperation("test-syslog"));
    }

    @Test
    public void testCanRemoveFormatter() throws Exception {
        File file = new File(logDir, "test-file.log");
        Assert.assertFalse(file.exists());

        ModelNode op = createRemoveHandlerReferenceOperation("test-file");
        executeForResult(op);
    }
}
