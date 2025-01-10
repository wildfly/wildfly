/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.securityapi;

import java.security.Principal;
import java.util.function.Function;

/**
 * A simple principal transformer that converts into a {@link TestCustomPrincipal custom principal}.
 *
 * @author <a href="mailto:jrodri@redhat.com">Jessica Rodriguez</a>
 */
public class TestCustomPrincipalTransformer implements Function<Principal, Principal> {

    @Override
    public Principal apply(Principal principal) {
        return new TestCustomPrincipal(principal);
    }
}
