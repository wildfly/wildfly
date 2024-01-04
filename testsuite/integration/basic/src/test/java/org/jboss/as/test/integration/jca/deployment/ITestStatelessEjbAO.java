/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.deployment;


import jakarta.ejb.Local;

/**
 * This is interface for the stateless ejb.
 */
@Local
public interface ITestStatelessEjbAO {
    boolean validateConnectorResource(String jndiName);
}
