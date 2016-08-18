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

package org.jboss.as.test.integration.jpa.hibernate;

import java.util.HashSet;
import java.util.Set;
import javax.ejb.Stateful;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.hibernate.Session;
import org.jboss.as.test.integration.jpa.hibernate.entity.Company;
import org.jboss.as.test.integration.jpa.hibernate.entity.Customer;
import org.jboss.as.test.integration.jpa.hibernate.entity.Flight;
import org.jboss.as.test.integration.jpa.hibernate.entity.Ticket;

/**
 * Stateful session bean for testing Hibernate entities:
 * One to Many, Many to One, Many to Many mapping and named queries.
 *
 * @author Zbyněk Roubalík
 */
@Stateful
public class EntityTest {

    @PersistenceContext(unitName = "entity_pc", type = PersistenceContextType.EXTENDED)
    private Session session;

    public Customer oneToManyCreate() throws Exception {
        Ticket t = new Ticket();
        t.setNumber("111");

        Customer c = new Customer();
        Set<Ticket> tickets = new HashSet<Ticket>();

        tickets.add(t);
        t.setCustomer(c);
        c.setTickets(tickets);

        session.save(c);

        return c;
    }

    public Flight manyToOneCreate() throws Exception {
        Flight f = new Flight();
        f.setName("Flight number one");
        f.setId(new Long(1));

        Company comp = new Company();
        comp.setName("Airline 1");
        f.setCompany(comp);

        session.save(f);

        return f;
    }

    public void manyToManyCreate() throws Exception {

        Flight f1 = findFlightById(new Long(1));
        Flight f2 = new Flight();

        f2.setId(new Long(2));
        f2.setName("Flight two");

        Company us = new Company();
        us.setName("Airline 2");
        f2.setCompany(us);

        Set<Customer> customers1 = new HashSet<Customer>();
        Set<Customer> customers2 = new HashSet<Customer>();

        Customer c1 = new Customer();
        c1.setName("John");
        customers1.add(c1);

        Customer c2 = new Customer();
        c2.setName("Tom");
        customers2.add(c2);

        Customer c3 = new Customer();
        c3.setName("Pete");
        customers2.add(c3);

        f1.setCustomers(customers1);
        f2.setCustomers(customers2);

        session.save(c1);
        session.save(c2);
        session.save(c3);
        session.save(f2);
    }

    public int testAllCustomersQuery() {

        //session.flush();
        return session.getNamedQuery("allCustomers").list().size();
    }

    public Customer testCustomerByIdQuery() {
        Customer c = new Customer();
        c.setName("Peter");

        session.save(c);
        session.flush();

        return (Customer) session.getNamedQuery("customerById").setParameter("id", c.getId()).uniqueResult();

    }

    public Customer createCustomer(String name) {
        Customer c = new Customer();
        c.setName(name);
        session.save(c);
        return c;
    }

    public void changeCustomer(Long id, String name) {
        Customer c = session.load(Customer.class, id);
        c.setName(name);
    }


    public Flight findFlightById(Long id) {

        return session.load(Flight.class, id);
    }

    public Company findCompanyById(Long id) {

        return session.load(Company.class, id);
    }

    public Customer findCustomerById(Long id) {

        return session.load(Customer.class, id);
    }

}
