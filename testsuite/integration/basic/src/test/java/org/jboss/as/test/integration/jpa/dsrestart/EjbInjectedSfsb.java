/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.dsrestart;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Stateless
public class EjbInjectedSfsb {

    @EJB
    JpaInjectedSfsb jpaEjb;

    public JpaInjectedSfsb getJpaEjb() {
        return jpaEjb;
    }
}
