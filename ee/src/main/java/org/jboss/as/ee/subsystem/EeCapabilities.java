/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.capability.RuntimeCapability;

/**
 * The capabilities provided by and required by this subsystem.
 *
 * @author Yeray Borges
 */
public final class EeCapabilities {
    private static final String CAPABILITY_BASE = "org.wildfly.ee.";

    static final String PATH_MANAGER_CAPABILITY = "org.wildfly.management.path-manager";

    public static final String EE_GLOBAL_DIRECTORY_CAPABILITY_NAME = CAPABILITY_BASE + "global-directory";

    public static final RuntimeCapability<Void> EE_GLOBAL_DIRECTORY_CAPABILITY = RuntimeCapability
            .Builder.of(EE_GLOBAL_DIRECTORY_CAPABILITY_NAME, true, GlobalDirectoryService.class)
            .addRequirements(PATH_MANAGER_CAPABILITY)
            .build();

    public static final String ELYTRON_JACC_CAPABILITY = "org.wildfly.security.jacc-policy";

}
