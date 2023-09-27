/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.resultstream;

import java.util.stream.Stream;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.PersistenceContext;

import jakarta.persistence.EntityManager;

/**
 * Stateful session bean for testing Jakarta Persistence 2.2 API
 * jakarta.persistence.Query#getResultStream
 *
 * @author Gail Badner
 */
@Stateful
public class ResultStreamTest {

    @PersistenceContext(unitName = "mypc")
    EntityManager em;

    public Ticket createTicket() throws Exception {
        Ticket t = new Ticket();
        t.setNumber("111");

        em.persist(t);

        return t;
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Stream getTicketStreamOrderedById() {
        return em.createQuery( "select t from Ticket t order by t.id" ).getResultStream();
    }

    public void deleteTickets() {
        em.createQuery( "delete from Ticket" ).executeUpdate();
    }
}
