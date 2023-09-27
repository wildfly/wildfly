/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.rolemappers;

import java.util.List;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.UndertowDomainMapper;

/**
 *
 * @author olukas
 */
class RoleMapperSetupUtils {

    static final String PROPERTIES_REALM_NAME = "RoleMapperPropertiesRealm";

    private RoleMapperSetupUtils() {
    }

    static void addSecurityDomainWithRoleMapper(List<ConfigurableElement> elements, String roleMapperName) {
        elements.add(SimpleSecurityDomain.builder().withName(roleMapperName)
                .withRoleMapper(roleMapperName)
                .withDefaultRealm(PROPERTIES_REALM_NAME)
                .withPermissionMapper("default-permission-mapper")
                .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                        .withRealm(PROPERTIES_REALM_NAME)
                        .withRoleDecoder("groups-to-roles")
                        .build())
                .build());
        elements.add(UndertowDomainMapper.builder()
                .withName(roleMapperName)
                .withApplicationDomains(roleMapperName)
                .build());
    }

}
