/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.elytron.audit;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.syslogserver.TCPSyslogServerConfig;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.SyslogAuditLog;

import static org.productivity.java.syslog4j.SyslogConstants.TCP;
import static org.wildfly.test.integration.elytron.audit.AbstractAuditLogTestCase.setEventListenerOfApplicationDomain;
import static org.wildfly.test.integration.elytron.audit.AbstractSyslogAuditLogTestCase.setupAndStartSyslogServer;

/**
 * Class for particular settings for 'syslog-audit-log' Elytron subsystem resource that communicates over TCP protocol.
 * Tests being run with this settings can be seen in {@link AbstractSyslogAuditLogTestCase}.
 *
 * @author Jan Tymel
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({AbstractAuditLogTestCase.SecurityDomainSetupTask.class, TCPSyslogAuditLogTestCase.SyslogAuditLogSetupTask.class})
public class TCPSyslogAuditLogTestCase extends AbstractSyslogAuditLogTestCase {

    private static final String NAME = TCPSyslogAuditLogTestCase.class.getSimpleName();
    private static final String HOSTNAME = "hostname-" + NAME;
    private static final int PORT = 10514;

    /**
     * Creates Elytron 'syslog-audit-log' and sets it as ApplicationDomain's security listener.
     */
    static class SyslogAuditLogSetupTask implements ServerSetupTask {

        SyslogAuditLog auditLog;

        @Override
        public void setup(ManagementClient managementClient, String string) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                String host = CoreUtils.stripSquareBrackets(managementClient.getMgmtAddress());

                final TCPSyslogServerConfig config = new TCPSyslogServerConfig();
                setupAndStartSyslogServer(config, host, PORT, TCP);

                auditLog = SyslogAuditLog.builder().withName(NAME)
                        .withServerAddress(managementClient.getMgmtAddress())
                        .withPort(PORT)
                        .withHostName(HOSTNAME)
                        .withTransportProtocol("TCP")
                        .build();
                auditLog.create(cli);

                setEventListenerOfApplicationDomain(cli, NAME);
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                setDefaultEventListenerOfApplicationDomain(cli);

                auditLog.remove(cli);
                stopSyslogServer();
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

    }
}
