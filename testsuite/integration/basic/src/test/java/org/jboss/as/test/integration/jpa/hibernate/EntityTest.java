/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hibernate;

import java.util.HashSet;
import java.util.Set;
import jakarta.ejb.Stateful;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

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
