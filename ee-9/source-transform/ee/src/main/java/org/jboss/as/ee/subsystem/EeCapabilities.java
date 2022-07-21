/*
 * Copyright 2019 Red Hat, Inc.
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

    public static final String LEGACY_JACC_CAPABILITY = "org.wildfly.legacy-security.jacc";

    public static final String ELYTRON_JACC_CAPABILITY = "org.wildfly.security.jacc-policy";

}
