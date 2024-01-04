/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
 * Hibernate test using Jakarta Persistence 2.2 API jakarta.persistence.Query#getResultStream
 * using {@link ResultStreamTest} bean.
 * <p>
 * Note that this test uses an extended persistence context, so that the Hibernate session will stay open long enough
 * to complete each test.  A transaction scoped entity manager would be closed after each Jakarta Transactions transaction completes.
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
