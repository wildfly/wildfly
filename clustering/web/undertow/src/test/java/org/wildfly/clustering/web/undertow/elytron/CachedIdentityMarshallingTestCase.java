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
import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.cache.CachedIdentity;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Paul Ferraro
 */
public class CachedIdentityMarshallingTestCase {
    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<CachedIdentity> tester = factory.createTester(CachedIdentityMarshallingTestCase::assertEquals);
        Principal principal = new NamePrincipal("name");
        tester.accept(new CachedIdentity(HttpServletRequest.BASIC_AUTH, false, principal));
        tester.accept(new CachedIdentity(HttpServletRequest.CLIENT_CERT_AUTH, false, principal));
        tester.accept(new CachedIdentity(HttpServletRequest.DIGEST_AUTH, false, principal));
        tester.accept(new CachedIdentity(HttpServletRequest.FORM_AUTH, false, principal));
        tester.accept(new CachedIdentity("Programmatic", true, principal));
    }

    static void assertEquals(CachedIdentity auth1, CachedIdentity auth2) {
        Assertions.assertEquals(auth1.getMechanismName(), auth2.getMechanismName());
        Assertions.assertEquals(auth1.getName(), auth2.getName());
        Assertions.assertEquals(auth1.isProgrammatic(), auth2.isProgrammatic());
    }
}
