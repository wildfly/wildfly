/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.datasources;

import javax.sql.CommonDataSource;

public class JavaxSqlUsage {
    CommonDataSource commonDataSource;
    // Usage from the method doesn't trigger the rule. It seems to need to be a field
    //public void useCommonDatasource() {
    //    CommonDataSource commonDataSource = null;
    //}
}
