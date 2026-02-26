/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.manual.management;

import org.wildfly.test.distribution.validation.ProvisioningConsistencyBaseTest;

/**
 * Validates that provisioning using wildfly-maven-plugin and the channel manifest produced by
 * building the standard dist produces an installation consistent with what is produced by
 * the galleon-maven-plugin when it produced the standard dist.
 * <p>
 * The purpose of this test is to demonstrate consistency between the two provisioning methods,
 * thus supporting the concept that tests run against an installation provisioned one way are
 * meaningful for an installation provisioned the other way.
 */
public class ProvisioningConsistencyTestCase extends ProvisioningConsistencyBaseTest {

    public ProvisioningConsistencyTestCase() {
        super(System.getProperty("dist.output.dir"));
    }
}
