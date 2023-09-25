/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import org.jboss.as.server.security.VirtualSecurityDomainCreationService;
import org.jboss.msc.service.Service;
import org.wildfly.security.auth.permission.LoginPermission;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.http.oidc.OidcSecurityRealm;

/**
 * Core {@link Service} handling virtual security domain creation.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class OidcVirtualSecurityDomainCreationService extends VirtualSecurityDomainCreationService {

    private static final String VIRTUAL_REALM = "virtual";

    @Override
    public SecurityDomain.Builder createVirtualSecurityDomainBuilder() {
        return SecurityDomain.builder()
                .addRealm(VIRTUAL_REALM, new OidcSecurityRealm()).build()
                .setDefaultRealmName(VIRTUAL_REALM)
                .setPermissionMapper((permissionMappable, roles) -> LoginPermission.getInstance());
    }
}
