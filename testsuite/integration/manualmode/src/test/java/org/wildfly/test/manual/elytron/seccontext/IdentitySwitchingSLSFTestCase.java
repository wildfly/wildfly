/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

/**
 * Identity switching for security context propagation test. Variant which uses Entry bean stateless and WhoAmI bean stateful.
 *
 * @author Josef Cacek
 */
public class IdentitySwitchingSLSFTestCase extends AbstractIdentitySwitchingTestCase {

    @Override
    protected boolean isEntryStateful() {
        return false;
    }

    @Override
    protected boolean isWhoAmIStateful() {
        return true;
    }

}
