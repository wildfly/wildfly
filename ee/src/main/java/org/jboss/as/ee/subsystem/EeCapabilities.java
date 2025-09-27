/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.services.path.PathManager;

/**
 * The capabilities provided by and required by this subsystem.
 *
 * @author Yeray Borges
 */
public final class EeCapabilities {
    private static final String CAPABILITY_BASE = "org.wildfly.ee.";

    public static final String EE_GLOBAL_DIRECTORY_CAPABILITY_NAME = CAPABILITY_BASE + "global-directory";

    public static final RuntimeCapability<Void> EE_GLOBAL_DIRECTORY_CAPABILITY = RuntimeCapability
            .Builder.of(EE_GLOBAL_DIRECTORY_CAPABILITY_NAME, true, GlobalDirectoryService.class)
            .addRequirements(PathManager.SERVICE_DESCRIPTOR)
            .build();

    /*
     * Prior to Jakarta EE 11 the Policy was an instance of java.security.Policy, within the Elytron subsystem
     * we would make use of an MSC service to handle the global registration. Any deployments that were utilising JACC
     * would need to depend on the "org.wildfly.security.jacc-policy" capability to ensure registration was complete
     * before processing the deployment.
     *
     * The Elytron subsystem now also supports an immediate registration which is indicated by the
     * "org.wildfly.security.jakarta-authorization" capability, if this is registered it means Jakarta Authorization
     * has already been registered.
     */

    public static final String ELYTRON_JACC_CAPABILITY = "org.wildfly.security.jacc-policy";
    public static final String ELYTRON_JAKARTA_AUTHORIZATION = "org.wildfly.security.jakarta-authorization";

}
