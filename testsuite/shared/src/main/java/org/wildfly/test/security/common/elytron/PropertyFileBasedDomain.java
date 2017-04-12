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
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import org.wildfly.security.auth.permission.LoginPermission;

/**
 * Elytron Security domain implementation which uses {@code properties-realm} to hold the users.
 * <p>
 * For this given domain are created also following Elytron resources (with the same name as the domain):
 * </p>
 * <ul>
 * <li>properties-realm</li>
 * <li>simple-role-decoder</li>
 * <li>constant-permission-mapper</li>
 * </ul>
 *
 * @author Josef Cacek
 */
public class PropertyFileBasedDomain extends AbstractUserRolesCapableElement implements SecurityDomain {

    private static final Logger LOGGER = Logger.getLogger(PropertyFileBasedDomain.class);

    private File tempFolder;

    protected final String permissionMapper;

    private PropertyFileBasedDomain(Builder builder) {
        super(builder);
        this.permissionMapper = builder.permMapper;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        tempFolder = createTemporaryFolder("ely-" + getName());
        final Properties usersProperties = new Properties();
        final Properties rolesProperties = new Properties();
        for (UserWithRoles user : getUsersWithRoles()) {
            usersProperties.setProperty(user.getName(), user.getPassword());
            rolesProperties.setProperty(user.getName(), String.join(",", user.getRoles()));
        }
        File usersFile = writeProperties(usersProperties, "users.properties");
        File rolesFile = writeProperties(rolesProperties, "roles.properties");

        // /subsystem=elytron/properties-realm=test:add(users-properties={path=/tmp/users.properties, plain-text=true},
        // groups-properties={path=/tmp/groups.properties})
        cli.sendLine(String.format(
                "/subsystem=elytron/properties-realm=%s:add(users-properties={path=\"%s\", plain-text=true}, groups-properties={path=\"%s\"})",
                name, asAbsolutePath(usersFile), asAbsolutePath(rolesFile)));

        // /subsystem=elytron/simple-role-decoder=test:add(attribute=groups)
        cli.sendLine(String.format("/subsystem=elytron/simple-role-decoder=%s:add(attribute=groups)", name));

        if(permissionMapper == null) {  // create a default permission mapper if a custom one wasn't specified
            // /subsystem=elytron/constant-permission-mapper=test:add(permissions=[{class-name="org.wildfly.security.auth.permission.LoginPermission"}])
            cli.sendLine(String.format("/subsystem=elytron/constant-permission-mapper=%s:add(permissions=[{class-name=\"%s\"}])",
                    name, LoginPermission.class.getName()));
        }


        final String permissionMapperName = permissionMapper == null ? name : permissionMapper;
        // /subsystem=elytron/security-domain=test:add(default-realm=test, permission-mapper=PERMISSION_MAPPER, realms=[{role-decoder=test,
        // realm=test}]
        cli.sendLine(String.format(
                "/subsystem=elytron/security-domain=%s:add(default-realm=%1$s, permission-mapper=%2$s, realms=[{role-decoder=%1$s, realm=%1$s}]",
                name, permissionMapperName));
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:remove()", name));
        if(permissionMapper == null) {
            cli.sendLine(String.format("/subsystem=elytron/constant-permission-mapper=%s:remove()", name));
        }
        cli.sendLine(String.format("/subsystem=elytron/simple-role-decoder=%s:remove()", name));
        cli.sendLine(String.format("/subsystem=elytron/properties-realm=%s:remove()", name));

        FileUtils.deleteQuietly(tempFolder);
    }

    /**
     * Creates builder to build {@link PropertyFileBasedDomain}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractUserRolesCapableElement.Builder<Builder> {

        private String permMapper;

        private Builder() {
            // empty
        }

        public PropertyFileBasedDomain build() {
            return new PropertyFileBasedDomain(this);
        }

        public Builder permissionMapper(String name) {
            permMapper = name;
            return self();
        }

        @Override
        protected Builder self() {
            return this;
        }

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
}
