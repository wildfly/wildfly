/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security;

import static org.jboss.as.test.integration.security.common.Utils.createTemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;
import org.wildfly.test.security.common.elytron.UserWithRoles;

/**
 * @author Jan Stourac
 */
public class WebSecurityCommon {
    /**
     * Reads 'elytron' property.
     *
     * @return true if 'elytron' property is defined, false otherwise.
     */
    public static boolean isElytron() {
        return System.getProperty("elytron") != null;
    }

    private static final Logger LOGGER = Logger.getLogger(WebSecurityCommon.class);


    public static PropertyFiles createPropertiesFiles(List<UserWithRoles> usersWithRoles, String realmName) throws
            Exception {
        File tempFolder = createTemporaryFolder("properties-" + realmName);
        final Properties usersProperties = new Properties();
        final Properties rolesProperties = new Properties();
        for (UserWithRoles user : usersWithRoles) {
            usersProperties.setProperty(user.getName(), user.getPassword());
            rolesProperties.setProperty(user.getName(), String.join(",", user.getRoles()));
        }
        File usersFile = new File(tempFolder, "users.properties");
        writeProperties(usersProperties, usersFile, realmName);
        File rolesFile = new File(tempFolder, "roles.properties");
        writeProperties(rolesProperties, rolesFile, realmName);

        return new PropertyFiles(usersFile, rolesFile);
    }

    public static void removePropertiesFiles(File tempFolder) throws Exception {
        FileUtils.deleteQuietly(tempFolder);
    }

    private static void writeProperties(Properties properties, File fileName, String realmName) throws IOException {
        LOGGER.debugv("Creating property file {0}", fileName);
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            // comment $REALM_NAME is just a workaround for https://issues.jboss.org/browse/WFLY-7104
            properties.store(fos, "$REALM_NAME=" + realmName + "$");
        }
    }

    public static class PropertyFiles {
        private final File usersFile;
        private final File rolesFile;

        public PropertyFiles(File usersFile, File rolesFile) {
            this.usersFile = usersFile;
            this.rolesFile = rolesFile;
        }

        public File getUsers() {
            return usersFile;
        }

        public File getRoles() {
            return rolesFile;
        }
    }
}
