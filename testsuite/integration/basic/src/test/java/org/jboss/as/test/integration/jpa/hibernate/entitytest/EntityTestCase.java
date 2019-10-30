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

package org.jboss.as.test.integration.jpa.hibernate.entitytest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jpa.hibernate.EntityTest;
import org.jboss.as.test.integration.jpa.hibernate.entity.Company;
import org.jboss.as.test.integration.jpa.hibernate.entity.Customer;
import org.jboss.as.test.integration.jpa.hibernate.entity.Flight;
import org.jboss.as.test.integration.jpa.hibernate.entity.Ticket;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Hibernate entity tests (based on the EAP 5 test), using {@link EntityTest} bean. Tests relations between entities,
 * loading entities and named queries.
 * <p>
 * Note that this test uses an extended persistence context, so that the Hibernate session will stay open long enough
 * to complete each test.  A transaction scoped entity manager would be closed after each JTA transaction completes.
 *
 * @author Zbyněk Roubalík
 */
@RunWith(Arquillian.class)
public class EntityTestCase {

    private static final String ARCHIVE_NAME = "jpa_entitytest";

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

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(EntityTestCase.class, Customer.class, Ticket.class, Flight.class, EntityTest.class, Company.class);
        jar.addAsManifestResource(EntityTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }


    @Test
    @InSequence(1)
    public void testOneToMany() throws Exception {

        EntityTest test = lookup("EntityTest", EntityTest.class);
        Customer c = test.oneToManyCreate();
        assertNotNull(c);
        assertNotNull(c.getTickets());
        Set<Ticket> tickets = c.getTickets();
        assertTrue(tickets.size() > 0);

        assertNotNull(c);
        assertNotNull(c.getTickets());
        tickets = c.getTickets();
        assertTrue(tickets.size() > 0);

    }

    @Test
    @InSequence(2)
    public void testManyToOne() throws Exception {

        EntityTest test = lookup("EntityTest", EntityTest.class);
        Flight f = test.manyToOneCreate();

        Flight f2 = test.findFlightById(f.getId());

        assertEquals(f.getId(), new Long(1));

        assertEquals(f.getName(), f2.getName());
        assertEquals(f.getCompany().getName(), f2.getCompany().getName());

        Company c = test.findCompanyById(f.getCompany().getId());
        assertNotNull("Company has one flight.", c.getFlights());
        assertEquals(f.getCompany().getFlights().size(), c.getFlights().size());

    }

    @Test
    @InSequence(3)
    public void testManyToMany() throws Exception {

        EntityTest test = lookup("EntityTest", EntityTest.class);
        test.manyToManyCreate();

        Flight f1 = test.findFlightById(new Long(1));
        assertTrue("Name read from Hibernate is Airline 1", f1.getCompany().getName().equals("Airline 1"));

        Flight f2 = test.findFlightById(new Long(2));
        assertTrue("Name read from Hibernate is Airline 1", f2.getCompany().getName().equals("Airline 2"));

        assertEquals(1, f1.getCustomers().size());
        assertEquals(2, f2.getCustomers().size());

    }

    @Test
    @InSequence(4)
    public void testNamedQueries() throws Exception {

        EntityTest test = lookup("EntityTest", EntityTest.class);
        int count = test.testAllCustomersQuery();
        assertEquals("Number returned for allCustomers query", 4, count);

        Customer c = test.testCustomerByIdQuery();
        assertNotNull("One object should be returned by customerById query", c);
    }

    @Test
    @InSequence(5)
    public void testFlush() throws Exception {

        EntityTest test = lookup("EntityTest", EntityTest.class);
        Customer c = test.createCustomer("Thomas");
        test.changeCustomer(c.getId(), "George");

        Customer c2 = test.findCustomerById(c.getId());
        assertEquals("George", c2.getName());

    }

}
