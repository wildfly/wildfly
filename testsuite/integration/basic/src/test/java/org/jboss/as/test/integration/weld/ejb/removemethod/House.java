/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.removemethod;

import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Stuart Douglas
 */
@Stateful
@ApplicationScoped
public class House {

    @Remove
    public void remove() {

    }

}
