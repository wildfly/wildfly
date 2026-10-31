/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.weld.builtinBeans;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.wildfly.security.auth.permission.ChangeRoleMapperPermission;
import org.wildfly.security.permission.ElytronPermission;


/**
 * See <a href="https://issues.jboss.org/browse/WFLY-11587">WFLY-11587</a>.
 */
@ExtendWith(ArquillianExtension.class)
public class InjectPrincipalTestCase {

    public static final String ANONYMOUS_PRINCIPAL = "anonymous";
    public static final String NON_ANONYMOUS_PRINCIPAL = "non-anonymous";

    @Deployment
    public static Archive<?> createTestArchive() {
        return ShrinkWrap.create(WebArchive.class).addPackage(InjectPrincipalTestCase.class.getPackage())
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(InjectPrincipalTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(createPermissionsXmlAsset(
                        new ElytronPermission("getIdentity"),
                        new ElytronPermission("createAdHocIdentity"),
                        new ChangeRoleMapperPermission("ejb")
                        ), "permissions.xml");
    }

    @Inject
    BeanWithInjectedPrincipal beanA;

    @Inject
    BeanWithPrincipalFromEJBContext beanB;

    @Inject
    CallerWithIdentity callerWithIdentity;

    @Test
    public void testAnonymousPrincipalInjected() {
        try {
            Assertions.assertEquals(ANONYMOUS_PRINCIPAL, beanA.getPrincipalName());
            Assertions.assertEquals(ANONYMOUS_PRINCIPAL, beanB.getPrincipalName());
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void testNonAnonymousPrincipalInjected() throws Exception {
        try {
            // Assert.assertEquals(NON_ANONYMOUS_PRINCIPAL, callerWithIdentity.getCallerPrincipalInjected()); TODO see issue WFLY-12538
            Assertions.assertEquals(NON_ANONYMOUS_PRINCIPAL, callerWithIdentity.getCallerPrincipalFromEJBContext());
            Assertions.assertEquals(NON_ANONYMOUS_PRINCIPAL, callerWithIdentity.getCallerPrincipalFromEJBContextSecuredBean());
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }
}
