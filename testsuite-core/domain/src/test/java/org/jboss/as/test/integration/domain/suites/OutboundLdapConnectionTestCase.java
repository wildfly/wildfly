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
package org.jboss.as.test.integration.domain.suites;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeoutException;
import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Basic tests of outbound LDAP connection in a domain.
 *
 * @author Ondrej Kotek <okotek@redhat.com>
 */
public class OutboundLdapConnectionTestCase {

    private static final Logger LOGGER = Logger.getLogger(OutboundLdapConnectionTestCase.class);

    private static final String USER_NAME = "jduke";
    private static final String USER_PASSWORD = "theduke";

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static URL managementInterfaceUrl;

    private static DirectoryService directoryService;
    private static LdapServer ldapServer;

    private DomainClient masterClient;


    @Test
    public void testSetupAndLoginToHttpManagementInterfaceWithOutboundLdapConnection() throws Exception {
        addLdapOutboundConnection();
        addTestRealm();
        addTestRealmLdapAuthentication();
        changeHttpInterfaceSecurityRealm();
        reload();

        testLoginToHttpManagementInterface(true);

        testLoginFailWithBadOutboundLdapConnection();
    }

    private void testLoginFailWithBadOutboundLdapConnection() throws Exception {
        changeLdapConnectionSearchDnAttribute("uid=admin,ou=system2XX");
        testLoginToHttpManagementInterface(false);

        changeLdapConnectionSearchDnAttribute("uid=admin,ou=system");
        testLoginToHttpManagementInterface(true);
    }

    private void testLoginToHttpManagementInterface(boolean expectToPass) throws IOException, URISyntaxException {
        final int expectedStatus = expectToPass ? 200 : 401;

        final String response =
                CoreUtils.makeCallWithBasicAuthn(managementInterfaceUrl, USER_NAME, USER_PASSWORD, expectedStatus);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(response);
        }
    }

    private void addLdapOutboundConnection() throws IOException, MgmtOperationException {
        final ModelNode addLdapOutboundConnection = ModelUtil.createOpNode(
                "host=master/core-service=management/ldap-connection=ldapConnection", ADD);
        addLdapOutboundConnection.get("url").set("ldap://localhost:10389");
        addLdapOutboundConnection.get("search-dn").set("uid=admin,ou=system");
        addLdapOutboundConnection.get("search-credential").set("secret");
        executeOperation(addLdapOutboundConnection);
    }

    private void addTestRealm() throws IOException, MgmtOperationException {
        final ModelNode addTestRealm = ModelUtil.createOpNode(
                "host=master/core-service=management/security-realm=TestRealm", ADD);
        executeOperation(addTestRealm);
    }

    private void addTestRealmLdapAuthentication() throws IOException, MgmtOperationException {
        final ModelNode addTestRealmLdapAuthentication = ModelUtil.createOpNode(
                "host=master/core-service=management/security-realm=TestRealm/authentication=ldap", ADD);
        addTestRealmLdapAuthentication.get("connection").set("ldapConnection");
        addTestRealmLdapAuthentication.get("base-dn").set("ou=People,dc=wildfly,dc=org");
        addTestRealmLdapAuthentication.get("username-attribute").set("uid");
        executeOperation(addTestRealmLdapAuthentication);
    }

    private void changeHttpInterfaceSecurityRealm() throws IOException, MgmtOperationException {
        final ModelNode changeHttpInterface = ModelUtil.createOpNode(
                "host=master/core-service=management/management-interface=http-interface", WRITE_ATTRIBUTE_OPERATION);
        changeHttpInterface.get("name").set("security-realm");
        changeHttpInterface.get("value").set("TestRealm");
        executeOperation(changeHttpInterface);
    }

    private void changeLdapConnectionSearchDnAttribute(String value) throws IOException, MgmtOperationException {
        final ModelNode changeLdapSearchDn = ModelUtil.createOpNode(
                "host=master/core-service=management/ldap-connection=ldapConnection", WRITE_ATTRIBUTE_OPERATION);
        changeLdapSearchDn.get("name").set("search-dn");
        changeLdapSearchDn.get("value").set(value);
        executeOperation(changeLdapSearchDn);
    }

    private ModelNode executeOperation(ModelNode operation) throws IOException, MgmtOperationException {
        final ModelNode ret = this.masterClient.execute(operation);

        if (!SUCCESS.equals(ret.get(OUTCOME).asString())) {
            throw new MgmtOperationException("Management operation failed: " + ret.get(FAILURE_DESCRIPTION), operation, ret);
        }
        return ret.get(RESULT);
    }

    private void reload() throws IOException, TimeoutException, InterruptedException {
        ModelNode reload = new ModelNode();
        reload.get(OP_ADDR).add(HOST, "master");
        reload.get(OP).set("reload");
        reload.get("admin-only").set(false);
        domainMasterLifecycleUtil.executeAwaitConnectionClosed(reload);

        domainMasterLifecycleUtil.connect();
        domainMasterLifecycleUtil.awaitHostController(System.currentTimeMillis());
    }


    @BeforeClass
    @CreateDS(
        name = "WildFlyDS",
        factory = InMemoryDirectoryServiceFactory.class,
        partitions = @CreatePartition(name = "wildfly", suffix = "dc=wildfly,dc=org"),
        allowAnonAccess = true
    )
    @CreateLdapServer(
        transports = @CreateTransport(protocol = "LDAP", address = "localhost", port = 10389),
        allowAnonymousAccess = true
    )
    public static void setUpLdap() throws Exception {
        directoryService = DSAnnotationProcessor.getDirectoryService();
        final SchemaManager schemaManager = directoryService.getSchemaManager();
        final InputStream ldif = OutboundLdapConnectionTestCase.class
                .getResourceAsStream("/" + OutboundLdapConnectionTestCase.class.getSimpleName() + ".ldif");
        for (LdifEntry ldifEntry : new LdifReader(ldif)) {
            directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
        }
        ldapServer = ServerAnnotationProcessor.getLdapServer(directoryService);
    }

    @AfterClass
    public static void tearDownLdap() throws Exception {
        ldapServer.stop();
        directoryService.shutdown();
        FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
    }


    @BeforeClass
    public static void setupDomain() throws Exception {
        final DomainTestSupport.Configuration config =
                DomainTestSupport.Configuration.create(OutboundLdapConnectionTestCase.class.getSimpleName(),
                "domain-configs/domain-standard.xml", "host-configs/host-outbound-ldap-connection.xml", null);

        testSupport = DomainTestSupport.create(config);
        testSupport.start();
        domainMasterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
        managementInterfaceUrl = new URL("http://" + TestSuiteEnvironment.formatPossibleIpv6Address(DomainTestSupport.masterAddress) + ":9990/management");
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        testSupport.stop();
        domainMasterLifecycleUtil = null;
        testSupport = null;
    }


    @Before
    public void setup() throws Exception {
        this.masterClient = domainMasterLifecycleUtil.getDomainClient();
    }
}
