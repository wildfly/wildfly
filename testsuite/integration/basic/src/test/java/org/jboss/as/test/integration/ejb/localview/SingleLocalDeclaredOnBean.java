/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.localview;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

/**
 * This bean class has a single local interface, declared on the bean class
 * @author Stuart Douglas
 */
@Stateless
@Local({OtherInterface.class})
public class SingleLocalDeclaredOnBean  implements OtherInterface, LocalInterface {
    @Override
    public void localOperation() {
    }

    @Override
    public void moreStuff() {
    }
}
