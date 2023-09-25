/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.localview;

import jakarta.ejb.Stateless;

/**
 * EJB with a single implicit local interface
 *
 * @author Stuart Douglas
 */
@Stateless
public class SimpleImplicitLocalInterfaceBean implements ImplicitLocalInterface {
    @Override
    public void message() {
    }
}
