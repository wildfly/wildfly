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

import static org.jboss.as.test.shared.CliUtils.asAbsolutePath;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.logging.Logger;
import org.wildfly.security.auth.permission.LoginPermission;

/**
 * Elytron Security domain implementation which uses {@code properties-realm} to hold the users and roles for
 * authorization and given realm for authentication.
 * <p>
 * For this given domain are created also following Elytron resources (with the name based on the domain name):
 * <ul>
 * <li>properties-realm (name has "-authzRealm" suffix)</li>
 * <li>aggregate-realm (name has "-aggregateRealm" suffix)</li>
 * <li>simple-role-decoder</li>
 * <li>constant-permission-mapper</li>
 * </ul>
 * </p>
 *
 * @author Ondrej Kotek
 */
public class PropertyFileAuthzBasedDomain extends AbstractUserRolesCapableElement implements SecurityDomain {

    private static final Logger LOGGER = Logger.getLogger(PropertyFileAuthzBasedDomain.class);

    private final String principalDecoder;
    private final String authnRealm;
    private final String authzRealm;
    private final String aggregateRealm;

    private File tempFolder;

    private PropertyFileAuthzBasedDomain(Builder builder) {
        super(builder);
        this.principalDecoder = builder.principalDecoder;
        this.authnRealm = Objects.requireNonNull(builder.authnRealm, "Realm for authentication must not be null");
        this.authzRealm = this.name + "-authzRealm";
        this.aggregateRealm = this.name + "-aggregateRealm";
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
                authzRealm, asAbsolutePath(usersFile), asAbsolutePath(rolesFile)));

        // /subsystem=elytron/aggregate-realm=test-aggregateRealm:add(authentication-realm=test,authorization-realm=test-authzRealm)
        cli.sendLine(String.format(
                "/subsystem=elytron/aggregate-realm=%s:add(authentication-realm=%s,authorization-realm=%s)",
                aggregateRealm, authnRealm, authzRealm));

        // /subsystem=elytron/simple-role-decoder=test:add(attribute=groups)
        cli.sendLine(String.format("/subsystem=elytron/simple-role-decoder=%s:add(attribute=groups)", name));

        // /subsystem=elytron/constant-permission-mapper=test:add(permissions=[{class-name="org.wildfly.security.auth.permission.LoginPermission"}])
        cli.sendLine(String.format("/subsystem=elytron/constant-permission-mapper=%s:add(permissions=[{class-name=\"%s\"}])",
                name, LoginPermission.class.getName()));

        // /subsystem=elytron/security-domain=test:add(default-realm=test-aggregateRealm, permission-mapper=test,
        // principal-decoder=test,realms=[{role-decoder=test, realm=test-aggregateRealm}])
        StringBuilder command = new StringBuilder("/subsystem=elytron/security-domain=").append(name)
                .append(":add(default-realm=").append(aggregateRealm)
                .append(",permission-mapper=").append(name)
                .append(",realms=[{role-decoder=").append(name).append(",realm=").append(aggregateRealm).append("}]");
        if (principalDecoder != null) {
            command.append(",principal-decoder=").append(principalDecoder);
        }
        command.append(")");
        cli.sendLine(String.format(command.toString()));
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:remove()", name));
        cli.sendLine(String.format("/subsystem=elytron/constant-permission-mapper=%s:remove()", name));
        cli.sendLine(String.format("/subsystem=elytron/simple-role-decoder=%s:remove()", name));
        cli.sendLine(String.format("/subsystem=elytron/aggregate-realm=%s:remove()", aggregateRealm));
        cli.sendLine(String.format("/subsystem=elytron/properties-realm=%s:remove()", authzRealm));

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
        private String authnRealm;
        private String principalDecoder;

        private Builder() {
        }

        public Builder withAuthnRealm(String authnRealm) {
            this.authnRealm = authnRealm;
            return this;
        }

        public Builder withPrincipalDecoder(String principalDecoder) {
            this.principalDecoder = principalDecoder;
            return this;
        }

        public PropertyFileAuthzBasedDomain build() {
            return new PropertyFileAuthzBasedDomain(this);
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

    /**
     * Creates a temporary folder name with given name prefix.
     *
     * @param prefix folder name prefix
     * @return created folder
     */
    private static File createTemporaryFolder(String prefix) throws IOException {
        File file = File.createTempFile(prefix, "", null);
        LOGGER.debugv("Creating temporary folder {0}", file);
        file.delete();
        file.mkdir();
        return file;
    }
}
