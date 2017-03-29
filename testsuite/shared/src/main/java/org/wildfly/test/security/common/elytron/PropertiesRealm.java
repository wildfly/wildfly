/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.security.common.elytron;

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
