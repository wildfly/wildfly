/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

/**
 * Authentication forwarding (credential forwarding) for security context propagation test. Variant which uses both Entry and
 * WhoAmI beans stateless.
 *
 * @author Josef Cacek
 */
public class AuthenticationForwardingSLSLTestCase extends AbstractAuthenticationForwardingTestCase {

    @Override
    protected boolean isEntryStateful() {
        return false;
    }

    @Override
    protected boolean isWhoAmIStateful() {
        return false;
    }

}
