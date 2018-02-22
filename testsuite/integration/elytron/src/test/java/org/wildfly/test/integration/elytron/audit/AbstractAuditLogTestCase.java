/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware, Inc., and individual contributors
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
import org.wildfly.test.security.common.elytron.UndertowDomainMapper;

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
    protected static final String SD_DEFAULT = "other";
    protected static final String SD_WITHOUT_LOGIN_PERMISSION = "no-login-permission";

    private static final String NAME = "AuditlogTestCase";

    /**
     * Creates WAR with a secured servlet and BASIC authentication configured in web.xml deployment descriptor.
     * It uses default security domain.
     */
    @Deployment(testable = false, name = SD_DEFAULT)
    public static WebArchive standardDeployment() {
        return createWar(SD_DEFAULT);
    }

    /**
     * Creates WAR with a secured servlet and BASIC authentication configured in web.xml deployment descriptor.
     * It uses newly created security domain {@link SD_WITHOUT_LOGIN_PERMISSION}.
     */
    @Deployment(testable = false, name = SD_WITHOUT_LOGIN_PERMISSION)
    public static WebArchive customizedDeployment() {
        return createWar(SD_WITHOUT_LOGIN_PERMISSION)
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(SD_WITHOUT_LOGIN_PERMISSION), "jboss-web.xml");
    }

    /**
     * This {@link ServerSetupTask} creates new security domain in Elytron and Undertow in order to fire
     * permission check fail event.
     */
    static class SecurityDomainSetupTask implements ServerSetupTask {
        SimpleSecurityDomain securityDomain;
        UndertowDomainMapper applicationSecurityDomain;

        @Override
        public void setup(ManagementClient managementClient, String string) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                securityDomain = createSecurityDomainWithoutPermissionMapper(SD_WITHOUT_LOGIN_PERMISSION);
                securityDomain.create(managementClient.getControllerClient(), cli);

                applicationSecurityDomain =  UndertowDomainMapper.builder().withName(SD_WITHOUT_LOGIN_PERMISSION)
                        .withApplicationDomains(SD_WITHOUT_LOGIN_PERMISSION).build();
                applicationSecurityDomain.create(cli);
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                applicationSecurityDomain.remove(cli);
                securityDomain.remove(managementClient.getControllerClient(), cli);
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
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
                SD_WITHOUT_LOGIN_PERMISSION, auditlog));
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
