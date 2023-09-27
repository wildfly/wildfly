/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.capacitypolicies;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.junit.runner.RunWith;

/**
 * Integration test for Jakarta Connectors capacity policies JBJCA-986 using xa-datasource
 *
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(XADatasourceCapacityPoliciesTestCase.DatasourceServerSetupTask.class)
public class XADatasourceCapacityPoliciesTestCase extends AbstractDatasourceCapacityPoliciesTestCase {

    public XADatasourceCapacityPoliciesTestCase() {
        // test xa datasource
        super(true);
    }

    static class DatasourceServerSetupTask extends AbstractDatasourceCapacityPoliciesServerSetup {

        DatasourceServerSetupTask() {
            // add xa datasource
            super(true);
        }
    }

}
