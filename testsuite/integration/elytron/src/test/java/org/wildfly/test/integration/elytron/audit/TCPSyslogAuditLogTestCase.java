/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
                        .setMaxReconnectAttempts(5) // an arbitrary number to avoid intermittent failures
                        .build();
                auditLog.create(cli);

                setEventListenerOfApplicationDomain(cli, NAME);
            }
            ServerReload.reloadIfRequired(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                setDefaultEventListenerOfApplicationDomain(cli);

                auditLog.remove(cli);
                stopSyslogServer();
            }
            ServerReload.reloadIfRequired(managementClient);
        }

    }
}
