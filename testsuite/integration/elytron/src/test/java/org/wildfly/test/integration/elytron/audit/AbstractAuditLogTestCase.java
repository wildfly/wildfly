/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.audit;

import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

/**
 * Abstract class for Elytron Audit Logging tests. It provides a deployment with {@link SimpleServlet} and a couple of helper
 * methods.
 *
 * @author Jan Tymel
 */
public abstract class AbstractAuditLogTestCase {

    @ArquillianResource
    protected URL url;

    protected static final String SUCCESSFUL_AUTH_EVENT = "SecurityAuthenticationSuccessfulEvent";
    protected static final String UNSUCCESSFUL_AUTH_EVENT = "SecurityAuthenticationFailedEvent";
    protected static final String SUCCESSFUL_PERMISSION_CHECK_EVENT = "SecurityPermissionCheckSuccessfulEvent";
    protected static final String UNSUCCESSFUL_PERMISSION_CHECK_EVENT = "SecurityPermissionCheckFailedEvent";

    protected static final String USER = "user1";
    protected static final String UNKNOWN_USER = "unknown-user";
    protected static final String PASSWORD = "password1";
    protected static final String WRONG_PASSWORD = "wrongPassword";
    protected static final String EMPTY_PASSWORD = "";
    protected static final String SD_DEFAULT_BASIC = "other";
    protected static final String SD_WITHOUT_LOGIN_PERMISSION_BASIC = "no-login-permission";
    protected static final String SD_DEFAULT_DIGEST = "other-digest";
    protected static final String SD_WITHOUT_LOGIN_PERMISSION_DIGEST = "no-login-permission-digest";

    private static final String NAME = "AuditlogTestCase";

    /**
     * Creates WAR with a secured servlet and BASIC authentication configured in web.xml deployment descriptor.
     * It uses default security domain.
     */
    @Deployment(testable = false, name = SD_DEFAULT_BASIC)
    public static WebArchive standardDeployment() {
        return createWar(SD_DEFAULT_BASIC);
    }

    /**
     * Creates WAR with a secured servlet and BASIC authentication configured in
     * web.xml deployment descriptor. It uses newly created security domain
     * {@link #SD_WITHOUT_LOGIN_PERMISSION_BASIC}.
     */
    @Deployment(testable = false, name = SD_WITHOUT_LOGIN_PERMISSION_BASIC)
    public static WebArchive customizedDeployment() {
        return createWar(SD_WITHOUT_LOGIN_PERMISSION_BASIC)
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(SD_WITHOUT_LOGIN_PERMISSION_BASIC), "jboss-web.xml");
    }

    /**
     * Creates WAR with a secured servlet and DIGEST authentication configured in
     * web.xml deployment descriptor.
     * It uses default security domain.
     */
    @Deployment(testable = false, name = SD_DEFAULT_DIGEST)
    public static WebArchive digestDeployment() {
        return ShrinkWrap.create(WebArchive.class, SD_DEFAULT_DIGEST + ".war")
                .addClasses(SimpleServlet.class)
                .addAsWebInfResource(AbstractAuditLogTestCase.class.getPackage(), "DigestAuthentication-web.xml",
                        "web.xml");
    }

    /**
     * Creates WAR with a secured servlet and DIGEST authentication configured in
     * web.xml deployment descriptor. It uses the security domain without a
     * permission mapper ({@link #SD_WITHOUT_LOGIN_PERMISSION_DIGEST}).
     */
    @Deployment(testable = false, name = SD_WITHOUT_LOGIN_PERMISSION_DIGEST)
    public static WebArchive digestCustomizedDeployment() {
        return ShrinkWrap.create(WebArchive.class, SD_WITHOUT_LOGIN_PERMISSION_DIGEST + ".war")
                .addClasses(SimpleServlet.class)
                .addAsWebInfResource(AbstractAuditLogTestCase.class.getPackage(), "DigestAuthentication-web.xml",
                        "web.xml")
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(SD_WITHOUT_LOGIN_PERMISSION_DIGEST), "jboss-web.xml");
    }

    /**
     * This {@link ServerSetupTask} creates new security domain in Elytron and Undertow in order to fire
     * permission check fail event.
     */
    static class SecurityDomainSetupTask implements ServerSetupTask {
        SimpleSecurityDomain basicSecurityDomain;
        UndertowApplicationSecurityDomain basicApplicationSecurityDomain;
        SimpleSecurityDomain digestSecurityDomain;
        UndertowApplicationSecurityDomain digestApplicationSecurityDomain;

        @Override
        public void setup(ManagementClient managementClient, String string) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                basicSecurityDomain = createSecurityDomainWithoutPermissionMapper(SD_WITHOUT_LOGIN_PERMISSION_BASIC);
                basicSecurityDomain.create(managementClient.getControllerClient(), cli);

                basicApplicationSecurityDomain = UndertowApplicationSecurityDomain.builder()
                        .withName(SD_WITHOUT_LOGIN_PERMISSION_BASIC)
                        .withSecurityDomain(SD_WITHOUT_LOGIN_PERMISSION_BASIC)
                        .build();
                basicApplicationSecurityDomain.create(managementClient.getControllerClient(), cli);

                digestSecurityDomain = createSecurityDomainWithoutPermissionMapper(SD_WITHOUT_LOGIN_PERMISSION_DIGEST);
                digestSecurityDomain.create(managementClient.getControllerClient(), cli);

                digestApplicationSecurityDomain = UndertowApplicationSecurityDomain.builder()
                        .withName(SD_WITHOUT_LOGIN_PERMISSION_DIGEST)
                        .withSecurityDomain(SD_WITHOUT_LOGIN_PERMISSION_DIGEST)
                        .build();
                digestApplicationSecurityDomain.create(managementClient.getControllerClient(), cli);
            }
            ServerReload.reloadIfRequired(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                basicApplicationSecurityDomain.remove(managementClient.getControllerClient(), cli);
                basicSecurityDomain.remove(managementClient.getControllerClient(), cli);
                digestApplicationSecurityDomain.remove(managementClient.getControllerClient(), cli);
                digestSecurityDomain.remove(managementClient.getControllerClient(), cli);
            }
            ServerReload.reloadIfRequired(managementClient);
        }
    }

    protected static void setDefaultEventListenerOfApplicationDomain(CLIWrapper cli) {
        setEventListenerOfApplicationDomain(cli, "local-audit");
    }

    protected static void setEventListenerOfApplicationDomain(CLIWrapper cli, String auditlog) {
        cli.sendLine(String.format(
                "/subsystem=elytron/security-domain=ApplicationDomain:write-attribute(name=security-event-listener,value=%s)",
                auditlog));
        cli.sendLine(String.format(
                "/subsystem=elytron/security-domain=%s:write-attribute(name=security-event-listener,value=%s)",
                SD_WITHOUT_LOGIN_PERMISSION_BASIC, auditlog));
        cli.sendLine(String.format(
                "/subsystem=elytron/security-domain=%s:write-attribute(name=security-event-listener,value=%s)",
                SD_WITHOUT_LOGIN_PERMISSION_DIGEST, auditlog));
    }

    protected static SimpleSecurityDomain createSecurityDomainWithoutPermissionMapper(String domainName) {
        return SimpleSecurityDomain.builder().withName(domainName)
                .withDefaultRealm("ApplicationFsRealm")
                .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                        .withRealm("ApplicationFsRealm")
                        .withRoleDecoder("groups-to-roles").build())
                .build();
    }

    private static WebArchive createWar(String warName) {
        return ShrinkWrap.create(WebArchive.class, warName + ".war")
                .addClasses(SimpleServlet.class)
                .addAsWebInfResource(FileAuditLogTestCase.class.getPackage(), "BasicAuthentication-web.xml", "web.xml");
    }
}
