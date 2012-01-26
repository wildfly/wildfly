/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2010, Red Hat, and individual contributors
 * as indicated by the @author tags. 
 * See the copyright.txt in the distribution for a full listing 
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 * 
 * @author JBoss Inc.
 */
/*
 * RestaurantManager.java
 *
 * Copyright (c) 2003 Arjuna Technologies Ltd.
 *
 * $Id: RestaurantManager.java,v 1.3 2004/04/21 13:09:18 jhalliday Exp $
 *
 */
package org.jboss.as.test.xts.simple.wsat;

import org.jboss.logging.Logger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class represents the back-end resource for managing Restaurant bookings.
 * 
 * This is a mock implementation that just keeps a counter of how many bookings have been made.
 * 
 * @author paul.robinson@redhat.com, 2012-01-04
 */
public class MockRestaurantManager {
    private static final Logger log = Logger.getLogger(MockRestaurantManager.class);

    // The singleton instance of this class.
    private static MockRestaurantManager singletonInstance;

    // A thread safe booking counter
    private AtomicInteger bookings = new AtomicInteger(0);

    private boolean volatileCommit = false;
    private boolean volatileRollback = false;

    /**
     * Accessor to obtain the singleton restaurant manager instance.
     * 
     * @return the singleton RestaurantManager instance.
     */
    public synchronized static MockRestaurantManager getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new MockRestaurantManager();
        }

        return singletonInstance;
    }

    /**
     * Make a booking with the restaurant.
     * 
     * @param txID The transaction identifier
     */
    public synchronized void makeBooking(Object txID) {
        log.info("[SERVICE] makeBooking called on backend resource.");
    }

    /**
     * Prepare local state changes for the supplied transaction. This method should persist any required information to ensure
     * that it can undo (rollback) or make permanent (commit) the work done inside this transaction, when told to do so.
     * 
     * @param txID The transaction identifier
     * @return true on success, false otherwise
     */
    public boolean prepare(Object txID) {
        log.info("[SERVICE] prepare called on backend resource.");
        return true;
    }

    /**
     * commit local state changes for the supplied transaction
     * 
     * @param txID The transaction identifier
     */
    public void commit(Object txID) {
        log.info("[SERVICE] commit called on backend resource.");
        bookings.getAndIncrement();
    }

    /**
     * roll back local state changes for the supplied transaction
     * 
     * @param txID The transaction identifier
     */
    public void rollback(Object txID) {
        log.info("[SERVICE] rollback called on backend resource.");
    }

    /**
     * a business action for Volatile2PCParticipant#prepare()
     *
     * @param txID The transaction identifier
     */
    public boolean volatilePrepare(Object txID) {
        log.info("[SERVICE] volatile prepare called on backend resource.");
        return true;
    }

    /**
     * a business action for Volatile2PCParticipant#commit()
     *
     * @param txID The transaction identifier
     */
    public void volatileCommit(Object txID) {
        log.info("[SERVICE] volatile commit called on backend resource.");
        volatileCommit = true;
    }

    /**
     * a business action for Volatile2PCParticipant#rollback()
     *
     * @param txID The transaction identifier
     */
    public void volatileRollback(Object txID) {
        log.info("[SERVICE] volatile rollback called on backend resource.");
        volatileRollback = true;
    }

    /**
     * Returns the number of bookings
     * 
     * @return the number of bookings.
     */
    public int getBookingCount() {
        return bookings.get();
    }

    public boolean wasVolatileCommit() {
        return volatileCommit;
    }

    public boolean wasVolatileRollback() {
        return volatileRollback;
    }

    /**
     * Reset the booking counter to zero
     */
    public void reset() {
        bookings.set(0);
        volatileCommit = false;
        volatileRollback = false;
    }
}
