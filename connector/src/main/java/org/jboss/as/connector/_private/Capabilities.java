/*
 * Copyright 2017 Red Hat, Inc.
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

package org.jboss.as.connector._private;

import javax.sql.DataSource;

import org.jboss.as.controller.capability.RuntimeCapability;

/**
 * Capabilities for the connector subsystems.
 * <p>
 * <strong>This is not to be used outside of the various connector subsystems.</strong>
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface Capabilities {
    /**
     * The name for the data-source capability
     */
    String DATA_SOURCE_CAPABILITY_NAME = "org.wildfly.data-source";

    /**
     * The name of the authentication-context capability provided by Elytron.
     */
    String AUTHENTICATION_CONTEXT_CAPABILITY = "org.wildfly.security.authentication-context";

    String ELYTRON_SECURITY_DOMAIN_CAPABILITY = "org.wildfly.security.security-domain";

    /**
     * The data-source capability
     */
    RuntimeCapability<Void> DATA_SOURCE_CAPABILITY = RuntimeCapability.Builder.of(DATA_SOURCE_CAPABILITY_NAME, true, DataSource.class)
            .build();
}
