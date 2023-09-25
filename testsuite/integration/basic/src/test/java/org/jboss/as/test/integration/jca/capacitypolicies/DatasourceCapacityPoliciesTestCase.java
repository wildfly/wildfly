/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.capacitypolicies;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.junit.runner.RunWith;

/**
 * Integration test for Jakarta Connectors capacity policies JBJCA-986 using datasource
 *
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(DatasourceCapacityPoliciesTestCase.DatasourceServerSetupTask.class)
public class DatasourceCapacityPoliciesTestCase extends AbstractDatasourceCapacityPoliciesTestCase {

    public DatasourceCapacityPoliciesTestCase() {
        // test non-xa datasource
        super(false);
    }

    static class DatasourceServerSetupTask extends AbstractDatasourceCapacityPoliciesTestCase.AbstractDatasourceCapacityPoliciesServerSetup {

        DatasourceServerSetupTask() {
            // add non-xa datasource
            super(false);
        }
    }

}
