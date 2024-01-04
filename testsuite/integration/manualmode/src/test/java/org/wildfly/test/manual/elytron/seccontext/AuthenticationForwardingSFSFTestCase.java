/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

/**
 * Authentication forwarding (credential forwarding) for security context propagation test. Variant which uses both Entry and
 * WhoAmI beans stateful.
 *
 * @author Josef Cacek
 */
public class AuthenticationForwardingSFSFTestCase extends AbstractAuthenticationForwardingTestCase {

    @Override
    protected boolean isEntryStateful() {
        return true;
    }

    @Override
    protected boolean isWhoAmIStateful() {
        return true;
    }

}
