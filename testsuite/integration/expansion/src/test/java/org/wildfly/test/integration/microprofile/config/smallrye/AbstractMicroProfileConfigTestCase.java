/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye;

import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.PropertyPermission;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class AbstractMicroProfileConfigTestCase {

    /**
     * Creates the following permissions for config tests:
     * <ul>
     * <li>A read {@link PropertyPermission} for each name</li>
     * <li>A {@code getenv} permission for each name</li>
     * <li>A {@code getenv} permission for each name replacing any dots {@code .} with an underscore {@code _}</li>
     * <li>A {@code getenv} permission for each name converted to upper case</li>
     * <li>A {@code getenv} permission for each name converted to upper case replacing any dots {@code .}
     * with an underscore {@code _}
     * </li>
     * </ul>
     *
     * @param names the names to create the permissions for
     *
     * @return the set of permissions
     */
    protected static Permission[] createPermissions(final String... names) {
        final Collection<Permission> permissions = new ArrayList<>(names.length * 2);
        for (String name : names) {
            permissions.add(new PropertyPermission(name, "read"));
            permissions.add(new RuntimePermission("getenv." + name));
            permissions.add(new RuntimePermission("getenv." + name.replace('.', '_')));
            permissions.add(new RuntimePermission("getenv." + name.toUpperCase(Locale.ROOT)));
            permissions.add(new RuntimePermission("getenv." + name.replace('.', '_').toUpperCase(Locale.ROOT)));
        }
        return permissions.toArray(new Permission[0]);
    }
}
