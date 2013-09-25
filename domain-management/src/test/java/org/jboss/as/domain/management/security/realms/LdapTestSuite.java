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

package org.jboss.as.domain.management.security.realms;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Suite for LDAP related tests.
 *
 * By using a suite the LDAP server can be started and initialised once and then
 * used for all test cases executed within the suite.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    LdapAuthenticationSuiteTest.class,
    GroupToPrincipalLdapSuiteTest.class,
    PrincipalToGroupLdapSuiteTest.class
})
public class LdapTestSuite {

    static final String HOST_NAME = "localhost";
    static int LDAP_PORT = 11389;

    private static final String DIRECTORY_NAME = "Test Service";
    private static boolean initialised;
    private static File workingDir;
    private static DirectoryService directoryService;
    private static LdapServer ldapServer;

    @BeforeClass
    public static void startLdapServerForSuite() throws Exception {
        startLdapServer();
    }

    public static boolean startLdapServer() throws Exception {
        if (initialised == true) {
            return false;
        }

        createWorkingDir();
        DirectoryServiceFactory dsf = new DefaultDirectoryServiceFactory();
        dsf.init(DIRECTORY_NAME);
        directoryService = dsf.getDirectoryService();
        directoryService.getChangeLog().setEnabled(false);
        SchemaManager schemaManager = directoryService.getSchemaManager();

        createPartition(dsf, schemaManager, "simple", "dc=simple,dc=wildfly,dc=org");
        createPartition(dsf, schemaManager, "group-to-principal", "dc=group-to-principal,dc=wildfly,dc=org");
        createPartition(dsf, schemaManager, "principal-to-group", "dc=principal-to-group,dc=wildfly,dc=org");

        CoreSession adminSession = directoryService.getAdminSession();
        processLdif(schemaManager, adminSession, "memberOf-schema.ldif");
        processLdif(schemaManager, adminSession, "simple-partition.ldif");
        processLdif(schemaManager, adminSession, "group-to-principal.ldif");
        processLdif(schemaManager, adminSession, "principal-to-group.ldif");

        ldapServer = new LdapServer();
        ldapServer.setServiceName("DefaultLDAP");
        Transport ldap = new TcpTransport( "0.0.0.0", LDAP_PORT, 3, 5 );
        ldapServer.addTransports(ldap);
        ldapServer.setDirectoryService(directoryService);
        ldapServer.start();

        initialised=true;
        return true;
    }

    private static void createPartition(final DirectoryServiceFactory dsf, final SchemaManager schemaManager, final String id,
            final String suffix) throws Exception {
        PartitionFactory pf = dsf.getPartitionFactory();
        Partition p = pf.createPartition(schemaManager, id, suffix, 1000, workingDir);
        pf.addIndex(p, "uid", 10);
        pf.addIndex(p, "departmentNumber", 10);
        pf.addIndex(p, "member", 10);
        pf.addIndex(p, "memberOf", 10);
        p.initialize();
        directoryService.addPartition(p);
    }

    private static void processLdif(final SchemaManager schemaManager, final CoreSession adminSession, final String ldifName) throws LdapException, IOException {
        InputStream ldifInput = LdapTestSuite.class.getResourceAsStream(ldifName);
        LdifReader ldifReader = new LdifReader(ldifInput);
        for (LdifEntry ldifEntry : ldifReader) {
            adminSession.add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
        }
        ldifReader.close();
        ldifInput.close();
    }

    private static void createWorkingDir() throws IOException {
        if (workingDir == null) {
            if (workingDir == null) {
                workingDir = new File(".");
                workingDir = new File(workingDir, "target");
                workingDir = new File(workingDir, "apacheds_working").getCanonicalFile();
                if (!workingDir.exists()) {
                    workingDir.mkdirs();
                }
            }
        }
        for (File current : workingDir.listFiles()) {
          current.delete();
        }
    }

    @AfterClass
    public static void stopLdapServer() throws Exception {
        if (ldapServer != null) {
            ldapServer.stop();
        }
        if (directoryService != null) {
            directoryService.shutdown();
        }
        workingDir = null;
        initialised = false;
    }

}
