/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wildfly.test.integration.elytron.oidc.client;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

/**
 * KeycloakContainer for testing.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class KeycloakContainer extends GenericContainer<KeycloakContainer> {
    public static final String ADMIN_USER = "admin";
    public static final String ADMIN_PASSWORD = "admin";

    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:19.0.1";
    private static final String SSO_IMAGE = System.getProperty("testsuite.integration.oidc.rhsso.image",KEYCLOAK_IMAGE);
    private static final int STARTUP_ATTEMPTS = Integer.parseInt(
            System.getProperty("testsuite.integration.oidc.container.startup.attempts", "5"));
    private static final Duration ATTEMPT_DURATION = Duration.parse(
            System.getProperty("testsuite.integration.oidc.container.attempt.duration", "PT30S"));
    private static final int PORT_HTTP = 8080;
    private static final int PORT_HTTPS = 8443;

    private boolean useHttps;

    public KeycloakContainer() {
        this(false);
    }

    public KeycloakContainer(final boolean useHttps) {
        super(SSO_IMAGE);
        this.useHttps = useHttps;
        this.withStartupTimeout(ATTEMPT_DURATION);
        this.setStartupAttempts(STARTUP_ATTEMPTS);
    }

    @Override
    protected void configure() {
        withExposedPorts(PORT_HTTP, PORT_HTTPS);
        withEnv("KEYCLOAK_ADMIN", ADMIN_USER);
        withEnv("KEYCLOAK_ADMIN_PASSWORD", ADMIN_PASSWORD);
        withEnv("SSO_ADMIN_USERNAME", ADMIN_USER);
        withEnv("SSO_ADMIN_PASSWORD", ADMIN_PASSWORD);
        if (isUsedRHSSOImage()) {
            waitingFor(Wait.forHttp("/auth").forPort(PORT_HTTP));
        }else{
            waitingFor(Wait.forHttp("/").forPort(PORT_HTTP));
            withCommand("start-dev");
        }
    }

    public String getAuthServerUrl() {
        Integer port = useHttps ? getMappedPort(PORT_HTTPS) : getMappedPort(PORT_HTTP);
        String authServerUrl = String.format("http://%s:%s", getContainerIpAddress(), port);
        if(isUsedRHSSOImage()){
            authServerUrl += "/auth";
        }
        return authServerUrl;
    }

    private boolean isUsedRHSSOImage(){
        return SSO_IMAGE.contains("rh-sso");
    }
}