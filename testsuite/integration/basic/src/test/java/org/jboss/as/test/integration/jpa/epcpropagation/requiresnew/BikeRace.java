/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.epcpropagation.requiresnew;

import java.util.List;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author Stuart Douglas
 */
@Stateless
public class BikeRace {

    @PersistenceContext
    private EntityManager entityManager;

    @EJB
    private BikeShop bikeShop;

    public List<Motorbike> allMotorBikes() {
        return entityManager.createQuery("Select m from Motorbike m").getResultList();
    }

    /**
     * As this em is enrolled in the transaction, if BikeShop executed in the context of this transaction
     * it would throw an exception, as as PC has already been bound to the current transaction.
     *
     * @return
     */
    public Motorbike createNewBike(int id, String name) {
        return bikeShop.createMotorBike(id, name);
    }

    public Motorbike find(int id) {
        return bikeShop.find(id);
    }

    public boolean contains(Object object) {
        return entityManager.contains(object);
    }

}
