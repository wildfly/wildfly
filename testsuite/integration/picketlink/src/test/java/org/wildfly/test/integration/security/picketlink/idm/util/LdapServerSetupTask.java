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

package org.wildfly.test.integration.security.picketlink.idm.util;

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
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.ManagedCreateTransport;
import org.jboss.as.test.integration.security.common.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>{@link org.jboss.as.arquillian.api.ServerSetupTask} that starts/stops a ApacheDS embedded server.</p>
 *
 * @author Pedro Igor
 */
public class LdapServerSetupTask implements ServerSetupTask {

    static final String KEYSTORE_FILENAME = "ldaps.jks";
    static final File KEYSTORE_FILE = new File(KEYSTORE_FILENAME);
    static final int LDAP_PORT = 10389;
    static final int LDAPS_PORT = 10636;

    private DirectoryService directoryService;
    private LdapServer ldapServer;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        startLdapServer(Utils.getSecondaryTestAddress(managementClient, false));
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        stopLdapServer(managementClient, containerId);
    }

    @CreateDS(
             name = "JBossDS-LdapServerSetupTask",
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
    @CreateLdapServer(
                     transports =
                     {
                     @CreateTransport( protocol = "LDAP",  port = LDAP_PORT),
                     @CreateTransport( protocol = "LDAPS", port = LDAPS_PORT)
                     },
                     certificatePassword="secret")
    //@formatter:on
    public void startLdapServer(final String hostname) throws Exception, IOException, ClassNotFoundException, FileNotFoundException {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("hostname", NetworkUtils.formatPossibleIpv6Address(hostname));
        directoryService = DSAnnotationProcessor.getDirectoryService();
        final String ldifContent = StrSubstitutor.replace(IOUtils.toString(LdapServerSetupTask.class.getResourceAsStream("picketlink-idm-tests.ldif"), "UTF-8"), map);

        final SchemaManager schemaManager = directoryService.getSchemaManager();
        try {
            for (LdifEntry ldifEntry : new LdifReader(IOUtils.toInputStream(ldifContent))) {
                directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
            }
        } catch (Exception e) {
            throw e;
        }
        final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer((CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
        FileOutputStream fos = new FileOutputStream(KEYSTORE_FILE);
        IOUtils.copy(getClass().getResourceAsStream(KEYSTORE_FILENAME), fos);
        fos.close();
        createLdapServer.setKeyStore(KEYSTORE_FILE.getAbsolutePath());
        fixTransportAddress(createLdapServer, hostname);
        ldapServer = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService);
        ldapServer.start();
    }

    /**
     * Stops LDAP server and KDCServer and shuts down the directory service.
     *
     * @param managementClient
     * @param containerId
     * @throws Exception
     * @see org.jboss.as.arquillian.api.ServerSetupTask#tearDown(org.jboss.as.arquillian.container.ManagementClient,
     *      String)
     */
    public void stopLdapServer(ManagementClient managementClient, String containerId) throws Exception {
        ldapServer.stop();
        directoryService.shutdown();
        KEYSTORE_FILE.delete();
        FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
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

}
