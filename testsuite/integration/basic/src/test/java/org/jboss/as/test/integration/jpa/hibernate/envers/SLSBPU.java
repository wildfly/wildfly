/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.hibernate.envers;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

/**
 * @author Strong Liu
 */
@Stateless
public class SLSBPU {
    @PersistenceContext(unitName = "mypc")
    EntityManager em;

    public Person createPerson(String firstName, String secondName, String streetName, int houseNumber) {
        Address address = new Address();
        address.setHouseNumber(houseNumber);
        address.setStreetName(streetName);
        Person person = new Person();
        person.setName(firstName);
        person.setSurname(secondName);
        person.setAddress(address);
        address.getPersons().add(person);

        em.persist(address);
        em.persist(person);
        return person;
    }

    public Person updatePerson(Person p) {
        return em.merge(p);
    }

    public Address updateAddress(Address a) {
        return em.merge(a);
    }

    public int retrieveOldPersonVersionFromAddress(int id) {
        AuditReader reader = AuditReaderFactory.get(em);
        Address address1_rev = reader.find(Address.class, id, 1);
        return address1_rev.getPersons().size();
    }
}
