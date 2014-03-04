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

package org.jboss.as.test.integration.mgmt.access;

import java.io.InputStream;

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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
@RunWith(Suite.class) // ensure that the LDAP server is started before Arquillian kicks in
@Suite.SuiteClasses(PropertiesRoleMappingTestCase.class) // the test cases are completely the same
public class LdapRoleMappingU2GTestCase {
    private static DirectoryService directoryService;
    private static LdapServer ldapServer;

    @BeforeClass
    @CreateDS(
            name = "WildFlyDS",
            partitions = @CreatePartition(name = "wildfly", suffix = "dc=wildfly,dc=org"),
            allowAnonAccess = true
    )
    @CreateLdapServer(
            transports = @CreateTransport(protocol = "LDAP", address = "localhost", port = 10389),
            allowAnonymousAccess = true
    )
    public static void setUp() throws Exception {
        directoryService = DSAnnotationProcessor.getDirectoryService();
        SchemaManager schemaManager = directoryService.getSchemaManager();
        InputStream ldif = LdapRoleMappingU2GTestCase.class.getResourceAsStream("/" + LdapRoleMappingU2GTestCase.class.getSimpleName() + ".ldif");
        for (LdifEntry ldifEntry : new LdifReader(ldif)) {
            directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
        }
        ldapServer = ServerAnnotationProcessor.getLdapServer(directoryService);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        ldapServer.stop();
        directoryService.shutdown();
        FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
    }
}
