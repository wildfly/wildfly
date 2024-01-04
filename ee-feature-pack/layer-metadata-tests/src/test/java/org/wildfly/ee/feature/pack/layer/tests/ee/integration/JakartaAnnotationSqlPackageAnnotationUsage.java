/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.ee.integration;

import jakarta.annotation.sql.DataSourceDefinition;

@DataSourceDefinition(name = "x", className = "X")
public class JakartaAnnotationSqlPackageAnnotationUsage {
}
