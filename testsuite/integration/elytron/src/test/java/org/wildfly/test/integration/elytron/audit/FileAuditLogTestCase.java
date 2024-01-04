/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.audit;

import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.jboss.as.test.shared.CliUtils.asAbsolutePath;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

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
    private static final String ENCODING_16BE = "UTF-16BE";

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
     * Tests audit log file encoding.
     */
    @Test
    @OperateOnDeployment(SD_WITHOUT_LOGIN_PERMISSION)
    public void testAuditLogFileEncoding() throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");

        discardCurrentContents(AUDIT_LOG_FILE);
        Utils.makeCallWithBasicAuthn(servletUrl, USER, PASSWORD, SC_UNAUTHORIZED);

        //Read the logged event using the same encoding "UTF-16BE" as the audit log file setup
        assertTrue(loggedAuthResult(AUDIT_LOG_FILE, USER, UNSUCCESSFUL_PERMISSION_CHECK_EVENT, StandardCharsets.UTF_16BE));
        //Read the logged event using different encoding
        assertFalse(loggedAuthResult(AUDIT_LOG_FILE, USER, UNSUCCESSFUL_PERMISSION_CHECK_EVENT, StandardCharsets.UTF_8));
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
                        .withEncoding(ENCODING_16BE)
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
                FileUtils.deleteDirectory(WORK_DIR);
            }
            ServerReload.reloadIfRequired(managementClient);
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
        return loggedAuthResult(file, user, expectedEvent, StandardCharsets.UTF_16BE);
    }

    private static boolean loggedAuthResult(File file, String user, String expectedEvent, Charset charset) throws Exception {
        List<String> lines = Files.readAllLines(file.toPath(), charset);
        for (String line : lines) {
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
