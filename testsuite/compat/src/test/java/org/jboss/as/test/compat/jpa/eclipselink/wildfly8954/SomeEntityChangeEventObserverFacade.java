/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.compat.jpa.eclipselink.wildfly8954;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.as.test.compat.jpa.eclipselink.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observe the event and report
 */
@Singleton
public class SomeEntityChangeEventObserverFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(SomeEntityChangeEventObserverFacade.class);

    // state that will be updated once the on success transaction is obseved
    /**
     * Intialize the bean state to bug not detected. Once the system tests fires the event this value might change to true if
     * the bug gets detected.
     */
    private boolean bugDetected = false;
    private SomeEntityChangeEvent lastProcessedSomeEntityChangeEvent = null;
    private String lastProcessedEntityAddressBeforeExecutingRefresh = null;
    private String lastProcessedEntityAddressAfterExecutingRefresh = null;

    @PersistenceContext(unitName = "hibernate3_pc")
    EntityManager em;

    /**
     * In this test we check if the requires new annotation in the observer is effective. in our application this is not the
     * case, and we were forced into using the executor facade to be able to open a new transaction context.
     *
     * Here it seems to be effective.
     *
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void observeEvent(
        @Observes(during = TransactionPhase.AFTER_SUCCESS) SomeEntityChangeEvent someEntityChangeEvent) {

        // (a) we start by fetching the entity that should be found on the eclipse link shared cache (no db access needed)
        Integer employeePrimaryKey = someEntityChangeEvent.getSomeEntityId();
        Employee entity = em.find(Employee.class, employeePrimaryKey);

        // (b) Now we see what value we have to the address
        String entityAddressBeforeExecutingRefresh = entity.getAddress();
        boolean eventBelieveWeHaveStaleData = someEntityChangeEvent.isValueReadFromSharedCacheStale(entityAddressBeforeExecutingRefresh);
        if (eventBelieveWeHaveStaleData) {
            LOGGER.error("According to event: {}  we are currently holding stale data. address value on enitity fetached from shared cache was: {}. This value is stale. ",
                someEntityChangeEvent, entityAddressBeforeExecutingRefresh);
        }

        // (c) Now we refresh the entity - which should force querying the DB to get fresh new data
        em.refresh(entity);
        String entityAddressAfterExecutingRefresh = entity.getAddress();

        LOGGER.info(
            "Before Refresh value was: {}, After Refresh: {}. The NEW Value on entity passed by event object - which must be the accurate value - was: {} ",
            entityAddressBeforeExecutingRefresh, entityAddressAfterExecutingRefresh, someEntityChangeEvent.getNewValue());
        if (!entityAddressAfterExecutingRefresh.equals(entityAddressBeforeExecutingRefresh)) {
            LOGGER.error(
                "NOT OK - BUG DETECTED - Wildfly ON_SUCCESS handling observing stale entity that does not match what transaction persisted."
                    + " Entity Address before refresh: {} And After Refresh: {} For Employee: {} ",
                entityAddressBeforeExecutingRefresh, entityAddressAfterExecutingRefresh, employeePrimaryKey);
            bugDetected = true;
        } else {
            LOGGER.info(
                " OK - The entity remained unchanged before and after refresh. Before Refresh value was: {}, After Refresh: {} ",
                entityAddressBeforeExecutingRefresh, entityAddressAfterExecutingRefresh);
            bugDetected = false;
        }

        // (d) Update the state of the singleton with the metadata about the last processed event
        // can be useful for the test fire an assertion erro
        lastProcessedSomeEntityChangeEvent = someEntityChangeEvent;
        lastProcessedEntityAddressBeforeExecutingRefresh = entityAddressBeforeExecutingRefresh;
        lastProcessedEntityAddressAfterExecutingRefresh = entityAddressAfterExecutingRefresh;
    }

    /**
     * @return TRUE if the bug has been detected.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean isBugDetected() {
        return bugDetected;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public SomeEntityChangeEvent getLastProcessedSomeEntityChangeEvent() {
        return lastProcessedSomeEntityChangeEvent;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String getLastProcessedEntityAddressBeforeExecutingRefresh() {
        return lastProcessedEntityAddressBeforeExecutingRefresh;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String getLastProcessedEntityAddressAfterExecutingRefresh() {
        return lastProcessedEntityAddressAfterExecutingRefresh;
    }

}
