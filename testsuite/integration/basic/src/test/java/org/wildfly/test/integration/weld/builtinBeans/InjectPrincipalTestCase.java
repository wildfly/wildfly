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
package org.wildfly.test.integration.weld.builtinBeans;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.permission.ChangeRoleMapperPermission;
import org.wildfly.security.permission.ElytronPermission;


/**
 * See <a href="https://issues.jboss.org/browse/WFLY-11587">WFLY-11587</a>.
 */
@RunWith(Arquillian.class)
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

    @Test
    public void testAnonymousPrincipalInjected(BeanWithInjectedPrincipal beanA, BeanWithPrincipalFromEJBContext beanB) {
        try {
            Assert.assertEquals(ANONYMOUS_PRINCIPAL, beanA.getPrincipalName());
            Assert.assertEquals(ANONYMOUS_PRINCIPAL, beanB.getPrincipalName());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNonAnonymousPrincipalInjected(CallerWithIdentity callerWithIdentity) throws Exception {
        try {
            // Assert.assertEquals(NON_ANONYMOUS_PRINCIPAL, callerWithIdentity.getCallerPrincipalInjected()); TODO see issue WFLY-12538
            Assert.assertEquals(NON_ANONYMOUS_PRINCIPAL, callerWithIdentity.getCallerPrincipalFromEJBContext());
            Assert.assertEquals(NON_ANONYMOUS_PRINCIPAL, callerWithIdentity.getCallerPrincipalFromEJBContextSecuredBean());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
