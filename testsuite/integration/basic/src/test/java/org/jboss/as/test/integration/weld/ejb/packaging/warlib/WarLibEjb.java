/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.packaging.warlib;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * @author Stuart Douglas
 */
@Stateless
public class WarLibEjb implements WarLibInterface {

    private final OtherEjb ejb;

    public WarLibEjb() {
        ejb = null;
    }

    @Inject
    public WarLibEjb(final OtherEjb ejb) {
        this.ejb = ejb;
    }

    public OtherEjb getEjb() {
        return ejb;
    }
}
