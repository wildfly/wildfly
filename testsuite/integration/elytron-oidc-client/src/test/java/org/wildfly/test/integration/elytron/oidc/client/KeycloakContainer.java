/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.oidc.client;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.jboss.as.test.config.ContainerConfig;

/**
 * KeycloakContainer for testing.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class KeycloakContainer extends GenericContainer<KeycloakContainer> {
    public static final String ADMIN_USER = "admin";
    public static final String ADMIN_PASSWORD = "admin";
    private static final int PORT_HTTP = 8080;
    private static final int PORT_HTTPS = 8443;

    private boolean useHttps;

    public KeycloakContainer() {
        this(false);
    }

    public KeycloakContainer(final boolean useHttps) {
        super(ContainerConfig.getKeycloakImage());
        this.useHttps = useHttps;
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
            waitingFor(Wait.forLogMessage(".*Keycloak.*started.*", 1));
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
        return ContainerConfig.getKeycloakImage().contains("rh-sso");
    }
}