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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Hibernate test using JPA 2.2 API javax.persistence.Query#getResultStream
 * using {@link ResultStreamTest} bean.
 * <p>
 * Note that this test uses an extended persistence context, so that the Hibernate session will stay open long enough
 * to complete each test.  A transaction scoped entity manager would be closed after each JTA transaction completes.
 *
 * @author Zbyněk Roubalík
 * @author Gail Badner
 */
@RunWith(Arquillian.class)
public class ResultStreamTestCase {

    private static final String ARCHIVE_NAME = "jpa_resultstreamtest";

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }


    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create( JavaArchive.class, ARCHIVE_NAME + ".jar" );
        jar.addClasses(ResultStreamTestCase.class, Ticket.class, ResultStreamTest.class);
        jar.addAsManifestResource( ResultStreamTestCase.class.getPackage(), "persistence.xml", "persistence.xml" );
        return jar;
    }

    @Test
    public void testCreateQueryRemove() throws Exception {

        ResultStreamTest test = lookup( "ResultStreamTest", ResultStreamTest.class );

        List<Ticket> tickets = new ArrayList<Ticket>(4);
        tickets.add( test.createTicket() );
        tickets.add( test.createTicket() );
        tickets.add( test.createTicket() );
        tickets.add( test.createTicket() );

        Stream<Ticket> stream = (Stream<Ticket>) test.getTicketStreamOrderedById();

        Iterator<Ticket> ticketIterator = tickets.iterator();
        stream.forEach( t ->  assertEquals( t.getId(), ticketIterator.next().getId() ) );

        test.deleteTickets();
    }
}
