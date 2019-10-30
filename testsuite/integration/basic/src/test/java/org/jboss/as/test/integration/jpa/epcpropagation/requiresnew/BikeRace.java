/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.jpa.epcpropagation.requiresnew;

import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
