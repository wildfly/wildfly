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
import java.util.regex.Matcher;

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
        values.setPassword("1sT%l<[pzD");
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
        assertUserPropertyFile(userName, null);
    }

    protected void assertUserPropertyFile(String userName, String expectedPassword) throws StartException, IOException {
        assertUserPropertyFile(userName, expectedPassword, values.getOptions().isDisable());
    }

    private void assertUserPropertyFile(String userName, String expectedPassword, boolean disable) throws StartException, IOException {
        final String password;
        password = getPassword(userName, disable);
        assertNotNull(password);
        if (expectedPassword != null) {
            assertEquals(expectedPassword, password);
        }
    }

    protected String getPassword(String userName) throws IOException, StartException {
        return getValueFromFile(userName, values.getUserFiles().get(0).getAbsolutePath(), false);
    }

    private String getPassword(String userName, boolean disable) throws IOException, StartException {
        String password;
        if (disable) {
            // Read the file line by line and return the password of the user
            password = getDisabledUserPassword(userName);
        } else {
            // Load the properties and return the password of the user
            password = getEnabledUserPassword(userName);
        }
        return password;
    }

    protected void assertRolePropertyFile(String userName) throws StartException, IOException {
        assertRolePropertyFile(userName, values.getGroups());
    }

    protected void assertRolePropertyFile(String userName, String expectedRoles) throws StartException, IOException {
        assertRolePropertyFile(userName, expectedRoles, values.getOptions().isDisable());
    }

    private void assertRolePropertyFile(String userName, String expectedRoles, boolean disable) throws StartException, IOException {
        final String roles;
        roles = getRoles(userName, disable);
        assertEquals(expectedRoles, roles);
    }

    protected String getRoles(String userName) throws IOException, StartException {
        return getValueFromFile(userName, values.getGroupFiles().get(0).getAbsolutePath(), false);
    }

    private String getRoles(String userName, boolean disable) throws IOException, StartException {
        String roles;
        if (disable) {
            // Read the file line by line and return the roles of the user
            roles = getDisabledUserRoles(userName);
        } else {
            // Load the properties and return the roles of the user
            roles = getEnabledUserRoles(userName);
        }
        return roles;
    }

    /**
     * Get the password of the enabled user.<br/>
     * Read the properties users file and return the password of the user
     *
     * @param userName The name of the user
     * @return The password of the user
     * @throws StartException
     * @throws IOException
     */
    protected String getEnabledUserPassword(String userName) throws StartException, IOException {
        return getValueFromProperty(userName, values.getUserFiles().get(0).getAbsolutePath());
    }

    /**
     * Get the roles of the enabled user.<br/>
     * Read the properties roles file and return the roles of the user
     *
     * @param userName The name of the user
     * @return The roles of the user
     * @throws StartException
     * @throws IOException
     */
    protected String getEnabledUserRoles(String userName) throws StartException, IOException {
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
     * Get the password of the disabled user.<br/>
     * Read the users file line by line and return the password of the user.
     *
     * @param userName The name of the user
     * @return The password of the user
     * @throws IOException
     * @see #getDisabledValueFromFile(String, String)
     */
    private String getDisabledUserPassword(String userName) throws IOException {
        return getDisabledValueFromFile(userName, values.getUserFiles().get(0).getAbsolutePath());
    }

    /**
     * Get the roles of the disabled user.<br/>
     * Read the roles file line by line and return the roles of the user.
     *
     * @param userName The name of the user
     * @return The roles of the user
     * @throws IOException
     */
    private String getDisabledUserRoles(String userName) throws IOException {
        return getDisabledValueFromFile(userName, values.getGroupFiles().get(0).getAbsolutePath());
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
     * Get the value of the disabled username line.<br/>
     * Read the file line by line and return the disabled value of the username line.
     *
     * @param userName The name of the user
     * @param filePath The file path
     * @return The disabled value of the username line
     * @throws IOException
     */
    private String getDisabledValueFromFile(String userName, String filePath) throws IOException {
        return getValueFromFile(userName, filePath, true);
    }

    /**
     * Get the value of the username line.<br/>
     * Read the file line by line and return the value of the username line.
     *
     * @param userName The name of the user
     * @param filePath The file path
     * @param onlyDisabledLine return only disabled line
     * @return The value of the username line
     * @throws IOException
     */
    private String getValueFromFile(String userName, String filePath, boolean onlyDisabledLine) throws IOException {
        List<String> content = readContent(filePath);
        boolean found = false;
        String value = null;
        for (String line : content) {
            String trimmed = line.trim();
            Matcher matcher = PropertiesFileLoader.PROPERTY_PATTERN.matcher(trimmed);
            if (!onlyDisabledLine || trimmed.startsWith("#")) {
                if (matcher.matches() && userName.equals(matcher.group(1))) {
                    found = true;
                    value = matcher.group(2);
                    break;
                }
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
