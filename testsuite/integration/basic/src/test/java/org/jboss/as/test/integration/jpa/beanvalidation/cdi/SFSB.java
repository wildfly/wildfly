/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.beanvalidation.cdi;

import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * A stateful session bean.
 *
 * @author Farah Juma
 */
@Stateful
public class SFSB {
    @PersistenceContext(unitName = "mypc")
    EntityManager em;

    public void createReservation(int numberOfPeople, String lastName) {
        Reservation reservation = new Reservation(numberOfPeople, lastName);
        em.persist(reservation);
        em.flush();
    }

}
