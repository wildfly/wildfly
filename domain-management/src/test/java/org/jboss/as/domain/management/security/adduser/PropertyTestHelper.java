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

package org.jboss.as.domain.management.security.adduser;

import org.jboss.as.domain.management.security.PropertiesFileLoader;
import org.jboss.msc.service.StartException;
import org.junit.Before;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        values.setUserFiles(usersPropertyFileList);
        values.setGroupFiles(rolesPropertyFileList);
        values.setUserName(USER_NAME);
        values.setPassword("1sT%l<[pzD".toCharArray());
        values.setRealm("Management");
        consoleMock = new ConsoleMock();
    }

    protected Properties loadProperties(String filePath) throws StartException, IOException {
        PropertiesFileLoader propertiesLoad = new PropertiesFileLoader(filePath);
        propertiesLoad.start(null);
        Properties properties = (Properties) propertiesLoad.getProperties().clone();
        propertiesLoad.stop(null);
        return properties;
    }

    protected void assertUserPropertyFile(String userName) throws StartException, IOException {
        assertUserPropertyFile(userName, false);
    }

    protected void assertUserPropertyFile(String userName, boolean disable) throws StartException, IOException {
        final String password;
        if (disable) {
            // Read the file line by line and return the password of the user
            password = getPasswordFromUserFile(userName);
        } else {
            // Load the properties and return the password of the user
            password = getPasswordFromUserProperty(userName);
        }
        assertNotNull(password);
    }

    protected void assertRolePropertyFile(String userName) throws StartException, IOException {
        assertRolePropertyFile(userName, false);
    }

    protected void assertRolePropertyFile(String userName, boolean disable) throws StartException, IOException {
        final String roles;
        if (disable) {
            // Read the file line by line and return the roles of the user
            roles = getRolesFromRoleFile(userName);
        } else {
            // Load the properties and return the roles of the user
            roles = getRolesFromRoleProperty(userName);
        }
        assertEquals(ROLES, roles);
    }

    /**
     * Read the properties users file and return the password of the user
     *
     * @param userName The name of the user
     * @return The password of the user
     * @throws StartException
     * @throws IOException
     */
    protected String getPasswordFromUserProperty(String userName) throws StartException, IOException {
        return getValueFromProperty(userName, values.getUserFiles().get(0).getAbsolutePath());
    }

    /**
     * Read the properties roles file and return the roles of the user
     *
     * @param userName The name of the user
     * @return The roles of the user
     * @throws StartException
     * @throws IOException
     */
    protected String getRolesFromRoleProperty(String userName) throws StartException, IOException {
        return getValueFromProperty(userName, values.getGroupFiles().get(0).getAbsolutePath());
    }

    /**
     * Load properties from the file and return the value of the username property
     *
     * @param userName The name of the user
     * @param filePath The file path
     * @return The value of the username property
     * @throws StartException
     * @throws IOException
     */
    private String getValueFromProperty(String userName, String filePath) throws StartException, IOException {
        Properties properties = loadProperties(filePath);
        return (String) properties.get(userName);
    }

    /**
     * Read the users file line by line and return the password of the user.
     *
     * @param userName The name of the user
     * @return The password of the user
     * @throws IOException
     * @see #getValueFromFile(String, String)
     */
    private String getPasswordFromUserFile(String userName) throws IOException {
        return getValueFromFile(userName, values.getUserFiles().get(0).getAbsolutePath());
    }

    /**
     * Read the roles file line by line and return the roles of the user.
     *
     * @param userName The name of the user
     * @return The roles of the user
     * @throws IOException
     */
    private String getRolesFromRoleFile(String userName) throws IOException {
        return getValueFromFile(userName, values.getGroupFiles().get(0).getAbsolutePath());
    }

    /**
     * Count the number of lines in the users file.
     *
     * @return The number of lines in the file
     * @throws IOException
     */
    protected int countLineNumberUserFile() throws IOException {
        return readContent(values.getUserFiles().get(0).getAbsolutePath()).size();
    }

    /**
     * Count the number of lines in the roles file
     *
     * @return The number of lines in the file
     * @throws IOException
     */
    protected int countLineNumberRoleFile() throws IOException {
        return readContent(values.getGroupFiles().get(0).getAbsolutePath()).size();
    }

    /**
     * Read the file line by line and return the value of the username line.
     *
     * @param userName The name of the user
     * @param filePath The file path
     * @return The value of the username line
     * @throws IOException
     */
    private String getValueFromFile(String userName, String filePath) throws IOException {
        List<String> content = readContent(filePath);
        boolean found = false;
        String value = null;
        for (String line : content) {
            String trimmed = line.trim();
            if (trimmed.contains("#" + userName)) {
                found = true;
                value = trimmed.substring(trimmed.indexOf('=') + 1, trimmed.length());
                break;
            }
        }
        assertTrue(found);
        return value;
    }

    private List<String> readContent(String filePath) throws IOException {
        List<String> content = new ArrayList<String>();
        FileReader fileReader = new FileReader(filePath);
        BufferedReader bufferedFileReader = new BufferedReader(fileReader);
        try {
            String line;
            while ((line = bufferedFileReader.readLine()) != null) {
                content.add(line);
            }
        } finally {
            safeClose(bufferedFileReader);
            safeClose(fileReader);
        }
        return content;
    }

    private void safeClose(final Closeable c) {
        try {
            c.close();
        } catch (IOException ignored) {
        }
    }
}
