/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.dependencies.ear;

/**
 * A remote interface for basic EJB tests.
 *
 * @author Josef Cacek
 */
public interface LogAccess {

    String getLog();

}