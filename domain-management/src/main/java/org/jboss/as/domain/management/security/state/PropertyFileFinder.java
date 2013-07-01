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

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
import static org.jboss.as.domain.management.security.AddPropertiesUser.APPLICATION_ROLES_PROPERTIES;
import static org.jboss.as.domain.management.security.AddPropertiesUser.APPLICATION_USERS_PROPERTIES;
import static org.jboss.as.domain.management.security.AddPropertiesUser.DOMAIN_BASE_DIR;
import static org.jboss.as.domain.management.security.AddPropertiesUser.DOMAIN_CONFIG_DIR;
import static org.jboss.as.domain.management.security.AddPropertiesUser.DOMAIN_CONFIG_USER_DIR;
import static org.jboss.as.domain.management.security.AddPropertiesUser.MGMT_USERS_PROPERTIES;
import static org.jboss.as.domain.management.security.AddPropertiesUser.SERVER_BASE_DIR;
import static org.jboss.as.domain.management.security.AddPropertiesUser.SERVER_CONFIG_DIR;
import static org.jboss.as.domain.management.security.AddPropertiesUser.SERVER_CONFIG_USER_DIR;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.domain.management.security.AddPropertiesUser.RealmMode;
import org.jboss.as.domain.management.security.ConsoleWrapper;
import org.jboss.as.domain.management.security.PropertiesFileLoader;
import org.jboss.msc.service.StartException;

/**
 * The first state executed, responsible for searching for the relevant properties files.
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class PropertyFileFinder implements State {

    private ConsoleWrapper theConsole;
    private final StateValues stateValues;

    public PropertyFileFinder(ConsoleWrapper theConsole, final StateValues stateValues) {
        this.theConsole = theConsole;
        this.stateValues = stateValues;
        if ((stateValues != null && stateValues.isSilent() == false) && theConsole.getConsole() == null) {
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

        stateValues.setUserFiles(foundFiles);

        String realmName = null;
        Set<String> foundUsers = new HashSet<String>();
        for (File current : stateValues.getUserFiles()) {
            PropertiesFileLoader pfl = null;
            try {
                pfl = loadUsersFile(current);
                foundUsers.addAll(pfl.getProperties().stringPropertyNames());
                if (realmName == null) {
                    realmName = pfl.getRealmName();
                } else {
                    String nextRealm = pfl.getRealmName();
                    if (realmName.equals(nextRealm)==false) {
                        return new ErrorState(theConsole, MESSAGES.multipleRealmsDetected(realmName, nextRealm), null, stateValues);
                    }
                }
                pfl.stop(null);
                pfl = null;
            } catch (IOException e) {
                return new ErrorState(theConsole, MESSAGES.unableToLoadUsers(current.getAbsolutePath(), e.getMessage()), null, stateValues);
            } finally {
                if (pfl != null) {
                    pfl.stop(null);
                    pfl = null;
                }
            }
        }
        if (realmName != null) {
            if (stateValues.getRealmMode() == RealmMode.USER_SUPPLIED && realmName.equals(stateValues.getRealm()) == false) {
                return new ErrorState(theConsole, MESSAGES.userRealmNotMatchDiscovered(stateValues.getRealm(), realmName),
                        null, stateValues);
            }

            stateValues.setRealm(realmName);
            stateValues.setRealmMode(RealmMode.DISCOVERED);
        }
        stateValues.setKnownUsers(foundUsers);

        // TODO - Should we go straight to user validation instead of prompting?
        return stateValues.isInteractive() ? new PromptRealmState(theConsole, stateValues) : new PromptNewUserState(theConsole,
                stateValues);
    }

    private PropertiesFileLoader loadUsersFile(File file) throws IOException {
        PropertiesFileLoader fileLoader = new UserPropertiesFileHandler(file.getAbsolutePath());
        try {
            fileLoader.start(null);
        } catch (StartException e) {
            throw new IOException(e);
        }

        return fileLoader;
    }

    private Map<String, String> loadAllRoles(List<File> foundRoleFiles) throws StartException, IOException {
        Map<String, String> loadedRoles = new HashMap<String, String>();
        for (File file : foundRoleFiles) {
            PropertiesFileLoader propertiesLoad = null;
            try {
                propertiesLoad = new UserPropertiesFileHandler(file.getCanonicalPath());
                propertiesLoad.start(null);
                loadedRoles.putAll((Map) propertiesLoad.getProperties());
            } finally {
                if (propertiesLoad != null) {
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
        File domainProps = buildFilePath(jbossHome, DOMAIN_CONFIG_USER_DIR, DOMAIN_CONFIG_DIR, DOMAIN_BASE_DIR, "domain", fileName);
        if (domainProps.exists()) {
            foundFiles.add(domainProps);
        }

        return !foundFiles.isEmpty();
    }

    private File buildFilePath(final String jbossHome, final String serverCofigUserDirPropertyName, final String serverConfigDirPropertyName,
                               final String serverBaseDirPropertyName, final String defaultBaseDir, final String fileName) {

        String configUserDirConfiguredPath = System.getProperty(serverCofigUserDirPropertyName);
        String configDirConfiguredPath = configUserDirConfiguredPath != null ? configUserDirConfiguredPath : System.getProperty(serverConfigDirPropertyName);

        File configDir = configDirConfiguredPath != null ? new File(configDirConfiguredPath) : null;
        if (configDir == null) {
            String baseDirConfiguredPath = System.getProperty(serverBaseDirPropertyName);
            File baseDir = baseDirConfiguredPath != null ? new File(baseDirConfiguredPath) : new File(jbossHome, defaultBaseDir);
            configDir = new File(baseDir, "configuration");
        }
        return new File(configDir, fileName);
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
