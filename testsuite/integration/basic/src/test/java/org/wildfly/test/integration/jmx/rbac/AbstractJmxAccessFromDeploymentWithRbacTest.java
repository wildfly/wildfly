/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.jmx.rbac;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.security.ControllerPermission;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.security.permission.ElytronPermission;
import org.wildfly.test.integration.jmx.rbac.deployment.JmxResource;

import java.io.FilePermission;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROVIDER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLE_MAPPING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USE_IDENTITY_ROLES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.management.MBeanServerPermission;

public abstract class AbstractJmxAccessFromDeploymentWithRbacTest {

    @ArquillianResource
    private URL url;

    private boolean securedApplication;

    AbstractJmxAccessFromDeploymentWithRbacTest() {
        this(true);
    }

    AbstractJmxAccessFromDeploymentWithRbacTest(boolean securedApplication) {
        this.securedApplication = securedApplication;
    }

    // Subclasses will need to call this method from their @Deployment annotated methods.
    static Archive<?> deploy(boolean securedApplication) {
        WebArchive war = ShrinkWrap.create(WebArchive.class,"jmx-access-rbac.war");
        war.addPackage(JmxResource.class.getPackage());
        war.addAsManifestResource(AbstractJmxAccessFromDeploymentWithRbacTest.class.getResource("jboss-deployment-structure.xml"), "jboss-deployment-structure.xml")
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                        // Required for JMXConnectorFactory.connect()
                        new RuntimePermission("shutdownHooks"),
                        new ElytronPermission("getSecurityDomain"),
                        new MBeanServerPermission("findMBeanServer,createMBeanServer"),
                        ControllerPermission.CAN_ACCESS_MODEL_CONTROLLER,
                        ControllerPermission.CAN_ACCESS_IMMUTABLE_MANAGEMENT_RESOURCE_REGISTRATION,
                        // Required for the service loader to load remoting-jmx
                        new FilePermission("<<ALL FILES>>", "read"),
                        new RemotingPermission("createEndpoint"),
                        new RemotingPermission("connect")
                ), "permissions.xml");
        if (securedApplication) {
            war.addAsWebInfResource(AbstractJmxAccessFromDeploymentWithRbacTest.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
            war.addAsWebInfResource(AbstractJmxAccessFromDeploymentWithRbacTest.class.getPackage(), "web.xml", "web.xml");
        }
        return war;
    }

    private String performCall(String urlPattern) throws Exception {
        try (CloseableHttpClient httpclient = createHttpClient()) {
            HttpGet httpget = new HttpGet(url.toExternalForm() + urlPattern);
            HttpResponse response = httpclient.execute(httpget);
            assertNotNull("Response is 'null', we expected non-null response!", response);
            final String text = Utils.getContent(response);
            assertEquals(text, 200, response.getStatusLine().getStatusCode());
            return text;
        }
    }

    private CloseableHttpClient createHttpClient() {
        HttpClientBuilder builder = HttpClients.custom();
        if (securedApplication) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(url.getHost(), url.getPort()),
                    new UsernamePasswordCredentials("kabir", "kabir"));
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }
        return builder.build();
    }

    @Test
    public void testJmxCallWithPlatformMBeanServer() throws Exception {
        String result = performCall("platform");
        assertEquals("ok", result);
    }

    @Test
    public void testJmxCallWithFoundMBeanServer() throws Exception {
        String result = performCall("found");
        assertEquals("ok", result);
    }

    @Test
    public void testJmxCallWithRemoteMBeanServer() throws Exception {
        String result = performCall("remote");
        assertEquals("ok", result);
    }

    static class EnableRbacSetupTask extends SnapshotRestoreSetupTask {
        private static final String APPLICATION_ROLES_PROPERTIES = "application-roles.properties";
        private static final String APPLICATION_USERS_PROPERTIES = "application-users.properties";
        private static final String MGMT_GROUPS_PROPERTIES = "mgmt-groups.properties";
        private static final String MGMT_USERS_PROPERTIES = "mgmt-users.properties";

        private static final String[] ALL_PROPERTIES = new String[]{
                APPLICATION_ROLES_PROPERTIES,
                APPLICATION_USERS_PROPERTIES,
                MGMT_GROUPS_PROPERTIES,
                MGMT_USERS_PROPERTIES};

        private final boolean useIdentityRoles;
        private boolean securedApplication;
        private final Set<String> skippedFiles = new HashSet<>();

        EnableRbacSetupTask(boolean useIdentityRoles, boolean securedApplication) {
            this.useIdentityRoles = useIdentityRoles;
            this.securedApplication = securedApplication;
            if (!useIdentityRoles) {
                skippedFiles.add(MGMT_GROUPS_PROPERTIES);
            }
        }

        @Override
        protected void doSetup(ManagementClient managementClient, String containerId) throws Exception {
            super.doSetup(managementClient, containerId);
            // Essentially we overwrite all the properties files for the Application- and ManagementRealm/Domains.
            // We restore them in nonManagementCleanUp()
            backupOrRestoreConfigDirPropertyFiles(true);
            copyPropertiesToConfigFolder();


            List<ModelNode> operations = new ArrayList<>();

            // /core-service=management/access=authorization:write-attribute(name=provider,value=rbac)
            PathAddress addr = PathAddress.pathAddress(CORE_SERVICE, MANAGEMENT).append(ACCESS, AUTHORIZATION);
            operations.add(Util.getWriteAttributeOperation(addr, PROVIDER, "rbac"));

            if (useIdentityRoles) {
                // /core-service=management/access=authorization:write-attribute(name=use-identity-roles,value=true)
                addr = PathAddress.pathAddress(CORE_SERVICE, MANAGEMENT).append(ACCESS, AUTHORIZATION);
                operations.add(Util.getWriteAttributeOperation(addr, USE_IDENTITY_ROLES, true));
            } else {
                if (securedApplication) {
                    // /core-service=management/access=authorization/role-mapping=SuperUser/include=user-kabir:add(name=kabir,type=USER)
                    operations.add(createSuperUserRoleMapping("kabir"));
                } else {
                    // /core-service=management/access=authorization/role-mapping=SuperUser/include=user-anonymous:add(name=anonymous,type=USER)
                    operations.add(createSuperUserRoleMapping("anonymous"));
                }
            }

            // /core-service=management/access=identity:add(security-domain=ManagementDomain)
            addr = PathAddress.pathAddress(CORE_SERVICE, MANAGEMENT).append(ACCESS, IDENTITY);
            ModelNode addAccessIdentity = Util.createAddOperation(addr);
            addAccessIdentity.get("security-domain").set("ManagementDomain");
            operations.add(addAccessIdentity);

            // /subsystem=elytron/security-domain=ApplicationDomain:write-attribute(name=outflow-security-domains, value=[ManagementDomain])
            addr = PathAddress.pathAddress(SUBSYSTEM, "elytron").append("security-domain", "ApplicationDomain");
            ModelNode outflowDomains = new ModelNode();
            outflowDomains.add("ManagementDomain");
            operations.add(Util.getWriteAttributeOperation(addr, "outflow-security-domains", outflowDomains));

            // /subsystem=elytron/security-domain=ManagementDomain:write-attribute(name=trusted-security-domains, value=[ApplicationDomain])
            addr = PathAddress.pathAddress(SUBSYSTEM, "elytron").append("security-domain", "ManagementDomain");
            ModelNode trustedDomains = new ModelNode();
            trustedDomains.add("ApplicationDomain");
            operations.add(Util.getWriteAttributeOperation(addr, "trusted-security-domains", trustedDomains));


            ModelNode response = managementClient.getControllerClient().execute(Util.createCompositeOperation(operations));
            Assert.assertEquals(SUCCESS, response.get(OUTCOME).asString());

            ServerReload.executeReloadAndWaitForCompletion(managementClient, TimeoutUtil.adjust(10000));
        }

        private ModelNode createSuperUserRoleMapping(String userName) {
            PathAddress addr = PathAddress.pathAddress(CORE_SERVICE, MANAGEMENT)
                    .append(ACCESS, AUTHORIZATION)
                    .append(ROLE_MAPPING, "SuperUser")
                    .append(INCLUDE, "user-" + userName);
            ModelNode addSuperUserInclude = Util.createAddOperation(addr);
            addSuperUserInclude.get(NAME, userName);
            addSuperUserInclude.get(TYPE, "USER");
            return addSuperUserInclude;
        }

        @Override
        protected void nonManagementCleanUp() throws Exception {
            if (!securedApplication) {
                return;
            }
            backupOrRestoreConfigDirPropertyFiles(false);
            super.nonManagementCleanUp();
        }

        private void backupOrRestoreConfigDirPropertyFiles(boolean setup) throws Exception {
            if (!securedApplication) {
                return;
            }
            Path target = Paths.get("target/wildfly/standalone/configuration");
            Assert.assertTrue(Files.exists(target));
            for (String fileName : ALL_PROPERTIES) {
                if (skippedFiles.contains(fileName)) {
                    continue;
                }

                String backupFileName = fileName + ".bak";

                Path main = target.resolve(fileName);
                Path backup = target.resolve(backupFileName);

                if (setup) {
                    if (Files.exists(backup)) {
                        Files.delete(backup);
                    }
                    Files.move(main, backup);

                } else {
                    if (Files.exists(backup)) {
                        if (Files.exists(main)) {
                            Files.delete(main);
                        }
                        Files.move(backup, main);
                    }
                }
            }
        }

        private void copyPropertiesToConfigFolder() throws Exception {
            if (!securedApplication) {
                return;
            }
            Path target = Paths.get("target/wildfly/standalone/configuration");
            for (String fileName : ALL_PROPERTIES) {
                if (skippedFiles.contains(fileName)) {
                    continue;
                }
                URL url = AbstractJmxAccessFromDeploymentWithRbacTest.class.getResource(fileName);
                Path source = Paths.get(url.toURI());
                Files.copy(source, target.resolve(fileName));
            }
        }
    }
}
