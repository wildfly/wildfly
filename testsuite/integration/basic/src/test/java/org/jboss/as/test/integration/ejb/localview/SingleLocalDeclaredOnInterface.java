/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.localview;

import java.io.Serializable;
import jakarta.ejb.Stateless;

/**
 * Bean with a single local interface, declared on the interface
 * @author Stuart Douglas
 */
@Stateless
public class SingleLocalDeclaredOnInterface implements NotViewInterface, LocalInterface, Serializable{
    @Override
    public void localOperation() {
    }

    @Override
    public void doOtherStuff() {
    }
}
