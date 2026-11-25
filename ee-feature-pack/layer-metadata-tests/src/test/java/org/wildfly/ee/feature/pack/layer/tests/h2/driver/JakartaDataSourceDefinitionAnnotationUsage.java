/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.h2.driver;

import jakarta.annotation.sql.DataSourceDefinition;

@DataSourceDefinition(name = "x", className = "org.h2.jdbcx.JdbcDataSource")
public class JakartaDataSourceDefinitionAnnotationUsage {
}
