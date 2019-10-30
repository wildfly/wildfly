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
package org.jboss.as.test.integration.jpa.hibernate.envers;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
