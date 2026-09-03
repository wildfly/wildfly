/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.lra.participant.jwt;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.auth.LoginConfig;

/**
 * JAX-RS activator for JWT LRA participant application.
 * The @LoginConfig annotation enables MicroProfile JWT authentication,
 * which activates the JsonWebToken CDI producer.
 *
 */
@ApplicationPath("/")
@LoginConfig(authMethod="MP-JWT", realmName="MP JWT Realm")
public class JaxRsActivator extends Application {
}
