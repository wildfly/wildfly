/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.classloading;

import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
@ServerSetup(DataSourceTcclDatasourceClassTestCase.Setup.class)
public class DataSourceTcclDatasourceClassTestCase extends AbstractDataSourceClassloadingTestCase {

    public static class Setup extends AbstractDataSourceClassloadingTestCase.Setup {

        public Setup() {
            super("driver-datasource-class-name", "org.jboss.as.test.integration.jca.classloading.ClassloadingDataSource");
        }
    }
}
