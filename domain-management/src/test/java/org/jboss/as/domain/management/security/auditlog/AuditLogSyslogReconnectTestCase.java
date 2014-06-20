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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;

import java.net.InetAddress;

import org.jboss.as.controller.audit.SyslogAuditLogHandler.MessageTransfer;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.management.audit.AuditLogHandlerResourceDefinition;
import org.jboss.as.domain.management.audit.SyslogAuditLogHandlerResourceDefinition;
import org.jboss.as.domain.management.audit.SyslogAuditLogProtocolResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Don't use core-model test for this. It does not support runtime, and more importantly for backwards compatibility the audit logger cannot be used
 *
 * @author: Kabir Khan
 */
public class AuditLogSyslogReconnectTestCase extends AbstractAuditLogHandlerTestCase {
    public AuditLogSyslogReconnectTestCase() {
        super(true, false);
    }

    @Test
    public void testAutoReconnect() throws Exception {
        final int timeoutSeconds = 2;
        ModelNode op = createAddSyslogHandlerTcpOperation("syslog", "test-formatter", InetAddress.getByName("localhost"), SYSLOG_PORT, null, MessageTransfer.OCTET_COUNTING);
        op.get(STEPS).asList().get(0).get(SyslogAuditLogHandlerResourceDefinition.MAX_FAILURE_COUNT.getName()).set(3);
        op.get(STEPS).asList().get(1).get(SyslogAuditLogProtocolResourceDefinition.Tcp.RECONNECT_TIMEOUT.getName()).set(timeoutSeconds);
        executeForResult(op);

        SimpleSyslogServer server = SimpleSyslogServer.createTcp(SYSLOG_PORT, true);
        executeForResult(createAddHandlerReferenceOperation("syslog"));
        Assert.assertNotNull(server.receiveData());

        try {
            final ModelNode readResource = Util.createOperation(READ_RESOURCE_OPERATION, AUDIT_ADDR);
            readResource.get(ModelDescriptionConstants.RECURSIVE).set(true);
            readResource.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);

            ModelNode result = executeForResult(readResource);
            Assert.assertNotNull(server.receiveData());
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 3, 0, false);

            int failures = result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog").get(AuditLogHandlerResourceDefinition.FAILURE_COUNT.getName()).asInt();

            server.close();

            result = sendUntilFailure(readResource, failures);
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 3, 1, false);

            result = executeForResult(readResource);
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 3, 2, false);

            long before = System.currentTimeMillis();
            result = executeForResult(readResource);
            //syslog handler should be disabled after 3 failures
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 3, 3, true);

            //Still disabled
            result = executeForResult(readResource);
            if (System.currentTimeMillis() - before < timeoutSeconds * 1000) {
                checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 3, 3, true);
            }

            Thread.sleep(timeoutSeconds * 1000 + 100);

            //Past the timeout, its hould now attempt and fail to reconnect, failure counts should remain the same
            result = executeForResult(readResource);
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 3, 3, true);

            result = executeForResult(readResource);
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 3, 3, true);

            SimpleSyslogServer oldServer = server;
            server = SimpleSyslogServer.createTcp(SYSLOG_PORT, true);

            Thread.sleep(timeoutSeconds * 1000 + 100);

            result = executeForResult(readResource);
            Assert.assertNotNull(server.receiveData());
            //Although it should work again now, this read is what triggered it so the failure count will still be 3, and the handler disabled
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 3, 3, true);

            result = executeForResult(readResource);
            Assert.assertNotNull(server.receiveData());
            //Now that it is all working again, the failure count should be 0 and the handler enabled
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 3, 0, false);

            //Make sure that the old server has no messages
            Assert.assertNull(oldServer.pollData());
        } finally {
            if (server != null) {
                server.close();
            }
        }
    }

    private ModelNode sendUntilFailure(ModelNode readResource, int existingFailures) throws Exception{
        //Since syslog does not have app-layer acks, the failure handling at the TCP layer when the remote socket is shut down is not immediate
        //so we loop around a few times until it eventually fails.
        //Some background information here:
        //      http://blog.gerhards.net/2008/04/on-unreliability-of-plain-tcp-syslog.html
        //      http://blog.gerhards.net/2008/05/why-you-cant-build-reliable-tcp.html

        int expectedFailures = existingFailures + 1;
        for (int i = 0 ; i < 1000 ; i++) {
            ModelNode result = executeForResult(readResource);
            if (expectedFailures == result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog").get(AuditLogHandlerResourceDefinition.FAILURE_COUNT.getName()).asInt()) {
                System.out.println("Number of messages sent until the TCP buffer was full " + i);
                return result;
            }
        }
        throw new AssertionError("Failure count never got incremented");
    }
}
