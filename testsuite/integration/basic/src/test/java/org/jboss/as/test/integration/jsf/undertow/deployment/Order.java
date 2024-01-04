/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jsf.undertow.deployment;

public abstract class Order {

    public abstract PersonalDetails getPersonalDetails(final PersonalID id);
}
