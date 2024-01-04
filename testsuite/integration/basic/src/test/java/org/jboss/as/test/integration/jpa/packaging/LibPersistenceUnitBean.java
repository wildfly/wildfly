/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.packaging;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

/**
 * stateful session bean
 *
 * @author Stuart Douglas
 */
@Stateless
public class LibPersistenceUnitBean {

    @PersistenceUnit(unitName = "../lib/lib.jar#mainPu")
    EntityManagerFactory entityManagerFactory;

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }
}
