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
