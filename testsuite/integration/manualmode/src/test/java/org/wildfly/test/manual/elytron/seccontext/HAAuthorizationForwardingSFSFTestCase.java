/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

/**
 * Tests authorization forwarding within a cluster. Variant which uses both Entry and WhoAmI beans stateful. See superclass for
 * details.
 *
 * @author Josef Cacek
 */
public class HAAuthorizationForwardingSFSFTestCase extends AbstractHAAuthorizationForwardingTestCase {

    @Override
    protected boolean isEntryStateful() {
        return true;
    }

    @Override
    protected boolean isWhoAmIStateful() {
        return true;
    }

}
