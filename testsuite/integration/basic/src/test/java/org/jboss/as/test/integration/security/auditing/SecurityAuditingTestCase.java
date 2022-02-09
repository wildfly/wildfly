/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.security.auditing;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.ejb.security.AnnSBTest;
import org.jboss.as.test.integration.ejb.security.authorization.SingleMethodsAnnOnlyCheckSFSB;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.ElytronDomainSetup;
import org.wildfly.test.security.common.elytron.ServletElytronDomainSetup;

/**
 * This class tests Security auditing functionality
 *
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({SecurityAuditingTestCase.SecurityAuditingTestCaseSetup.class})
public class SecurityAuditingTestCase extends AnnSBTest {

    private static final Logger log = Logger.getLogger(testClass());

    private static File auditLog = new File(System.getProperty("jboss.home", null), "standalone" + File.separator + "log" + File.separator + "audit.log");

    static class SecurityAuditingTestCaseSetup extends AbstractMgmtTestBase implements ServerSetupTask {

        /**
         * The LOGGING
         */
        private static final String LOGGING = "logging";
        private static final String SECURITY_DOMAIN_NAME = "form-auth";

        private AutoCloseable snapshot;
        private ElytronDomainSetup elytronDomainSetup;
        private ServletElytronDomainSetup servletElytronDomainSetup;
        private ManagementClient managementClient;

        protected static String getUsersFile() {
            return new File(SecurityAuditingTestCase.class.getResource("form-auth/users.properties").getFile()).getAbsolutePath();
        }

        protected static String getGroupsFile() {
            return new File(SecurityAuditingTestCase.class.getResource("form-auth/roles.properties").getFile()).getAbsolutePath();
        }

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            this.managementClient = managementClient;
            snapshot = ServerSnapshot.takeSnapshot(managementClient);
            final List<ModelNode> updates = new ArrayList<ModelNode>();

            ModelNode op = new ModelNode();
            elytronDomainSetup = new ElytronDomainSetup(getUsersFile(), getGroupsFile(), SECURITY_DOMAIN_NAME);
            servletElytronDomainSetup = new ServletElytronDomainSetup(SECURITY_DOMAIN_NAME, false);

            elytronDomainSetup.setup(managementClient, containerId);
            servletElytronDomainSetup.setup(managementClient, containerId);

            // /subsystem=elytron/security-domain=form-auth:write-attribute(name=security-event-listener, value=local-audit)
            op = new ModelNode();
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(OP_ADDR).add(SUBSYSTEM, "elytron");
            op.get(OP_ADDR).add("security-domain", SECURITY_DOMAIN_NAME);
            op.get("name").set("security-event-listener");
            op.get("value").set("local-audit");
            updates.add(op);

            // /subsystem=elytron/security-domain=ApplicationDomain:write-attribute(name=security-event-listener, value=local-audit)
            op = new ModelNode();
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(OP_ADDR).add(SUBSYSTEM, "elytron");
            op.get(OP_ADDR).add("security-domain", "ApplicationDomain");
            op.get("name").set("security-event-listener");
            op.get("value").set("local-audit");
            updates.add(op);

            executeOperations(updates);

            ServerReload.executeReloadAndWaitForCompletion(managementClient, 50000);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            snapshot.close();

            // /subsystem=elytron/security-domain=ApplicationDomain:undefine-attribute(name=security-event-listener)
            final List<ModelNode> updates = new ArrayList<ModelNode>();
            ModelNode op = new ModelNode();
            op.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
            op.get(OP_ADDR).add(SUBSYSTEM, "elytron");
            op.get(OP_ADDR).add("security-domain", SECURITY_DOMAIN_NAME);
            op.get("name").set("security-event-listener");
            updates.add(op);
            executeOperations(updates);

            elytronDomainSetup.tearDown(managementClient, containerId);
            servletElytronDomainSetup.tearDown(managementClient, containerId);
        }

        @Override
        protected ModelControllerClient getModelControllerClient() {
            return managementClient.getControllerClient();
        }
    }

    @Deployment
    public static WebArchive deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "form-auth.war");
        war.setWebXML("org/jboss/as/test/integration/security/auditing/form-auth/web.xml");
        war.addAsWebInfResource("org/jboss/as/test/integration/security/auditing/form-auth/jboss-web.xml");
        war.addAsWebResource("org/jboss/as/test/integration/security/auditing/form-auth/index.jsp");
        war.addAsWebResource("org/jboss/as/test/integration/security/auditing/form-auth/login.jsp");
        war.addAsWebResource("org/jboss/as/test/integration/security/auditing/form-auth/loginerror.jsp");
        war.addAsWebResource("org/jboss/as/test/integration/security/auditing/form-auth/logout.jsp");
        war.addAsResource("org/jboss/as/test/integration/security/auditing/form-auth/users.properties", "/users.properties");
        war.addAsResource("org/jboss/as/test/integration/security/auditing/form-auth/roles.properties", "/roles.properties");
        return war;
    }

    private static final String MODULE = "singleMethodsAnnOnlySFSB";

    private static Class<?> testClass() {
        return SecurityAuditingTestCase.class;
    }

    private static Class<?> beanClass() {
        return SingleMethodsAnnOnlyCheckSFSB.class;
    }

    @Deployment(name = MODULE + ".jar", order = 1, testable = false)
    public static Archive<JavaArchive> deployment() {
        return testAppDeployment(Logger.getLogger(testClass()), MODULE, beanClass());
    }

    /**
     * Basic test if auditing works in EJB module.
     *
     * @throws Exception
     */
    @Test
    public void testSingleMethodAnnotationsUser1Template() throws Exception {

        assertTrue("Audit log file has not been created (" + auditLog.getAbsolutePath() + ")", auditLog.exists());
        assertTrue("Audit log file is closed for reading (" + auditLog.getAbsolutePath() + ")", auditLog.canRead());

        BufferedReader reader = Files.newBufferedReader(auditLog.toPath(), StandardCharsets.UTF_8);

        while (reader.readLine() != null) {
            // we need to get trough all old records (if any)
        }

        testSingleMethodAnnotationsUser1Template(MODULE, log, beanClass());

        checkAuditLog(reader, "SecurityAuthenticationSuccessfulEvent.*\"name\":\"user1\"");
    }

    /**
     * Basic test if auditing works for web module.
     *
     * @param url
     * @throws Exception
     */
    @Test
    public void test(@ArquillianResource URL url) throws Exception {

        assertTrue("Audit log file has not been created (" + auditLog.getAbsolutePath() + ")", auditLog.exists());
        assertTrue("Audit log file is closed for reading (" + auditLog.getAbsolutePath() + ")", auditLog.canRead());

        BufferedReader reader = Files.newBufferedReader(auditLog.toPath(), StandardCharsets.UTF_8);

        while (reader.readLine() != null) {
            // we need to get trough all old records (if any)
        }

        Utils.makeCall(url.toString(), "anil", "anil", 200);

        checkAuditLog(reader, "SecurityAuthenticationSuccessfulEvent.*\"name\":\"anil\"");
    }

    private void checkAuditLog(BufferedReader reader, String regex) throws Exception {

        Pattern successPattern = Pattern.compile(regex);

        // we'll be actively waiting for a given INTERVAL for the record to appear
        final long INTERVAL = TimeoutUtil.adjust(5000);
        long startTime = System.currentTimeMillis();
        String line;
        search_for_log:
        while (true) {
            // some new lines were added -> go trough those and check whether our record is present
            while (null != (line = reader.readLine())) {
                if (successPattern.matcher(line).find()) {
                    break search_for_log;
                }
            }
            // record not written to log yet -> continue checking if the time has not yet expired
            if (System.currentTimeMillis() > startTime + INTERVAL) {
                // time expired
                throw new AssertionError("Login record has not been written into audit log! (time expired). Checked regex="
                        + regex);
            }
        }

    }

}
