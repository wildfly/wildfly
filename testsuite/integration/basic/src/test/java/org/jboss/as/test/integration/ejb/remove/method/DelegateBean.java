/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remove.method;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DelegateBean implements Delegate {
    // Instance Members

    @EJB
    private RemoveStatefulLocal stateful;

    @EJB
    private RemoveStatelessLocal stateless;

    // Required Implementations

    public String invokeStatefulRemove() {
        return stateful.remove();
    }

    public String invokeStatelessRemove() {
        return stateless.remove();
    }

}
