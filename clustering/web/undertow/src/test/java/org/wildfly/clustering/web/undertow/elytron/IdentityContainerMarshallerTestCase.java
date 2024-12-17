/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.elytron;

import java.security.Principal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;
import org.wildfly.elytron.web.undertow.server.servlet.ServletSecurityContextImpl.IdentityContainer;
import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.cache.CachedIdentity;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Paul Ferraro
 */
public class IdentityContainerMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<IdentityContainer> tester = factory.createTester(IdentityContainerMarshallerTestCase::assertEquals);
        Principal principal = new NamePrincipal("name");
        tester.accept(new IdentityContainer(new CachedIdentity(HttpServletRequest.BASIC_AUTH, false, principal), "foo"));
        tester.accept(new IdentityContainer(new CachedIdentity(HttpServletRequest.BASIC_AUTH, false, principal), null));
    }

    static void assertEquals(IdentityContainer container1, IdentityContainer container2) {
        CachedIdentityMarshallingTestCase.assertEquals(container1.getSecurityIdentity(), container2.getSecurityIdentity());
        Assertions.assertEquals(container1.getAuthType(), container2.getAuthType());
    }
}
