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
public class OrganisationBean {

    @PersistenceUnit(unitName = "mainPu")
    private EntityManagerFactory entityManagerFactory;

    // AS7-2275 requires each PU reference to specify a persistence unit name, if there are
    // multiple persistence unit definitions.
    // as a workaround, specified the pu name
    @PersistenceUnit(unitName = "mainPu")
    private EntityManagerFactory defaultEntityManagerFactory;

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public EntityManagerFactory getDefaultEntityManagerFactory() {
        return defaultEntityManagerFactory;
    }
}
