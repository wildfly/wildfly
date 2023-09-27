/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

/**
 * Authorization forwarding (credential less forwarding) for security context propagation test. Variant which uses Entry bean
 * stateless and WhoAmI bean stateful.
 *
 * @author Josef Cacek
 */
public class AuthorizationForwardingSLSFTestCase extends AbstractAuthorizationForwardingTestCase {

    @Override
    protected boolean isEntryStateful() {
        return false;
    }

    @Override
    protected boolean isWhoAmIStateful() {
        return true;
    }

}
