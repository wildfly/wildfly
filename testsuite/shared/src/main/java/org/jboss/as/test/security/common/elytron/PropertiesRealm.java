/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.security.common.elytron;

import static org.jboss.as.test.integration.security.common.Utils.createTemporaryFolder;
import static org.jboss.as.test.shared.CliUtils.asAbsolutePath;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.logging.Logger;

/**
 * Configuration for properties-realms Elytron resource.
 *
 * @author Josef Cacek
 */
public class PropertiesRealm extends AbstractUserRolesCapableElement implements SecurityRealm {

    private static final Logger LOGGER = Logger.getLogger(PropertiesRealm.class);

    private final String groupsAttribute;
    private File tempFolder;

    private PropertiesRealm(Builder builder) {
        super(builder);
        this.groupsAttribute = builder.groupsAttribute;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        this.tempFolder = createTemporaryFolder("ely-" + name);
        final Properties usersProperties = new Properties();
        final Properties rolesProperties = new Properties();
        for (UserWithRoles user : getUsersWithRoles()) {
            usersProperties.setProperty(user.getName(), user.getPassword());
            rolesProperties.setProperty(user.getName(), String.join(",", user.getRoles()));
        }
        File usersFile = writeProperties(usersProperties, "users.properties");
        File rolesFile = writeProperties(rolesProperties, "roles.properties");

        // /subsystem=elytron/properties-realm=test:add(users-properties={path=/tmp/users.properties, plain-text=true},
        // groups-properties={path=/tmp/groups.properties}, groups-attribute="groups")
        final String groupsAttrStr = groupsAttribute == null ? "" : String.format(", groups-attribute=\"%s\"", groupsAttribute);
        cli.sendLine(String.format(
                "/subsystem=elytron/properties-realm=%s:add(users-properties={path=\"%s\", plain-text=true}, groups-properties={path=\"%s\"}%s)",
                name, asAbsolutePath(usersFile), asAbsolutePath(rolesFile), groupsAttrStr));
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/properties-realm=%s:remove()", name));
        FileUtils.deleteQuietly(tempFolder);
        tempFolder = null;
    }

    /**
     * Creates builder to build {@link PropertiesRealm}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private File writeProperties(Properties properties, String fileName) throws IOException {
        File result = new File(tempFolder, fileName);
        LOGGER.debugv("Creating property file {0}", result);
        try (FileOutputStream fos = new FileOutputStream(result)) {
            // comment $REALM_NAME is just a workaround for https://issues.jboss.org/browse/WFLY-7104
            properties.store(fos, "$REALM_NAME=" + name + "$");
        }
        return result;
    }

    /**
     * Builder to build {@link PropertiesRealm}.
     */
    public static final class Builder extends AbstractUserRolesCapableElement.Builder<Builder> {
        private String groupsAttribute;

        private Builder() {
        }

        public Builder withGroupsAttribute(String groupsAttribute) {
            this.groupsAttribute = groupsAttribute;
            return this;
        }

        public PropertiesRealm build() {
            return new PropertiesRealm(this);
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
