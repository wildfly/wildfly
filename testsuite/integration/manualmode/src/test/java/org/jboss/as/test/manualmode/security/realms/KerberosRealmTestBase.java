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
package org.jboss.as.test.manualmode.security.realms;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.HashMap;
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
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.security.common.KDCServerAnnotationProcessor;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.ManagedCreateTransport;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.logging.Logger;

/**
 * Base class for Kerberos authentication in Security Realm.
 *
 * @author olukas
 */
public abstract class KerberosRealmTestBase extends AbstractCliTestBase {
    
    private static final Logger LOGGER = Logger.getLogger(KerberosRealmTestBase.class);
    
    private static final int KERBEROS_PORT1 = 6088;
    private static final int KERBEROS_PORT2 = 6188;
    private static final int LDAP_PORT1 = 10389;
    private static final int LDAP_PORT2 = 10489;
    
    /**
     * Escapes backslashes in the file path.
     * @param path
     * @return
     */
    protected static String escapePath(String path) {
        return path.replace("\\", "\\\\");
    }
    
    /**
     * Runs a CLI script in as a batch.
     *
     * @param batchFile CLI file
     * @return true if CLI returns Success
     * @throws IOException
     */
    protected static boolean runBatch(File batchFile) throws IOException {
        cli.sendLine("run-batch --file=\"" + batchFile.getAbsolutePath()
                + "\" --headers={allow-resource-service-restart=true} -v", false);
        return cli.readAllAsOpResult().isIsOutcomeSuccess();
    }
    
    abstract static class AbstractKerberosServerConfig {

        private DirectoryService directoryService;
        private KdcServer kdcServer;
        private LdapServer ldapServer;

        private boolean removeBouncyCastle = false;

        public abstract void setup(ManagementClient managementClient, InputStream ldifInputStream) throws Exception;

        /**
         * Creates directory services, starts LDAP server and KDCServer
         *
         * @param managementClient
         * @throws Exception
         */
        protected final void setupInternal(ManagementClient managementClient, InputStream ldifInputStream) throws Exception {
            if (directoryService != null) {
                throw new IllegalStateException("DS service is already instantiated.");
            }
            try {
                if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                    removeBouncyCastle = true;
                }
            } catch (SecurityException ex) {
                LOGGER.warn("Cannot register BouncyCastleProvider", ex);
            }
            directoryService = DSAnnotationProcessor.getDirectoryService();
            final String hostname = Utils.getCanonicalHost(managementClient);
            final Map<String, String> map = new HashMap<String, String>();
            map.put("hostname", NetworkUtils.formatPossibleIpv6Address(hostname));
            final String secondaryTestAddress = Utils.getSecondaryTestAddress(managementClient, true);
            if (ldifInputStream != null) {
                final String ldifContent = StrSubstitutor.replace(IOUtils.toString(ldifInputStream, "UTF-8"), map);
            LOGGER.info(ldifContent);

            // InputStream resourceAsStream = KerberosRealmTestBase.class.getResourceAsStream(getLdifFileSimpleName());

            final SchemaManager schemaManager = directoryService.getSchemaManager();
            try {
                for (LdifEntry ldifEntry : new LdifReader(IOUtils.toInputStream(ldifContent))) {
                    directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            }
            kdcServer = KDCServerAnnotationProcessor.getKdcServer(directoryService, 10000, hostname);

            final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                    (CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
            fixTransportAddress(createLdapServer, secondaryTestAddress);

            ldapServer = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService);
            ldapServer.start();
        }

        /**
         * Fixes bind address in the CreateTransport annotation.
         *
         * @param createLdapServer
         */
        private static void fixTransportAddress(ManagedCreateLdapServer createLdapServer, String address) {
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
         * @throws Exception
         */
        public final void tearDown() throws Exception {
            if (ldapServer == null || kdcServer == null || directoryService == null) {
                throw new IllegalStateException("LDAP/KDC/DS services are not running.");
            }
            ldapServer.stop();
            ldapServer = null;
            kdcServer.stop();
            kdcServer = null;
            directoryService.shutdown();
            FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
            directoryService = null;
            if (removeBouncyCastle) {
                try {
                    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
                } catch (SecurityException ex) {
                    LOGGER.warn("Cannot deregister BouncyCastleProvider", ex);
                }
            }
        }

        /**
         * Returns true if the directory service is started.
         * 
         * @return
         */
        public boolean isDirectoryServiceRunning() {
            return directoryService != null && directoryService.isStarted();
        }
    }

    public static class JBossOrgKerberosServerConfig extends AbstractKerberosServerConfig {

        //@formatter:off
        @Override
        @CreateDS(
            name = "JBossDS",
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
                    @CreateTransport( protocol = "LDAP",  port = LDAP_PORT1)
                })
        @CreateKdcServer(primaryRealm = "JBOSS.ORG",
            kdcPrincipal = "krbtgt/JBOSS.ORG@JBOSS.ORG",
            searchBaseDn = "dc=jboss,dc=org",
            transports =
            {
                @CreateTransport(protocol = "UDP", port = KERBEROS_PORT1)
            })
        //@formatter:on
        public final void setup(ManagementClient managementClient, InputStream ldifInputStream) throws Exception {
            setupInternal(managementClient, ldifInputStream);
        }
    }

    public static class JBossComKerberosServerConfig extends AbstractKerberosServerConfig {

        //@formatter:off
        @Override
        @CreateDS(
            name = "JBossDS2",
            partitions =
            {
                @CreatePartition(
                    name = "jboss2",
                    suffix = "dc=jboss,dc=com",
                    contextEntry = @ContextEntry(
                        entryLdif =
                            "dn: dc=jboss,dc=com\n" +
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
                    @CreateTransport( protocol = "LDAP",  port = LDAP_PORT2)
                })
        @CreateKdcServer(primaryRealm = "JBOSS.COM",
            kdcPrincipal = "krbtgt/JBOSS.COM@JBOSS.COM",
            searchBaseDn = "dc=jboss,dc=com",
            transports =
            {
                @CreateTransport(protocol = "UDP", port = KERBEROS_PORT2)
            })
        //@formatter:on
        public final void setup(ManagementClient managementClient, InputStream ldifInputStream) throws Exception {
            setupInternal(managementClient, ldifInputStream);
        }
    }
}
