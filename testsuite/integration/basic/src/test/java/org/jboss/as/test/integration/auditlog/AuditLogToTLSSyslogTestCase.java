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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLIENT_CERT_STORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TLS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TRUSTSTORE;
import static org.jboss.as.domain.management.ModelDescriptionConstants.KEYSTORE_PASSWORD;
import static org.jboss.as.domain.management.ModelDescriptionConstants.KEYSTORE_PATH;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.logging.syslogserver.TLSSyslogServerConfig;
import org.jboss.dmr.ModelNode;
import org.junit.runner.RunWith;
import org.productivity.java.syslog4j.server.SyslogServerConfigIF;

/**
 * Tests TLS protocol of auditlog-to-syslog handler.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(AuditLogToTLSSyslogTestCase.AuditLogToTLSSyslogTestCaseSetup.class)
public class AuditLogToTLSSyslogTestCase extends AuditLogToSyslogTestCase {

    /**
     * {@link org.jboss.as.arquillian.api.ServerSetupTask} implementation which configures syslog server and auditlog-to-syslog
     * handler for this test. It creates key material in a temporary folder in addition to actions described in the parent
     * class.
     *
     * @author Josef Cacek
     */
    static class AuditLogToTLSSyslogTestCaseSetup extends AuditLogToSyslogSetup {

        private static final File WORK_DIR = new File("audit-workdir");

        public static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, "server.keystore");
        public static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, "server.truststore");
        public static final File CLIENT_KEYSTORE_FILE = new File(WORK_DIR, "client.keystore");
        public static final File CLIENT_TRUSTSTORE_FILE = new File(WORK_DIR, "client.truststore");

        private static String PASSWORD = "123456";

        @Override
        protected String getSyslogProtocol() {
            return TLS;
        }

        @Override
        protected ModelNode addAuditlogSyslogProtocol(PathAddress syslogHandlerAddress) {
            ModelNode op = Util.createAddOperation(syslogHandlerAddress.append(PROTOCOL, TLS));
            op.get("message-transfer").set("OCTET_COUNTING");
            return op;
        }

        @Override
        protected SyslogServerConfigIF getSyslogConfig() {
            TLSSyslogServerConfig config = new TLSSyslogServerConfig();
            config.setKeyStore(SERVER_KEYSTORE_FILE.getAbsolutePath());
            config.setKeyStorePassword(PASSWORD);
            config.setTrustStore(SERVER_TRUSTSTORE_FILE.getAbsolutePath());
            config.setTrustStorePassword(PASSWORD);
            return config;
        }

        @Override
        protected List<ModelNode> addProtocolSettings(PathAddress syslogHandlerAddress) {
            PathAddress protocolAddress = syslogHandlerAddress.append(PROTOCOL, TLS);
            List<ModelNode> ops = new ArrayList<ModelNode>();
            ModelNode op1 = Util.createAddOperation(protocolAddress.append(AUTHENTICATION, TRUSTSTORE));
            op1.get(KEYSTORE_PATH).set(CLIENT_TRUSTSTORE_FILE.getAbsolutePath());
            op1.get(KEYSTORE_PASSWORD).set(PASSWORD);
            ops.add(op1);
            ModelNode op2 = Util.createAddOperation(protocolAddress.append(AUTHENTICATION, CLIENT_CERT_STORE));
            op2.get(KEYSTORE_PATH).set(CLIENT_KEYSTORE_FILE.getAbsolutePath());
            op2.get(KEYSTORE_PASSWORD).set(PASSWORD);
            ops.add(op2);
            return ops;
        }

        /**
         * Creates {@link #WORK_DIR} folder and copies keystores and truststores to it. Then calls parent
         * {@link AuditLogToSyslogSetup#setup(ManagementClient, String)} method.
         *
         * @see org.jboss.as.test.integration.auditlog.AuditLogToSyslogSetup#setup(org.jboss.as.arquillian.container.ManagementClient,
         *      java.lang.String)
         */
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            FileUtils.deleteDirectory(WORK_DIR);
            WORK_DIR.mkdirs();
            createTestResource(SERVER_KEYSTORE_FILE);
            createTestResource(SERVER_TRUSTSTORE_FILE);
            createTestResource(CLIENT_KEYSTORE_FILE);
            createTestResource(CLIENT_TRUSTSTORE_FILE);
            super.setup(managementClient, containerId);
        }

        /**
         * Then calls parent {@link AuditLogToSyslogSetup#tearDown(ManagementClient, String)} method and then deletes
         * {@link #WORK_DIR} folder. Creates {@link #WORK_DIR} folder and copies keystores and truststores to it.
         *
         * @see org.jboss.as.test.integration.auditlog.AuditLogToSyslogSetup#tearDown(org.jboss.as.arquillian.container.ManagementClient,
         *      java.lang.String)
         */
        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            super.tearDown(managementClient, containerId);
            FileUtils.deleteDirectory(WORK_DIR);
        }

        /**
         * Copies a resource file from current package to location denoted by given {@link File} instance.
         *
         * @param file
         * @throws IOException
         */
        private void createTestResource(File file) throws IOException {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                IOUtils.copy(getClass().getResourceAsStream(file.getName()), fos);
            } finally {
                IOUtils.closeQuietly(fos);
            }
        }

    }
}
