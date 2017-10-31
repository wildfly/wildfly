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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.URL;
import org.codehaus.plexus.util.FileUtils;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.ServerReload;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.FileAuditLog;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.jboss.as.test.shared.CliUtils.asAbsolutePath;
import static org.junit.Assert.assertTrue;
import static org.wildfly.test.integration.elytron.audit.AbstractAuditLogTestCase.SD_WITHOUT_LOGIN_PERMISSION;
import static org.wildfly.test.integration.elytron.audit.AbstractAuditLogTestCase.setEventListenerOfApplicationDomain;

/**
 * Test case for 'file-audit-log' Elytron subsystem resource.
 *
 * @author Jan Tymel
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({AbstractAuditLogTestCase.SecurityDomainSetupTask.class, FileAuditLogTestCase.FileAuditLogSetupTask.class})
public class FileAuditLogTestCase extends AbstractAuditLogTestCase {

    private static final String NAME = FileAuditLogTestCase.class.getSimpleName();
    private static final String AUDIT_LOG_NAME = "test-audit.log";
    private static final File WORK_DIR = new File("target" + File.separatorChar + NAME);
    private static final File AUDIT_LOG_FILE = new File(WORK_DIR, AUDIT_LOG_NAME);

    /**
     * Tests whether successful authentication was logged.
     */
    @Test
    @OperateOnDeployment(SD_DEFAULT)
    public void testSuccessfulAuth() throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");

        discardCurrentContents(AUDIT_LOG_FILE);
        Utils.makeCallWithBasicAuthn(servletUrl, USER, PASSWORD, SC_OK);

        assertTrue("Successful authentication was not logged", loggedSuccessfulAuth(AUDIT_LOG_FILE, USER));
    }

    /**
     * Tests whether failed authentication with wrong user was logged.
     */
    @Test
    @OperateOnDeployment(SD_DEFAULT)
    public void testFailedAuthWrongUser() throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");

        discardCurrentContents(AUDIT_LOG_FILE);
        Utils.makeCallWithBasicAuthn(servletUrl, UNKNOWN_USER, PASSWORD, SC_UNAUTHORIZED);

        assertTrue("Failed authentication with wrong user was not logged", loggedFailedAuth(AUDIT_LOG_FILE, UNKNOWN_USER));
    }

    /**
     * Tests whether failed authentication with wrong password was logged.
     */
    @Test
    @OperateOnDeployment(SD_DEFAULT)
    public void testFailedAuthWrongPassword() throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");

        discardCurrentContents(AUDIT_LOG_FILE);
        Utils.makeCallWithBasicAuthn(servletUrl, USER, WRONG_PASSWORD, SC_UNAUTHORIZED);

        assertTrue("Failed authentication with wrong password was not logged", loggedFailedAuth(AUDIT_LOG_FILE, USER));
    }

    /**
     * Tests whether failed authentication with empty password was logged.
     */
    @Test
    @OperateOnDeployment(SD_DEFAULT)
    public void testFailedAuthEmptyPassword() throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");

        discardCurrentContents(AUDIT_LOG_FILE);
        Utils.makeCallWithBasicAuthn(servletUrl, USER, EMPTY_PASSWORD, SC_UNAUTHORIZED);

        assertTrue("Failed authentication with empty password was not logged", loggedFailedAuth(AUDIT_LOG_FILE, USER));
    }

    /**
     * Tests whether successful permission check was logged.
     */
    @Test
    @OperateOnDeployment(SD_DEFAULT)
    public void testSuccessfulPermissionCheck() throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");

        discardCurrentContents(AUDIT_LOG_FILE);
        Utils.makeCallWithBasicAuthn(servletUrl, USER, PASSWORD, SC_OK);

        assertTrue("Successful permission check was not logged", loggedSuccessfulPermissionCheck(AUDIT_LOG_FILE, USER));
    }

    /**
     * Tests whether failed permission check was logged.
     */
    @Test
    @OperateOnDeployment(SD_WITHOUT_LOGIN_PERMISSION)
    public void testFailedPermissionCheck() throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");

        discardCurrentContents(AUDIT_LOG_FILE);
        Utils.makeCallWithBasicAuthn(servletUrl, USER, PASSWORD, SC_UNAUTHORIZED);

        assertTrue("Failed permission check was not logged", loggedFailedPermissionCheck(AUDIT_LOG_FILE, USER));
    }

    /**
     * Creates Elytron 'file-audit-log' and sets it as ApplicationDomain's security listener.
     */
    static class FileAuditLogSetupTask implements ServerSetupTask {

        FileAuditLog auditLog;

        @Override
        public void setup(ManagementClient managementClient, String string) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                createEmptyDirectory(WORK_DIR);

                auditLog = FileAuditLog.builder().withName(NAME)
                        .withPath(asAbsolutePath(AUDIT_LOG_FILE))
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
                FileUtils.deleteDirectory(WORK_DIR);
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

    }

    private static boolean loggedSuccessfulAuth(File file, String user) throws Exception {
        return loggedAuthResult(file, user, SUCCESSFUL_AUTH_EVENT);
    }

    private static boolean loggedFailedAuth(File file, String user) throws Exception {
        return loggedAuthResult(file, user, UNSUCCESSFUL_AUTH_EVENT);
    }

    private static boolean loggedSuccessfulPermissionCheck(File file, String user) throws Exception {
        return loggedAuthResult(file, user, SUCCESSFUL_PERMISSION_CHECK_EVENT);
    }

    private static boolean loggedFailedPermissionCheck(File file, String user) throws Exception {
        return loggedAuthResult(file, user, UNSUCCESSFUL_PERMISSION_CHECK_EVENT);
    }

    private static boolean loggedAuthResult(File file, String user, String expectedEvent) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(file));

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(expectedEvent) && line.contains(user)) {
                return true;
            }
        }
        return false;
    }

    private static void discardCurrentContents(File file) throws Exception {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.print("");
        }
    }

    private static void createEmptyDirectory(File workDir) throws Exception {
        FileUtils.deleteDirectory(workDir);
        workDir.mkdirs();
        Assert.assertTrue(workDir.exists());
        Assert.assertTrue(workDir.isDirectory());
    }
}
