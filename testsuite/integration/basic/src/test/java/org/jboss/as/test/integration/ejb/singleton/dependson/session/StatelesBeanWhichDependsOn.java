/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.dependson.session;

import jakarta.ejb.DependsOn;
import jakarta.ejb.Remote;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateless;

/**
 * @author baranowb
 */
@Stateless
@Remote(Trigger.class)
@DependsOn("CallCounterProxy")
public class StatelesBeanWhichDependsOn extends BeanBase {
    // This is required to trigger purge - circumvents WFLY-817
    @Remove
    public void trigger() {
        super.logger.trace("Session.trigger");
        super.counter.setMessage();
    }
}
