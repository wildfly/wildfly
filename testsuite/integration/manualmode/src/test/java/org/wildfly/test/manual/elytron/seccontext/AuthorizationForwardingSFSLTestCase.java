/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

/**
 * Authorization forwarding (credential less forwarding) for security context propagation test. Variant which uses Entry bean
 * stateful and WhoAmI bean stateless.
 *
 * @author Josef Cacek
 */
public class AuthorizationForwardingSFSLTestCase extends AbstractAuthorizationForwardingTestCase {

    @Override
    protected boolean isEntryStateful() {
        return true;
    }

    @Override
    protected boolean isWhoAmIStateful() {
        return false;
    }

}
