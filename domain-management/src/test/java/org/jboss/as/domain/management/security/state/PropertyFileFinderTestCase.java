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

import org.jboss.as.domain.management.security.AddPropertiesUser;
import org.jboss.as.domain.management.security.AssertConsoleBuilder;
import org.jboss.msc.service.StartException;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Properties;

import static java.lang.System.getProperty;
import static org.junit.Assert.assertTrue;

/**
 * Test the property file finder.
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class PropertyFileFinderTestCase extends PropertyTestHelper {


    @Before
    public void setup() throws IOException {
        values.setManagement(true);
        values.setJbossHome(getProperty("java.io.tmpdir"));
    }

    private File createPropertyFile(String filename, String mode) throws IOException {

        File domainDir = new File(getProperty("java.io.tmpdir")+File.separator+mode);
        domainDir.mkdir();
        domainDir.deleteOnExit();
        File propertyUserFile = new File(domainDir, filename);
        propertyUserFile.createNewFile();
        propertyUserFile.deleteOnExit();
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(propertyUserFile),"UTF8"));
        try {
          Properties domainPropeties = new Properties();
          domainPropeties.setProperty(USER_NAME,"mypassword");
          domainPropeties.store(bw,"");
        } finally {
           bw.close();
        }
        return propertyUserFile;
    }

    @Test
    public void overridePropertyfileLocationRead() throws IOException {
        File domainMgmtUserFile = createPropertyFile("mgmt-users.properties", "domain");
        File standaloneMgmtUserFile = createPropertyFile("mgmt-users.properties", "standalone");

        System.setProperty("jboss.server.config.user.dir", standaloneMgmtUserFile.getParent());
        System.setProperty("jboss.domain.config.user.dir", domainMgmtUserFile.getParent());
        State propertyFileFinder = new PropertyFileFinder(consoleMock, values);
        State nextState = propertyFileFinder.execute();
        assertTrue(nextState instanceof PromptNewUserState);
        assertTrue("Expected to find the "+USER_NAME+" in the list of known users",values.getKnownUsers().contains(USER_NAME));
        assertTrue("Expected the values.getPropertiesFiles() contained the "+standaloneMgmtUserFile,values.getPropertiesFiles().contains(standaloneMgmtUserFile));
        assertTrue("Expected the values.getPropertiesFiles() contained the "+domainMgmtUserFile,values.getPropertiesFiles().contains(domainMgmtUserFile));
    }

    @Test
    public void overridePropertyfileLocationWrite() throws IOException, StartException {
        File domainUserFile = createPropertyFile("application-users.properties", "domain");
        File standaloneUserFile = createPropertyFile("application-users.properties", "standalone");
        File domainRolesFile = createPropertyFile("application-roles.properties", "domain");
        File standaloneRolesFile = createPropertyFile("application-roles.properties", "standalone");

        String newUserName = "Hugh.Jackman";
        values.setRoles(null);
        values.setUserName(newUserName);
        values.setManagement(false);
        System.setProperty("jboss.server.config.user.dir", domainUserFile.getParent());
        System.setProperty("jboss.domain.config.user.dir", standaloneUserFile.getParent());
        State propertyFileFinder = new PropertyFileFinder(consoleMock, values);
        State nextState = propertyFileFinder.execute();
        assertTrue(nextState instanceof PromptNewUserState);

        File locatedDomainPropertyFile = values.getPropertiesFiles().get(values.getPropertiesFiles().indexOf(domainUserFile));
        File locatedStandalonePropertyFile = values.getPropertiesFiles().get(values.getPropertiesFiles().indexOf(standaloneUserFile));
        UpdateUser updateUserState = new UpdateUser(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().
                expectedDisplayText(updateUserState.consoleUserMessage(locatedDomainPropertyFile.getCanonicalPath())).
                expectedDisplayText(AddPropertiesUser.NEW_LINE).
                expectedDisplayText(updateUserState.consoleUserMessage(locatedStandalonePropertyFile.getCanonicalPath())).
                expectedDisplayText(AddPropertiesUser.NEW_LINE);
        consoleMock.setResponses(consoleBuilder);
        updateUserState.update(values);

        assertUserPropertyFile(newUserName);
        consoleBuilder.validate();
    }


}
