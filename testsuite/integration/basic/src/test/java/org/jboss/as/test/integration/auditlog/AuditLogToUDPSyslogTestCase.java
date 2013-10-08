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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UDP;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.logging.syslogserver.UDPSyslogServerConfig;
import org.jboss.dmr.ModelNode;
import org.junit.runner.RunWith;
import org.productivity.java.syslog4j.SyslogConstants;
import org.productivity.java.syslog4j.server.SyslogServerConfigIF;

/**
 * Tests UDP protocol of auditlog-to-syslog handler.
 * 
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(AuditLogToUDPSyslogTestCase.AuditLogToUDPSyslogTestCaseSetup.class)
public class AuditLogToUDPSyslogTestCase extends AuditLogToSyslogTestCase {

    /**
     * {@link org.jboss.as.arquillian.api.ServerSetupTask} implementation which configures syslog server and auditlog-to-syslog
     * handler for this test.
     */
    static class AuditLogToUDPSyslogTestCaseSetup extends AuditLogToSyslogSetup {

        @Override
        protected ModelNode addAuditlogSyslogProtocol() {
            return Util.createAddOperation(AUDIT_SYSLOG_HANDLER_ADDR.append(PROTOCOL, UDP));
        }

        @Override
        protected String getSyslogProtocol() {
            return SyslogConstants.UDP;
        }

        @Override
        protected SyslogServerConfigIF getSyslogConfig() {
            return new UDPSyslogServerConfig();
        }
    }

}
