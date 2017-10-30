/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.test.compat.jpa.eclipselink.wildfly8954;

import static java.lang.String.format;

import java.util.concurrent.atomic.AtomicInteger;

import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.as.test.compat.jpa.eclipselink.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An ejb whose pupose is to modify a value on an existing entity and fire out an event that this action has been done.
 */
@LocalBean
@Stateful
public class EjbThatModifiesEntityAndFiresEventLocalFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(EjbThatModifiesEntityAndFiresEventLocalFacade.class);

    /**
     * Each time we modify an address we will build an incrementing address number. This will make it very obvious if we are
     * dealing with a stale value or not.
     */
    private static final AtomicInteger CURRENT_ADDRESS_MODIFICATION_NUMBER = new AtomicInteger(0);

    @PersistenceContext(unitName = "hibernate3_pc")
    EntityManager em;

    @Inject
    Event<SomeEntityChangeEvent> someEntityChangeEvent;

    /**
     * Opens a new jta transaction, modifies an entity by increasing its address and finally fires an event.
     *
     * @param employeePrimaryKey
     *
     *        the primary key of the entity to be updated.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void modifyEmployeedAddressAndFireAChangeEvent(int employeePrimaryKey) {
        // (a) load the entity we want to update
        Employee entityToUpdate = em.find(Employee.class, Integer.valueOf(employeePrimaryKey));
        if (entityToUpdate == null) {
            throw new RuntimeException(format("No employee etity could be found with id: %1$s", employeePrimaryKey));
        }

        // (b) Before we update the entity we prepare the state variables that will go into the update event
        // and make some noise on the log
        String oldAddress = entityToUpdate.getAddress();
        String newAddress = getIncrementedAddressNumber();
        LOGGER.info("Going to update employee: {} to have its address change from: {} -> to:  {}", employeePrimaryKey, oldAddress, newAddress);

        // (c) So now we do our change
        entityToUpdate.setAddress(newAddress);

        // (f) We inform the interested parties that this employeed just got its address updated to some new incremented address
        SomeEntityChangeEvent eventToFire = new SomeEntityChangeEvent(oldAddress, newAddress, Integer.valueOf(employeePrimaryKey));
        someEntityChangeEvent.fire(eventToFire);
    }

    /**
     *
     * @return a string of the form "IncrementAddressNumber: 1"
     */
    protected String getIncrementedAddressNumber() {
        return format("IncrementAddressNumber: %1$s ", CURRENT_ADDRESS_MODIFICATION_NUMBER.incrementAndGet());
    }

}
