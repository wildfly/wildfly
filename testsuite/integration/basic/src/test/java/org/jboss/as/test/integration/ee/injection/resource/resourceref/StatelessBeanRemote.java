/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.resourceref;

/**
 * @author Jaikiran Pai
 */
public interface StatelessBeanRemote {

    boolean isEJBContextAvailableThroughResourceEnvRef();

    boolean isUserTransactionAvailableThroughResourceEnvRef();

    boolean isOtherResourceAvailableThroughResourceEnvRef();
}
