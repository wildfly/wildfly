/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
public class PropertyFileBasedDomain extends AbstractUserAttributeValuesCapableElement implements SecurityDomain {

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
        for (UserWithAttributeValues user : getUsersWithAttributeValues()) {
            usersProperties.setProperty(user.getName(), user.getPassword());
            rolesProperties.setProperty(user.getName(), String.join(",", user.getValues()));
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

    public static final class Builder extends AbstractUserAttributeValuesCapableElement.Builder<Builder> {

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
