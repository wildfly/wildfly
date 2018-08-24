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

package org.jboss.as.test.integration.security.loginmodules;

import java.io.File;
import java.io.FileOutputStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.AnnotationUtils;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.ManagedCreateTransport;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;
import org.jboss.logging.Logger;

/**
 * A server setup task which configures and starts 2 LDAP servers for {@link LdapExtLoginModuleTestCase} and
 * {@link LdapExtLikeAdvancedLdapLMTestCase}.
 */
public class LdapExtLDAPServerSetupTask implements ServerSetupTask {

    private static Logger LOGGER = Logger.getLogger(LdapExtLDAPServerSetupTask.class);

    static final String SECURITY_CREDENTIALS = "secret";
    static final String SECURITY_PRINCIPAL = "uid=admin,ou=system";

    static final String KEYSTORE_FILENAME = "ldaps.jks";
    static final File KEYSTORE_FILE = new File(KEYSTORE_FILENAME);
    static final int LDAP_PORT = 10389;
    static final int LDAP_PORT2 = 11389;
    static final int LDAPS_PORT = 10636;

    static final String[] ROLE_NAMES = {"TheDuke", "Echo", "TheDuke2", "Echo2", "JBossAdmin", "jduke", "jduke2", "RG1", "RG2",
            "RG3", "R1", "R2", "R3", "R4", "R5", "Roles", "User", "Admin", "SharedRoles", "RX"};

    static final String QUERY_ROLES;

    static {
        final List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        for (final String role : ROLE_NAMES) {
            qparams.add(new BasicNameValuePair(RolePrintingServlet.PARAM_ROLE_NAME, role));
        }
        QUERY_ROLES = URLEncodedUtils.format(qparams, "UTF-8");
    }

    private DirectoryService directoryService1;
    private DirectoryService directoryService2;
    private LdapServer ldapServer1;
    private LdapServer ldapServer2;

    private boolean removeBouncyCastle = false;

    /**
     * Creates directory services, starts LDAP server and KDCServer
     *
     * @param managementClient
     * @param containerId
     * @throws Exception
     * @see org.jboss.as.arquillian.api.ServerSetupTask#setup(org.jboss.as.arquillian.container.ManagementClient,
     * java.lang.String)
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

        final String hostname = Utils.getSecondaryTestAddress(managementClient, false);
        createLdap1(hostname);
        createLdap2(hostname);
    }

    //@formatter:off
    @CreateDS(
            name = "JBossDS-LdapExtLDAPServerSetupTask",
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
                                                            "objectClass: domain\n\n"),
                                    indexes =
                                            {
                                                    @CreateIndex(attribute = "objectClass"),
                                                    @CreateIndex(attribute = "dc"),
                                                    @CreateIndex(attribute = "ou")
                                            })
                    },
            additionalInterceptors = {KeyDerivationInterceptor.class})
    @CreateLdapServer(
            transports =
                    {
                            @CreateTransport(protocol = "LDAP", port = LDAP_PORT),
                            @CreateTransport(protocol = "LDAPS", port = LDAPS_PORT)
                    },
            certificatePassword = "secret")
    //@formatter:on
    public void createLdap1(final String hostname) throws Exception {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("hostname", NetworkUtils.formatPossibleIpv6Address(hostname));
        map.put("ldapPort2", Integer.toString(LDAP_PORT2));
        directoryService1 = DSAnnotationProcessor.getDirectoryService();
        final String ldifContent = StrSubstitutor.replace(
                IOUtils.toString(
                        LdapExtLoginModuleTestCase.class.getResourceAsStream(LdapExtLoginModuleTestCase.class.getSimpleName()
                                + ".ldif"), "UTF-8"), map);
        LOGGER.debug(ldifContent);

        final SchemaManager schemaManager = directoryService1.getSchemaManager();
        try {
            for (LdifEntry ldifEntry : new LdifReader(IOUtils.toInputStream(ldifContent))) {
                directoryService1.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                (CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
        FileOutputStream fos = new FileOutputStream(KEYSTORE_FILE);
        IOUtils.copy(getClass().getResourceAsStream(KEYSTORE_FILENAME), fos);
        fos.close();
        createLdapServer.setKeyStore(KEYSTORE_FILE.getAbsolutePath());
        fixTransportAddress(createLdapServer, hostname);
        ldapServer1 = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService1);
        ldapServer1.start();
        LOGGER.trace("ldapServer1 = " + ldapServer1);
    }

    //@formatter:off
    @CreateDS(
            name = "JBossComDS",
            factory = org.jboss.as.test.integration.ldap.InMemoryDirectoryServiceFactory.class,
            partitions =
                    {
                            @CreatePartition(
                                    name = "jbossCom",
                                    suffix = "dc=jboss,dc=com",
                                    contextEntry = @ContextEntry(
                                            entryLdif =
                                                    "dn: dc=jboss,dc=com\n" +
                                                            "dc: jboss\n" +
                                                            "objectClass: top\n" +
                                                            "objectClass: domain\n\n"
                                    ),
                                    indexes =
                                            {
                                                    @CreateIndex(attribute = "objectClass"),
                                                    @CreateIndex(attribute = "dc"),
                                                    @CreateIndex(attribute = "ou")
                                            }),
                            @CreatePartition(
                                    name = "jbossOrg",
                                    suffix = "dc=jboss,dc=org",
                                    contextEntry = @ContextEntry(
                                            entryLdif =
                                                    "dn: dc=jboss,dc=org\n" +
                                                            "dc: jboss\n" +
                                                            "objectClass: top\n" +
                                                            "objectClass: domain\n\n"
                                    ),
                                    indexes =
                                            {
                                                    @CreateIndex(attribute = "objectClass"),
                                                    @CreateIndex(attribute = "dc"),
                                                    @CreateIndex(attribute = "ou")
                                            })
                    },
            additionalInterceptors = {KeyDerivationInterceptor.class})
    @CreateLdapServer(
            transports =
                    {
                            @CreateTransport(protocol = "LDAP", port = LDAP_PORT2)
                    })
    //@formatter:on
    public void createLdap2(final String hostname) throws Exception {
        directoryService2 = DSAnnotationProcessor.getDirectoryService();
        final SchemaManager schemaManager = directoryService2.getSchemaManager();
        try {
            for (LdifEntry ldifEntry : new LdifReader(
                    LdapExtLoginModuleTestCase.class.getResourceAsStream(LdapExtLoginModuleTestCase.class.getSimpleName()
                            + "2.ldif"))) {
                directoryService2.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                (CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
        fixTransportAddress(createLdapServer, hostname);
        ldapServer2 = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService2);
        ldapServer2.start();
        LOGGER.trace("ldapServer2 = " + ldapServer2);
    }

    /**
     * Fixes bind address in the CreateTransport annotation.
     *
     * @param createLdapServer
     */
    private void fixTransportAddress(ManagedCreateLdapServer createLdapServer, String address) {
        final CreateTransport[] createTransports = createLdapServer.transports();
        for (int i = 0; i < createTransports.length; i++) {
            final ManagedCreateTransport mgCreateTransport = new ManagedCreateTransport(createTransports[i]);
            mgCreateTransport.setAddress(address);
            createTransports[i] = mgCreateTransport;
        }
    }

    /**
     * Stops LDAP server and KDCServer and shuts down the directory service.
     *
     * @param managementClient
     * @param containerId
     * @throws Exception
     * @see org.jboss.as.arquillian.api.ServerSetupTask#tearDown(org.jboss.as.arquillian.container.ManagementClient,
     * java.lang.String)
     */
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        ldapServer2.stop();
        directoryService2.shutdown();
        ldapServer1.stop();
        directoryService1.shutdown();
        KEYSTORE_FILE.delete();
        FileUtils.deleteDirectory(directoryService2.getInstanceLayout().getInstanceDirectory());
        FileUtils.deleteDirectory(directoryService1.getInstanceLayout().getInstanceDirectory());
        if (removeBouncyCastle) {
            try {
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
            } catch (SecurityException ex) {
                LOGGER.warn("Cannot deregister BouncyCastleProvider", ex);
            }
        }
    }

    /**
     * This setup task sets truststore file.
     */
    static class SystemPropertiesSetup extends AbstractSystemPropertiesServerSetupTask {

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask#getSystemProperties()
         */
        @Override
        protected SystemProperty[] getSystemProperties() {
            return new SystemProperty[]{new DefaultSystemProperty("javax.net.ssl.trustStore", KEYSTORE_FILE.getAbsolutePath()),
                    new DefaultSystemProperty("com.sun.jndi.ldap.object.disableEndpointIdentification","")};
        }
    }

}
