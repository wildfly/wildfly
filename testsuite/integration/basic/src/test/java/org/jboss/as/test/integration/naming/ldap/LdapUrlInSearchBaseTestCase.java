/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.naming.ldap;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import java.net.SocketPermission;
import java.net.URL;
import java.net.URLEncoder;
import java.security.Security;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.AnnotationUtils;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Regression test for LDAP related issues.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({ LdapUrlInSearchBaseTestCase.LDAPServerSetupTask.class })
@RunAsClient
public class LdapUrlInSearchBaseTestCase {

    private static Logger LOGGER = Logger.getLogger(LdapUrlInSearchBaseTestCase.class);

    @ArquillianResource
    ManagementClient mgmtClient;

    @ArquillianResource
    URL webAppURL;

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link WebArchive} with the {@link LdapUrlTestServlet}.
     *
     * @return
     */
    @Deployment
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ldap-test.war");
        war.addClasses(LdapUrlTestServlet.class);

        war.addAsManifestResource(createPermissionsXmlAsset(
                new SocketPermission("*:10389", "connect,resolve"),
                new RuntimePermission("accessClassInPackage.com.sun.jndi.ldap"),
                new RuntimePermission("accessClassInPackage.com.sun.jndi.url.ldap")
        ), "permissions.xml");

        return war;
    }

    /**
     * Tests if it's possible to have searchBase prefixed with LDAP URL, InitialDirContext is used.
     *
     * @throws Exception
     */
    @Test
    public void testDir() throws Exception {
        final URL servletURL = new URL(webAppURL.toExternalForm() + "?" + LdapUrlTestServlet.PARAM_HOST + "="
                + URLEncoder.encode(Utils.getSecondaryTestAddress(mgmtClient), "UTF-8"));
        assertEquals("cn=Java Duke", Utils.makeCallWithBasicAuthn(servletURL, null, null, 200));
    }

    /**
     * Tests if it's possible to have searchBase prefixed with LDAP URL, InitialLdapContext is used.
     *
     * @throws Exception
     */
    @Test
    public void testLdap() throws Exception {
        final URL servletURL = new URL(webAppURL.toExternalForm() + "?" + LdapUrlTestServlet.PARAM_HOST + "="
                + URLEncoder.encode(Utils.getSecondaryTestAddress(mgmtClient), "UTF-8") + "&" + LdapUrlTestServlet.PARAM_LDAP
                + "=");
        assertEquals("cn=Java Duke", Utils.makeCallWithBasicAuthn(servletURL, null, null, 200));
    }

    /**
     * To test the behavior outside of the AS (i.e. org.jboss.as.naming.InitialContext is not used).
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        LDAPServerSetupTask ldapSetup = new LDAPServerSetupTask();
        ldapSetup.setup(null, null);
        ldapSetup.tearDown(null, null);
    }

    // Embedded classes ------------------------------------------------------

    /**
     * A server setup task which configures and starts LDAP server.
     */
    //@formatter:off
    @CreateDS(
        name = "JBossDS-LdapUrlInSearchBaseTestCase",
        factory = org.jboss.as.test.integration.ldap.InMemoryDirectoryServiceFactory.class,
        partitions =
        {
            @CreatePartition(
                name = "jboss",
                suffix = "dc=jboss,dc=org",
                contextEntry = @ContextEntry(
                    entryLdif =
                        "dn: dc=jboss,dc=org\n" +
                        "dc: jboss\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n" ),
                indexes =
                {
                    @CreateIndex( attribute = "objectClass" ),
                    @CreateIndex( attribute = "dc" ),
                    @CreateIndex( attribute = "ou" )
                })
        })
    @CreateLdapServer (
        transports =
        {
            @CreateTransport( protocol = "LDAP",  port = 10389, address = "0.0.0.0" ),
        })
    //@formatter:on
    static class LDAPServerSetupTask implements ServerSetupTask {

        private DirectoryService directoryService;
        private LdapServer ldapServer;
        private boolean removeBouncyCastle = false;

        /**
         * Creates directory services and starts LDAP server
         *
         * @param managementClient
         * @param containerId
         * @throws Exception
         * @see org.jboss.as.arquillian.api.ServerSetupTask#setup(org.jboss.as.arquillian.container.ManagementClient,
         *      java.lang.String)
         */
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            try {
                if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                    removeBouncyCastle = true;
                }
            } catch (SecurityException ex) {
                LOGGER.warn("Cannot register BouncyCastleProvider", ex);
            }
            directoryService = DSAnnotationProcessor.getDirectoryService();
            DSAnnotationProcessor.injectEntries(directoryService, "dn: uid=jduke,dc=jboss,dc=org\n" //
                    + "objectclass: top\n" //
                    + "objectclass: uidObject\n" //
                    + "objectclass: person\n" //
                    + "uid: jduke\n" //
                    + "cn: Java Duke\n" //
                    + "sn: Duke\n" //
                    + "userPassword: theduke\n");
            final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                    (CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
            Utils.fixApacheDSTransportAddress(createLdapServer, Utils.getSecondaryTestAddress(managementClient, false));
            ldapServer = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService);
            ldapServer.start();
        }

        /**
         * Stops LDAP server and shuts down the directory service.
         *
         * @param managementClient
         * @param containerId
         * @throws Exception
         * @see org.jboss.as.arquillian.api.ServerSetupTask#tearDown(org.jboss.as.arquillian.container.ManagementClient,
         *      java.lang.String)
         */
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ldapServer.stop();
            directoryService.shutdown();
            FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
            if (removeBouncyCastle) {
                try {
                    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
                } catch (SecurityException ex) {
                    LOGGER.warn("Cannot deregister BouncyCastleProvider", ex);
                }
            }
        }
    }

}
