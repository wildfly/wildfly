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

import java.io.File;
import org.codehaus.plexus.util.FileUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.syslogserver.TLSSyslogServerConfig;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.ClientSslContext;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.KeyStore;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.SimpleClientSslContext;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.security.common.elytron.SimpleTrustManager;
import org.wildfly.test.security.common.elytron.SyslogAuditLog;
import org.wildfly.test.security.common.elytron.TrustManager;

import static org.jboss.as.test.integration.security.common.SecurityTestConstants.KEYSTORE_PASSWORD;
import static org.wildfly.test.integration.elytron.audit.AbstractAuditLogTestCase.setEventListenerOfApplicationDomain;
import static org.wildfly.test.integration.elytron.audit.AbstractSyslogAuditLogTestCase.setupAndStartSyslogServer;

/**
 * Class for particular settings for 'syslog-audit-log' Elytron subsystem resource that communicates over TLS protocol. Tests
 * being run with this settings can be seen in {@link AbstractSyslogAuditLogTestCase}.
 *
 * @author Jan Tymel
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({AbstractAuditLogTestCase.SecurityDomainSetupTask.class, TLSSyslogAuditLogTestCase.SyslogAuditLogSetupTask.class})
public class TLSSyslogAuditLogTestCase extends AbstractSyslogAuditLogTestCase {

    private static final String NAME = TLSSyslogAuditLogTestCase.class.getSimpleName();
    private static final String HOSTNAME = "hostname-" + NAME;
    private static final int PORT = 10514;

    private static final String TRUST_STORE_NAME = "trust-store-" + NAME;
    private static final String TRUST_MANAGER_NAME = "trust-manager-" + NAME;
    private static final String SSL_CONTEXT_NAME = "ssl-context" + NAME;

    private static final File WORK_DIR = new File("target" + File.separatorChar + NAME);
    private static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    private static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_TRUSTSTORE);

    /**
     * Creates Elytron 'syslog-audit-log' and sets it as ApplicationDomain's security listener.
     */
    static class SyslogAuditLogSetupTask implements ServerSetupTask {

        TrustManager trustManager;
        KeyStore trustStore;
        ClientSslContext sslContext;
        SyslogAuditLog auditLog;

        @Override
        public void setup(ManagementClient managementClient, String string) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                keyMaterialSetup(WORK_DIR);

                trustStore = SimpleKeyStore.builder().withName(TRUST_STORE_NAME)
                        .withPath(Path.builder().withPath(SERVER_KEYSTORE_FILE.getPath()).build())
                        .withCredentialReference(CredentialReference.builder().withClearText(KEYSTORE_PASSWORD).build())
                        .withType("JKS")
                        .build();
                trustStore.create(cli);

                trustManager = SimpleTrustManager.builder().withName(TRUST_MANAGER_NAME)
                        .withKeyStore(TRUST_STORE_NAME)
                        .build();
                trustManager.create(cli);

                sslContext = SimpleClientSslContext.builder().withName(SSL_CONTEXT_NAME)
                        .withTrustManager(TRUST_MANAGER_NAME)
                        .build();
                sslContext.create(cli);

                final String host = CoreUtils.stripSquareBrackets(managementClient.getMgmtAddress());
                final TLSSyslogServerConfig config = getTlsSyslogConfig();
                setupAndStartSyslogServer(config, host, PORT, "TLS");

                auditLog = SyslogAuditLog.builder().withName(NAME)
                        .withServerAddress(managementClient.getMgmtAddress())
                        .withPort(PORT)
                        .withHostName(HOSTNAME)
                        .withTransportProtocol("SSL_TCP")
                        .withSslContext(SSL_CONTEXT_NAME)
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

                sslContext.remove(cli);
                trustManager.remove(cli);
                trustStore.remove(cli);
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

    }

    private static void keyMaterialSetup(File workDir) throws Exception {
        FileUtils.deleteDirectory(workDir);
        workDir.mkdirs();
        Assert.assertTrue(workDir.exists());
        Assert.assertTrue(workDir.isDirectory());
        CoreUtils.createKeyMaterial(workDir);
    }

    private static TLSSyslogServerConfig getTlsSyslogConfig() {
        TLSSyslogServerConfig config = new TLSSyslogServerConfig();
        config.setKeyStore(SERVER_KEYSTORE_FILE.getAbsolutePath());
        config.setKeyStorePassword(KEYSTORE_PASSWORD);
        config.setTrustStore(SERVER_TRUSTSTORE_FILE.getAbsolutePath());
        config.setTrustStorePassword(KEYSTORE_PASSWORD);

        return config;
    }
}
