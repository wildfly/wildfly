/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.oidc.client.multitenancy;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.wildfly.security.http.oidc.OidcClientConfiguration;
import org.wildfly.security.http.oidc.OidcClientConfigurationBuilder;
import org.wildfly.security.http.oidc.OidcClientConfigurationResolver;
import org.wildfly.security.http.oidc.OidcHttpFacade;

/**
 * Multi-tenant resolver.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class MultiTenantResolverProviderUrl implements OidcClientConfigurationResolver {

    private final Map<String, OidcClientConfiguration> cache = new ConcurrentHashMap<>();

    @Override
    public OidcClientConfiguration resolve(OidcHttpFacade.Request request) {
        String path = request.getURI();
        int multitenantIndex = path.indexOf(OidcWithMultiTenancyTest.MULTI_TENANCY_PROVIDER_URL_APP + "/");
        if (multitenantIndex == -1) {
            throw new IllegalStateException("Cannot resolve the realm to use from the request");
        }

        String tenant = path.substring(multitenantIndex).split("/")[1];
        if (tenant.contains("?")) {
            tenant = tenant.split("\\?")[0];
        }

        OidcClientConfiguration clientConfiguration = cache.get(tenant);
        if (clientConfiguration == null) {
            // not found in the simple cache, try to load it instead
            InputStream is = getClass().getResourceAsStream(OidcWithMultiTenancyTest.MULTI_TENANCY_PROVIDER_URL_APP + "-" + tenant + ".json");
            if (is == null) {
                throw new IllegalStateException("Cannot find realm-specific configuration file");
            }
            clientConfiguration = OidcClientConfigurationBuilder.build(is);
            cache.put(tenant, clientConfiguration);
        }
        return clientConfiguration;
    }

}
