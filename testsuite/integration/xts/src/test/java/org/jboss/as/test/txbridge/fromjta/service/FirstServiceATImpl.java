/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.txbridge.fromjta.service;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
