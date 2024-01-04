/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.txbridge.fromjta.service;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.jboss.logging.Logger;

@Stateless
@Remote(FirstServiceAT.class)
@WebService(serviceName = "FirstServiceATService", portName = "FirstServiceAT",
    name = "FirstServiceAT", targetNamespace = "http://www.jboss.com/jbossas/test/txbridge/fromjta/first")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class FirstServiceATImpl implements FirstServiceAT {
    private static final Logger log = Logger.getLogger(FirstServiceATImpl.class);
    private static final int ENTITY_ID = 1;

    @PersistenceContext
    protected EntityManager em;

    /**
     * Increment the first counter. This is done by updating the counter within Jakarta Transactions transaction.
     * The Jakarta Transactions transaction was automatically bridged from the WS-AT transaction.
     */
    @WebMethod
    public void incrementCounter(int num) {
        log.trace("Service invoked to increment the counter by '" + num + "'");
        FirstCounterEntity entityFirst = lookupCounterEntity();
        entityFirst.incrementCounter(num);
        em.merge(entityFirst);
    }

    @WebMethod
    public int getCounter() {
        log.trace("Service getCounter was invoked");
        FirstCounterEntity firstCounterEntity = lookupCounterEntity();
        if (firstCounterEntity == null) {
            return -1;
        }
        return firstCounterEntity.getCounter();
    }

    @WebMethod
    public void resetCounter() {
        FirstCounterEntity entityFirst = lookupCounterEntity();
        entityFirst.setCounter(0);
        em.merge(entityFirst);
    }

    private FirstCounterEntity lookupCounterEntity() {
        FirstCounterEntity entityFirst = em.find(FirstCounterEntity.class, ENTITY_ID);
        if (entityFirst == null) {
            entityFirst = new FirstCounterEntity(ENTITY_ID, 0);
            em.persist(entityFirst);
        }
        return entityFirst;
    }

}
