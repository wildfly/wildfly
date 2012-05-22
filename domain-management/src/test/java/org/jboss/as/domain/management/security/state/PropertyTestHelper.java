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

package org.jboss.as.domain.management.security.state;

import org.jboss.as.domain.management.security.ConsoleMock;
import org.jboss.msc.service.StartException;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Helper for setting up a test case with ConsoleMock, StateValues and
 * property files for user and roles
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class PropertyTestHelper {

    protected static final String USER_NAME = "Aldo.Raine";
    protected static final String ROLES = "admin, jms";
    protected ConsoleMock consoleMock;
    protected StateValues values;

    @Before
    public void setUp() throws IOException {
        ArrayList<File> usersPropertyFileList = new ArrayList<File>();
        ArrayList<File> rolesPropertyFileList = new ArrayList<File>();

        File usersPropertyFile = File.createTempFile("UpdateUser", null);
        usersPropertyFile.deleteOnExit();

        File rolesPropertyFile = File.createTempFile("UpdateRoles", null);
        rolesPropertyFile.deleteOnExit();

        usersPropertyFileList.add(usersPropertyFile);
        rolesPropertyFileList.add(rolesPropertyFile);

        values = new StateValues();
        values.setPropertiesFiles(usersPropertyFileList);
        values.setRoleFiles(rolesPropertyFileList);
        values.setUserName(USER_NAME);
        values.setPassword("1sT%l<[pzD".toCharArray());
        values.setRealm("Management");
        consoleMock = new ConsoleMock();
    }

    protected Properties loadProperties(String filePath) throws StartException, IOException {
        UserPropertiesFileHandler propertiesLoad = new UserPropertiesFileHandler(filePath);
        propertiesLoad.start(null);
        Properties properties = (Properties) propertiesLoad.getProperties().clone();
        propertiesLoad.stop(null);
        return properties;
    }

    protected void assertUserPropertyFile(String userName) throws StartException, IOException {
        Properties properties = loadProperties(values.getPropertiesFiles().get(0).getAbsolutePath());
        String password = (String) properties.get(userName);
        assertNotNull(password);
    }

    protected void assertRolePropertyFile(String userName) throws StartException, IOException {
        Properties properties = loadProperties(values.getRoleFiles().get(0).getAbsolutePath());
        String roles = (String) properties.get(userName);
        assertEquals(ROLES,roles);
    }
}
