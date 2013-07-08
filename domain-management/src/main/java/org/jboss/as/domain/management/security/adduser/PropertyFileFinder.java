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

package org.jboss.as.domain.management.security.adduser;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
import static org.jboss.as.domain.management.security.adduser.AddUser.APPLICATION_ROLES_PROPERTIES;
import static org.jboss.as.domain.management.security.adduser.AddUser.APPLICATION_USERS_PROPERTIES;
import static org.jboss.as.domain.management.security.adduser.AddUser.DOMAIN_BASE_DIR;
import static org.jboss.as.domain.management.security.adduser.AddUser.DOMAIN_CONFIG_DIR;
import static org.jboss.as.domain.management.security.adduser.AddUser.DOMAIN_CONFIG_USER_DIR;
import static org.jboss.as.domain.management.security.adduser.AddUser.MGMT_USERS_PROPERTIES;
import static org.jboss.as.domain.management.security.adduser.AddUser.MGMT_GROUPS_PROPERTIES;
import static org.jboss.as.domain.management.security.adduser.AddUser.SERVER_BASE_DIR;
import static org.jboss.as.domain.management.security.adduser.AddUser.SERVER_CONFIG_DIR;
import static org.jboss.as.domain.management.security.adduser.AddUser.SERVER_CONFIG_USER_DIR;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.domain.management.security.PropertiesFileLoader;
import org.jboss.as.domain.management.security.UserPropertiesFileLoader;
import org.jboss.as.domain.management.security.adduser.AddUser.FileMode;
import org.jboss.as.domain.management.security.adduser.AddUser.RealmMode;
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
        stateValues.setKnownGroups(new HashMap<String, String>());

        if (stateValues.getOptions().getGroupProperties() != null && stateValues.getOptions().getUserProperties() == null) {
            return new ErrorState(theConsole, MESSAGES.groupPropertiesButNoUserProperties(stateValues.getOptions()
                    .getGroupProperties()), null, stateValues);
        }

        List<File> foundFiles = new ArrayList<File>(2);
        String fileName = stateValues.getOptions().getUserProperties();
        fileName = fileName == null ? stateValues.getFileMode() == FileMode.MANAGEMENT ? MGMT_USERS_PROPERTIES : APPLICATION_USERS_PROPERTIES : fileName;
        if (!findFiles(foundFiles, fileName)) {
            return new ErrorState(theConsole, MESSAGES.propertiesFileNotFound(fileName), null, stateValues);
        }
        fileName = stateValues.getOptions().getGroupProperties();


        if (fileName != null || stateValues.getFileMode() != FileMode.UNDEFINED) {
            boolean groupFileMandatory = true;
            List<File> foundGroupFiles = new ArrayList<File>(2);
            if (fileName == null && stateValues.getFileMode() == FileMode.APPLICATION) {
                fileName = APPLICATION_ROLES_PROPERTIES;
            } else if (fileName == null) {
                fileName = MGMT_GROUPS_PROPERTIES;
                groupFileMandatory = false;
            }
            fileName = fileName == null ? APPLICATION_ROLES_PROPERTIES : fileName;
            if (!findFiles(foundGroupFiles, fileName) && groupFileMandatory) {
                return new ErrorState(theConsole, MESSAGES.propertiesFileNotFound(fileName), null, stateValues);
            }
            stateValues.setGroupFiles(foundGroupFiles);
            try {
                stateValues.setKnownGroups(loadAllGroups(foundGroupFiles));
            } catch (Exception e) {
                return new ErrorState(theConsole, MESSAGES.propertiesFileNotFound(fileName), null, stateValues);
            }
        }

        stateValues.setUserFiles(foundFiles);

        String realmName = null;
        Set<String> foundUsers = new HashSet<String>();
        for (File current : stateValues.getUserFiles()) {
            UserPropertiesFileLoader pfl = null;
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

    private UserPropertiesFileLoader loadUsersFile(File file) throws IOException {
        UserPropertiesFileLoader fileLoader = new UserPropertiesFileLoader(file.getAbsolutePath());
        try {
            fileLoader.start(null);
        } catch (StartException e) {
            throw new IOException(e);
        }

        return fileLoader;
    }

    private Map<String, String> loadAllGroups(List<File> foundGroupsFiles) throws StartException, IOException {
        Map<String, String> loadedGroups = new HashMap<String, String>();
        for (File file : foundGroupsFiles) {
            PropertiesFileLoader propertiesLoad = null;
            try {
                propertiesLoad = new PropertiesFileLoader(file.getCanonicalPath());
                propertiesLoad.start(null);
                loadedGroups.putAll((Map) propertiesLoad.getProperties());
            } finally {
                if (propertiesLoad != null) {
                    propertiesLoad.stop(null);
                }
            }
        }
        return loadedGroups;
    }

    private boolean findFiles(final List<File> foundFiles, final String fileName) {
        File singleFile = new File(fileName);
        if (singleFile.exists()) {
            foundFiles.add(singleFile);
            return true;
        }

        File standaloneProps = buildFilePath(SERVER_CONFIG_USER_DIR, stateValues.getOptions().getServerConfigDir(),
                                             SERVER_CONFIG_DIR, SERVER_BASE_DIR, "standalone", fileName);
        if (standaloneProps.exists()) {
            foundFiles.add(standaloneProps);
        }
        File domainProps = buildFilePath(DOMAIN_CONFIG_USER_DIR, stateValues.getOptions().getDomainConfigDir(),
                                         DOMAIN_CONFIG_DIR, DOMAIN_BASE_DIR, "domain", fileName);
        if (domainProps.exists()) {
            foundFiles.add(domainProps);
        }

        return !foundFiles.isEmpty();
    }

    private File buildFilePath(final String serverConfigUserDirPropertyName, final String suppliedConfigDir,
            final String serverConfigDirPropertyName, final String serverBaseDirPropertyName, final String defaultBaseDir,
            final String fileName) {
        return new File(buildDirPath(serverConfigUserDirPropertyName, suppliedConfigDir, serverConfigDirPropertyName,
                                     serverBaseDirPropertyName, defaultBaseDir), fileName);
    }

    /**
     * This method attempts to locate a suitable directory by checking a number of different configuration sources.
     *
     * 1 - serverConfigUserDirPropertyName - This value is used to check it a matching system property has been set. 2 -
     * suppliedConfigDir - If a path was specified on the command line it is expected to be passed in as this parameter. 3 -
     * serverConfigDirPropertyName - This is a second system property to check.
     *
     * And finally if none of these match defaultBaseDir specifies the configuration being searched and is appended to the JBoss
     * Home value discovered when the utility started.
     */
    private File buildDirPath(final String serverConfigUserDirPropertyName, final String suppliedConfigDir,
            final String serverConfigDirPropertyName, final String serverBaseDirPropertyName, final String defaultBaseDir) {
        String propertyDir = System.getProperty(serverConfigUserDirPropertyName);
        if (propertyDir != null) {
            return new File(propertyDir);
        }
        if (suppliedConfigDir != null) {
            return new File(suppliedConfigDir);
        }
        propertyDir = System.getProperty(serverConfigDirPropertyName);
        if (propertyDir != null) {
            return new File(propertyDir);
        }

        propertyDir = System.getProperty(serverBaseDirPropertyName);
        if (propertyDir != null) {
            return new File(propertyDir);
        }

        return new File(new File(stateValues.getOptions().getJBossHome(), defaultBaseDir), "configuration");
    }

}
