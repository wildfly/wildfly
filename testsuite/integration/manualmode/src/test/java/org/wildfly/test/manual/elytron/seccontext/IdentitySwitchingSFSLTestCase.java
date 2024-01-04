/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

/**
 * Identity switching for security context propagation test. Variant which uses Entry bean stateful and WhoAmI bean stateless.
 *
 * @author Josef Cacek
 */
public class IdentitySwitchingSFSLTestCase extends AbstractIdentitySwitchingTestCase {

    @Override
    protected boolean isEntryStateful() {
        return true;
    }

    @Override
    protected boolean isWhoAmIStateful() {
        return false;
    }

}
