/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

/**
 * Describe the purpose
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */

import org.jboss.as.domain.management.security.ConsoleWrapper;
import org.jboss.as.domain.management.security.PropertiesFileLoader;
import org.jboss.msc.service.StartException;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
import static org.jboss.as.domain.management.security.AddPropertiesUser.*;

/**
 * The first state executed, responsible for searching for the relevant properties files.
 */
public class PropertyFileFinder implements State {

    private ConsoleWrapper theConsole;
    private final StateValues stateValues;

    public PropertyFileFinder(ConsoleWrapper theConsole,final StateValues stateValues) {
        this.theConsole = theConsole;
        this.stateValues = stateValues;
        if (theConsole.getConsole() == null) {
            throw MESSAGES.noConsoleAvailable();
        }
    }

    @Override
    public State execute() {
        stateValues.setKnownRoles(new HashMap<String, String>());
        String jbossHome = stateValues.getJBossHome();
        if (jbossHome == null) {
            return new ErrorState(theConsole, MESSAGES.jbossHomeNotSet(), null, stateValues);
        }

        List<File> foundFiles = new ArrayList<File>(2);
        final String fileName = stateValues.isManagement() ? MGMT_USERS_PROPERTIES : APPLICATION_USERS_PROPERTIES;
        if (!findFiles(jbossHome, foundFiles, fileName)) {
            return new ErrorState(theConsole, MESSAGES.propertiesFileNotFound(fileName), null, stateValues);
        }
        if (!stateValues.isManagement()) {
            List<File> foundRoleFiles = new ArrayList<File>(2);
            if (!findFiles(jbossHome, foundRoleFiles, APPLICATION_ROLES_PROPERTIES)) {
                return new ErrorState(theConsole, MESSAGES.propertiesFileNotFound(APPLICATION_ROLES_PROPERTIES), null, stateValues);
            }
            stateValues.setRoleFiles(foundRoleFiles);
            try {
                stateValues.setKnownRoles(loadAllRoles(foundRoleFiles));
            } catch (Exception e) {
                return new ErrorState(theConsole, MESSAGES.propertiesFileNotFound(APPLICATION_ROLES_PROPERTIES), null, stateValues);
            }
        }

        stateValues.setPropertiesFiles(foundFiles);

        Set<String> foundUsers = new HashSet<String>();
        for (File current : stateValues.getPropertiesFiles()) {
            try {
                foundUsers.addAll(loadUserNames(current));
            } catch (IOException e) {
                return new ErrorState(theConsole, MESSAGES.unableToLoadUsers(current.getAbsolutePath(), e.getMessage()), null, stateValues);
            }
        }
        stateValues.setKnownUsers(foundUsers);

        if (stateValues == null) {
            return new PromptNewUserState(theConsole);
        } else {
            return new PromptNewUserState(theConsole, stateValues);
        }
    }

    private Map<String,String> loadAllRoles(List<File> foundRoleFiles) throws StartException, IOException {
        Map<String, String> loadedRoles = new HashMap<String, String>();
        for (File file : foundRoleFiles) {
            PropertiesFileLoader propertiesLoad = null;
            try {
                propertiesLoad = new UserPropertiesFileHandler(file.getCanonicalPath());
                propertiesLoad.start(null);
                loadedRoles.putAll((Map) propertiesLoad.getProperties());
            }  finally {
                if (propertiesLoad!=null) {
                    propertiesLoad.stop(null);
                }
            }
        }
        return loadedRoles;
    }

    private boolean findFiles(final String jbossHome, final List<File> foundFiles, final String fileName) {
        File standaloneProps = buildFilePath(jbossHome, SERVER_CONFIG_USER_DIR, SERVER_CONFIG_DIR, SERVER_BASE_DIR, "standalone", fileName);
        if (standaloneProps.exists()) {
            foundFiles.add(standaloneProps);
        }
        File domainProps = buildFilePath(jbossHome, DOMAIN_CONFIG_USER_DIR,DOMAIN_CONFIG_DIR, DOMAIN_BASE_DIR, "domain", fileName);
        if (domainProps.exists()) {
            foundFiles.add(domainProps);
        }

        if (foundFiles.size() == 0) {
            return false;
        }
        return true;
    }

    private File buildFilePath(final String jbossHome, final String serverCofigUserDirPropertyName, final String serverConfigDirPropertyName,
                               final String serverBaseDirPropertyName, final String defaultBaseDir, final String fileName) {

        String configUserDirConfiguredPath = System.getProperty(serverCofigUserDirPropertyName);
        String configDirConfiguredPath = configUserDirConfiguredPath != null ? configUserDirConfiguredPath : System.getProperty(serverConfigDirPropertyName);

        File configDir =  configDirConfiguredPath != null ? new File(configDirConfiguredPath) : null;
        if(configDir == null) {
            String baseDirConfiguredPath = System.getProperty(serverBaseDirPropertyName);
            File baseDir = baseDirConfiguredPath != null ? new File(baseDirConfiguredPath) : new File(jbossHome, defaultBaseDir);
            configDir = new File(baseDir, "configuration");
        }
        return new File(configDir, fileName);
    }

    private Set<String> loadUserNames(final File file) throws IOException {

        InputStreamReader fis = null;
        try {
            fis = new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8"));
            Properties tempProps = new Properties();
            tempProps.load(fis);

            return tempProps.stringPropertyNames();
        } finally {
            safeClose(fis);
        }

    }

    private static void safeClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }

}
