/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.jboss.as.test.integration.security.common.Utils.REDIRECT_STRATEGY;
import static org.junit.Assert.assertEquals;
import static org.wildfly.test.manual.elytron.seccontext.AbstractSecurityContextPropagationTestBase.server1;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.SERVER1_BACKUP;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_ENTRY_SERVLET_FORM;

import java.io.IOException;
import java.net.URL;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests authorization forwarding within a cluster.
 *
 * <h3>Given</h3>
 * See the superclass for common implementation details.
 * <pre>
 * Additional started and configured servers:
 * - seccontext-server1-backup (standalone-ha.xml - creates cluster with seccontext-server1) -
 *   * entry-servlet-form.war
 * </pre>
 * @author Josef Cacek
 */
public abstract class AbstractHAAuthorizationForwardingTestCase extends AbstractSecurityContextPropagationTestBase {

    private static final ServerHolder server1backup = new ServerHolder(SERVER1_BACKUP, TestSuiteEnvironment.getServerAddress(),
            2000);

    /**
     * Creates deployment with Entry servlet and FORM authentication.
     */
    @Deployment(name = WAR_ENTRY_SERVLET_FORM + "backup", managed = false, testable = false)
    @TargetsContainer(SERVER1_BACKUP)
    public static Archive<?> createDeploymentForBackup() {
        return createEntryServletFormAuthnDeployment();
    }

    /**
     * Start server1backup.
     */
    @Before
    public void startServer1backup() throws CommandLineException, IOException, MgmtOperationException {
        server1backup.resetContainerConfiguration(new ServerConfigurationBuilder()
                .withDeployments(WAR_ENTRY_SERVLET_FORM + "backup")
                .build());
    }

    /**
     * Shut down server1backup.
     */
    @AfterClass
    public static void shutdownServer1backup() throws IOException {
        server1backup.shutDown();
    }

    /**
     * Verifies, the distributable web-app with FORM authentication supports session replication out of the box.
     *
     * <pre>
     * When: HTTP client calls WhoAmIServlet as "admin" (using FORM authn) on first cluster node and then
     *       it calls WhoAmIServlet (without authentication needed) on the second cluster node
     * Then: the call to WhoAmIServlet on second node (without authentication) passes and returns "admin"
     *       (i.e. SSO works with FORM authentication)
     * </pre>
     */
    @Test
    public void testServletSso() throws Exception {
        final URL whoamiUrl = new URL(
                server1.getApplicationHttpUrl() + "/" + WAR_ENTRY_SERVLET_FORM + WhoAmIServlet.SERVLET_PATH);
        final URL whoamiBackupUrl = new URL(
                server1backup.getApplicationHttpUrl() + "/" + WAR_ENTRY_SERVLET_FORM + WhoAmIServlet.SERVLET_PATH);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
            assertEquals("Unexpected result from WhoAmIServlet", "admin",
                    doHttpRequestFormAuthn(httpClient, whoamiUrl, true, "admin", "admin", SC_OK));
            assertEquals("Unexpected result from WhoAmIServlet (backup-server)", "admin",
                    doHttpRequest(httpClient, whoamiBackupUrl, SC_OK));
        }
    }

    /**
     * Verifies, the authorization forwarding works within cluster (FORM authn). This simulates failover on
     * distributed web application (e.g. when load balancer is used).
     *
     * <pre>
     * When: HTTP client calls WhoAmIServlet as "admin" (using FORM authn) on second cluster node and then
     *       it calls EntryServlet (without authentication needed) on the first cluster node;
     *       the EntryServlet uses Elytron API to forward authz name to call remote WhoAmIBean
     * Then: the calls pass and WhoAmIBean returns "admin" username
     * </pre>
     */
    @Test
    public void testServletSsoPropagation() throws Exception {
        final URL entryServletUrl = getEntryServletUrl(WAR_ENTRY_SERVLET_FORM, "server", "server",
                ReAuthnType.FORWARDED_AUTHORIZATION);
        final URL whoamiUrl = new URL(
                server1backup.getApplicationHttpUrl() + "/" + WAR_ENTRY_SERVLET_FORM + WhoAmIServlet.SERVLET_PATH);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
            assertEquals("Unexpected result from WhoAmIServlet (backup-server)", "admin",
                    doHttpRequestFormAuthn(httpClient, whoamiUrl, true, "admin", "admin", SC_OK));
            assertEquals("Unexpected result from EntryServlet", "admin", doHttpRequest(httpClient, entryServletUrl, SC_OK));
        }
    }
}
