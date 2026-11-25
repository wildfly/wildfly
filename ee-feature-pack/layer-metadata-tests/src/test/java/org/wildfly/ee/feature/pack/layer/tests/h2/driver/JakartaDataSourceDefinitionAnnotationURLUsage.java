/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.h2.driver;

import jakarta.annotation.sql.DataSourceDefinition;

@DataSourceDefinition(name = "x", className = "org.example.MyClass", url = "jdbc:h2:some_content")
public class JakartaDataSourceDefinitionAnnotationURLUsage {
}
