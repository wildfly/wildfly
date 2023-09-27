/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

/**
 * Identity switching for security context propagation test. Variant which uses both Entry and WhoAmI beans stateful.
 *
 * @author Josef Cacek
 */
public class IdentitySwitchingSFSFTestCase extends AbstractIdentitySwitchingTestCase {

    @Override
    protected boolean isEntryStateful() {
        return true;
    }

    @Override
    protected boolean isWhoAmIStateful() {
        return true;
    }

}
