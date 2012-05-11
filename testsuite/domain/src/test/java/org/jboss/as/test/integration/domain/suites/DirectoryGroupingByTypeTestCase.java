/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.junit.Assert.*;

import java.io.File;

import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test that <servers directory-grouping="by-type"> results in the proper directory organization
 *
 * @author @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class DirectoryGroupingByTypeTestCase {

    private static DomainTestSupport testSupport;

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(CoreResourceManagementTestCase.class.getSimpleName());
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        testSupport = null;
        DomainTestSuite.stopSupport();
    }

    @Test
    public void testDirectoryLocations() throws Exception {
        File baseDir = new File(testSupport.getDomainSlaveConfiguration().getDomainDirectory());
        validateDirectory(baseDir);
        File data = new File(baseDir, "data");
        validateDirectory(data);
        validateServerDirectory(data);
        File log = new File(baseDir, "log");
        validateDirectory(log);
        validateServerDirectory(log);
        File tmp = new File(baseDir, "tmp");
        validateDirectory(tmp);
        validateServerDirectory(tmp);
    }

    private void validateServerDirectory(File typeDir) {
        File servers = new File(typeDir, "servers");
        validateDirectory(servers);
        File server = new File(servers, "main-three");
        validateDirectory(server);
    }

    private void validateDirectory(File file) {
        assertTrue(file + " exists", file.exists());
        assertTrue(file + " is a directory", file.isDirectory());
    }
}
