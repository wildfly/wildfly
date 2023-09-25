/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api;

/**
 * User: jpai
 */
public interface EmployeeManager {

    AliasedEmployee addNickNames(final Employee employee, final String... nickNames);
}
