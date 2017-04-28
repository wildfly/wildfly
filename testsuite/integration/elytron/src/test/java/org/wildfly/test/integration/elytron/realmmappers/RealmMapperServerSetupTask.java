/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.test.integration.elytron.realmmappers;

import java.util.ArrayList;
import java.util.List;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.PropertiesRealm;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.UndertowDomainMapper;
import org.wildfly.test.security.servlets.SecuredPrincipalPrintingServlet;

/**
 *
 * @author olukas
 */
public class RealmMapperServerSetupTask extends AbstractElytronSetupTask {

    public static final String USER_IN_DEFAULT_REALM = "userInDefaultRealm";
    public static final String USER_IN_REALM1 = "userInRealm1";
    public static final String USER_IN_REALM2 = "userInRealm2";
    public static final String USER_IN_REALM3 = "userInRealm3";

    public static final String USER_IN_DEFAULT_REALM_MAPPED = "userInDefaultRealm&mapped";
    public static final String USER_IN_DEFAULT_REALM_MAPPED2 = "userInDefaultRealm@mapped2";

    public static final String USER_IN_REALM1_WITH_REALM = "userInRealm1@realm1";
    public static final String USER_IN_REALM1_WITH_REALM_AND_SUFFIX = "userInRealm1@realm1&suffix";
    public static final String USER_IN_REALM1_WITH_INFIX_AND_SUFFIX = "userInRealm1@infix&suffix";
    public static final String USER_IN_REALM1_WITH_REALM_AND_DIFFERENT_SUFFIX = "userInRealm1@realm1&different";

    public static final String USER_IN_REALM2_WITH_REALM1 = "userInRealm2&realm1";

    public static final String CORRECT_PASSWORD = "password";

    public static final String SECURITY_DOMAIN_NAME = "constantRealmMapperSecurityDomain";
    public static final String SECURITY_DOMAIN_REFERENCE = "usedSecurityDomain";

    public static final String DEFAULT_REALM = "defaultRealm";
    public static final String REALM1 = "realm1";
    public static final String REALM2 = "realm2";
    public static final String REALM3 = "realm3";

    @Override
    protected ConfigurableElement[] getConfigurableElements() {
        List<ConfigurableElement> elements = new ArrayList<>();
        elements.add(PropertiesRealm.builder().withName(DEFAULT_REALM)
                .withUser(USER_IN_DEFAULT_REALM, CORRECT_PASSWORD, SecuredPrincipalPrintingServlet.ALLOWED_ROLE)
                .withUser(USER_IN_DEFAULT_REALM_MAPPED, CORRECT_PASSWORD, SecuredPrincipalPrintingServlet.ALLOWED_ROLE)
                .withUser(USER_IN_DEFAULT_REALM_MAPPED2, CORRECT_PASSWORD, SecuredPrincipalPrintingServlet.ALLOWED_ROLE)
                .build());
        elements.add(PropertiesRealm.builder().withName(REALM1)
                .withUser(USER_IN_REALM1, CORRECT_PASSWORD, SecuredPrincipalPrintingServlet.ALLOWED_ROLE)
                .withUser(USER_IN_REALM1_WITH_REALM, CORRECT_PASSWORD, SecuredPrincipalPrintingServlet.ALLOWED_ROLE)
                .withUser(USER_IN_REALM1_WITH_REALM_AND_SUFFIX, CORRECT_PASSWORD, SecuredPrincipalPrintingServlet.ALLOWED_ROLE)
                .withUser(USER_IN_REALM1_WITH_INFIX_AND_SUFFIX, CORRECT_PASSWORD, SecuredPrincipalPrintingServlet.ALLOWED_ROLE)
                .withUser(USER_IN_REALM1_WITH_REALM_AND_DIFFERENT_SUFFIX, CORRECT_PASSWORD, SecuredPrincipalPrintingServlet.ALLOWED_ROLE)
                .build());
        elements.add(PropertiesRealm.builder().withName(REALM2)
                .withUser(USER_IN_REALM2, CORRECT_PASSWORD, SecuredPrincipalPrintingServlet.ALLOWED_ROLE)
                .withUser(USER_IN_REALM2_WITH_REALM1, CORRECT_PASSWORD, SecuredPrincipalPrintingServlet.ALLOWED_ROLE)
                .build());
        elements.add(PropertiesRealm.builder().withName(REALM3)
                .withUser(USER_IN_REALM3, CORRECT_PASSWORD, SecuredPrincipalPrintingServlet.ALLOWED_ROLE).build());

        SimpleSecurityDomain.SecurityDomainRealm sdDefaultRealm = SimpleSecurityDomain.SecurityDomainRealm.builder().withRoleDecoder("groups-to-roles")
                .withRealm(DEFAULT_REALM).build();
        SimpleSecurityDomain.SecurityDomainRealm sdRealm1 = SimpleSecurityDomain.SecurityDomainRealm.builder().withRoleDecoder("groups-to-roles")
                .withRealm(REALM1).build();
        SimpleSecurityDomain.SecurityDomainRealm sdRealm2 = SimpleSecurityDomain.SecurityDomainRealm.builder().withRoleDecoder("groups-to-roles")
                .withRealm(REALM2).build();
        SimpleSecurityDomain.SecurityDomainRealm sdRealm3 = SimpleSecurityDomain.SecurityDomainRealm.builder().withRoleDecoder("groups-to-roles")
                .withRealm(REALM3).build();
        elements.add(SimpleSecurityDomain.builder().withName(SECURITY_DOMAIN_NAME).withDefaultRealm(DEFAULT_REALM)
                .withPermissionMapper("default-permission-mapper").withRealms(sdDefaultRealm, sdRealm1, sdRealm2, sdRealm3)
                .build());

        elements.add(UndertowDomainMapper.builder().withName(SECURITY_DOMAIN_NAME)
                .withApplicationDomains(SECURITY_DOMAIN_REFERENCE)
                .build());

        return elements.toArray(new ConfigurableElement[elements.size()]);
    }

}
