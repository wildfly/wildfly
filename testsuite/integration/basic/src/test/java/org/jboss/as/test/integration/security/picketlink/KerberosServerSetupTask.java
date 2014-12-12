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

package org.jboss.as.test.integration.security.picketlink;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.apache.directory.server.annotations.CreateKdcServer;
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
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.security.common.AbstractKrb5ConfServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.as.test.integration.security.common.KDCServerAnnotationProcessor;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.ManagedCreateTransport;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;
import org.jboss.logging.Logger;
import org.jboss.security.SecurityConstants;

/**
 * A server setup task which configures and starts two LDAP servers with Kerberos capabilities.
 * <p>
 * Based on {@link org.jboss.as.test.integration.security.loginmodules.LdapExtLDAPServerSetupTask}
 */
public class KerberosServerSetupTask implements ServerSetupTask {

    private static final Logger LOGGER = Logger.getLogger(KerberosServerSetupTask.class);

    public static final String SECURITY_CREDENTIALS = "secret";
    public static final String SECURITY_PRINCIPAL = "uid=admin,ou=system";

    private static final String KEYSTORE_FILENAME = "ldaps.jks";
    private static final File KEYSTORE_FILE = new File(KEYSTORE_FILENAME);

    public static final int LDAP_PORT = 10389;
    public static final int LDAPS_PORT = 10636;

    public static final int KERBEROS_PORT = 6088;

    public static final String KERBEROS_PRIMARY_REALM = "JBOSS.ORG";

    public static final String[] ROLE_NAMES = { "TheDuke", "Echo", "Admin", "SharedRoles", "RX" };

    public static final String QUERY_ROLES;
    static {
        final List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        for (final String role : ROLE_NAMES) {
            qparams.add(new BasicNameValuePair(RolePrintingServlet.PARAM_ROLE_NAME, role));
        }
        QUERY_ROLES = URLEncodedUtils.format(qparams, "UTF-8");
    }

    private boolean removeBouncyCastle = false;

    private DirectoryService directoryService1;
    private LdapServer ldapServer1;
    private KdcServer krbServer1;

    /**
     * Creates directory services, starts LDAP server and KDCServer
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

        final String hostname = Utils.getHost(managementClient);
        createLdap1(managementClient, hostname);
    }

    //@formatter:off
    @CreateDS(
        name = "JBossDS",
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
        },
        additionalInterceptors = { KeyDerivationInterceptor.class })
    @CreateLdapServer (
        transports =
        {
            @CreateTransport( protocol = "LDAP",  port = LDAP_PORT),
            @CreateTransport( protocol = "LDAPS", port = LDAPS_PORT)
        },
        certificatePassword="secret")
    @CreateKdcServer(
      primaryRealm = KERBEROS_PRIMARY_REALM,
      kdcPrincipal = "krbtgt/" + KERBEROS_PRIMARY_REALM + "@" + KERBEROS_PRIMARY_REALM,
      searchBaseDn = "dc=jboss,dc=org",
      transports = {
        @CreateTransport( protocol = "UDP", port = KERBEROS_PORT ),
        @CreateTransport( protocol = "TCP", port = KERBEROS_PORT )
    })
    //@formatter:on
    public void createLdap1(ManagementClient managementClient, final String hostname) throws Exception, IOException,
            ClassNotFoundException, FileNotFoundException {
        final Map<String, String> map = new HashMap<String, String>();
        final String cannonicalHost = NetworkUtils.formatPossibleIpv6Address(Utils.getCannonicalHost(managementClient));
        map.put("hostname", cannonicalHost);
        map.put("realm", KERBEROS_PRIMARY_REALM);
        directoryService1 = DSAnnotationProcessor.getDirectoryService();
        final String ldifContent = StrSubstitutor.replace(
                IOUtils.toString(
                        KerberosServerSetupTask.class.getResourceAsStream(KerberosServerSetupTask.class.getSimpleName()
                                + ".ldif"), "UTF-8"), map);
        LOGGER.info(ldifContent);

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
        fixTransportAddress(createLdapServer, cannonicalHost);

        ldapServer1 = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService1);
        krbServer1 = KDCServerAnnotationProcessor.getKdcServer(directoryService1, KERBEROS_PORT, cannonicalHost);
        ldapServer1.start();
    }

    public static String getHttpServicePrincipal(ManagementClient managementClient) {
        return "HTTP/" + NetworkUtils.formatPossibleIpv6Address(Utils.getCannonicalHost(managementClient)) + "@"
                + KerberosServerSetupTask.KERBEROS_PRIMARY_REALM;
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
     *      java.lang.String)
     */
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        krbServer1.stop();
        ldapServer1.stop();
        directoryService1.shutdown();

        KEYSTORE_FILE.delete();

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
    public static class SystemPropertiesSetup extends AbstractSystemPropertiesServerSetupTask {

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask#getSystemProperties()
         */
        @Override
        protected SystemProperty[] getSystemProperties() {
            final Map<String, String> map = new HashMap<String, String>();
            map.put("javax.net.ssl.trustStore", KEYSTORE_FILE.getAbsolutePath());
            map.put("java.security.krb5.conf", Krb5ConfServerSetupTask.getKrb5ConfFullPath());
            map.put("sun.security.krb5.debug", "true");
            map.put(SecurityConstants.DISABLE_SECDOMAIN_OPTION, "true");
            return mapToSystemProperties(map);
        }
    }

    /**
     * Task which generates krb5.conf and keytab file(s).
     */
    public static class Krb5ConfServerSetupTask extends AbstractKrb5ConfServerSetupTask {
        @Override
        protected List<UserForKeyTab> kerberosUsers() {
            return null;
        }
    }
}
