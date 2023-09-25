/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.audit;

import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.URL;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.server.event.SecurityAuthenticationFailedEvent;
import org.wildfly.security.auth.server.event.SecurityAuthenticationSuccessfulEvent;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({AbstractAuditLogTestCase.SecurityDomainSetupTask.class, CustomSecurityEventListenerTestCase.CustomListenerSetupTask.class})
public class CustomSecurityEventListenerTestCase extends AbstractAuditLogTestCase {

    private static final String NAME = CustomSecurityEventListener.class.getSimpleName();
    private static final String AUDIT_LOG_NAME = NAME + ".log";
    private static final File AUDIT_LOG_FILE = new File("target", AUDIT_LOG_NAME);

    /**
     * Tests whether successful authentication was logged.
     */
    @Test
    @OperateOnDeployment(SD_DEFAULT)
    public void testSuccessfulAuth() throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");

        discardCurrentContents();
        Utils.makeCallWithBasicAuthn(servletUrl, USER, PASSWORD, SC_OK);

        assertEquals("Successful authentication was not logged", "testingValue:" + SecurityAuthenticationSuccessfulEvent.class.getName(), getContent());
    }

    /**
     * Tests whether failed authentication with wrong user was logged.
     */
    @Test
    @OperateOnDeployment(SD_DEFAULT)
    public void testFailedAuthWrongUser() throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");

        discardCurrentContents();
        Utils.makeCallWithBasicAuthn(servletUrl, UNKNOWN_USER, PASSWORD, SC_UNAUTHORIZED);

        assertEquals("Failed authentication was not logged", "testingValue:" + SecurityAuthenticationFailedEvent.class.getName(), getContent());
    }

    static class CustomListenerSetupTask implements ServerSetupTask {

        static final Class<?> listenerClass = CustomSecurityEventListener.class;
        private final TestModule module;

        CustomListenerSetupTask() {
            module = new TestModule(listenerClass.getName(), "org.wildfly.security.elytron");
            JavaArchive auditJar = module.addResource(listenerClass.getSimpleName() + ".jar");
            auditJar.addClass(listenerClass);
        }

        @Override
        public void setup(ManagementClient managementClient, String string) throws Exception {
            module.create(true);
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine("/subsystem=elytron/custom-security-event-listener=" + NAME + ":add(" +
                        "module=\"" + listenerClass.getName() + "\", " +
                        "class-name=\"" + listenerClass.getName() + "\"," +
                        "configuration={ testingAttribute = testingValue })");

                setEventListenerOfApplicationDomain(cli, NAME);
            }
            ServerReload.reloadIfRequired(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                setDefaultEventListenerOfApplicationDomain(cli);

                cli.sendLine("/subsystem=elytron/custom-security-event-listener=" + NAME + ":remove");
            }
            module.remove();
            ServerReload.reloadIfRequired(managementClient);
        }

    }

    private static void discardCurrentContents() throws Exception {
        try (PrintWriter writer = new PrintWriter(AUDIT_LOG_FILE)) {
            writer.print("");
        }
    }

    private static String getContent() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(AUDIT_LOG_FILE));
        return reader.readLine();
    }
}
