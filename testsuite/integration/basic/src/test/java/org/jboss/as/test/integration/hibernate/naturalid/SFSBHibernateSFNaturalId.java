/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.hibernate.naturalid;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.ServiceRegistry;


/**
 * Test that naturalId API used with Hibernate sessionfactory can be initiated from hibernate.cfg.xml and properties added to
 * Hibernate Configuration in AS7 container
 *
 * @author Madhumita Sadhukhan
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSBHibernateSFNaturalId {

    private static SessionFactory sessionFactory;

    public void cleanup() {
        sessionFactory.close();
    }

    public void setupConfig() {
        try {
            final ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder(new BootstrapServiceRegistryBuilder().build())
                    .configure("hibernate.cfg.xml")
                    .applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop")
                    .applySetting(AvailableSettings.DATASOURCE, "java:jboss/datasources/ExampleDS")
                    .build();

            final Metadata metadata = new MetadataSources(serviceRegistry)
                    .buildMetadata();

            sessionFactory = metadata.buildSessionFactory();
        } catch (Throwable ex) {
            throw new RuntimeException("Could not setup config", ex);
        }
    }

    // create person
    public Person createPerson(String firstName, String lastName, String address, int voterId) {

        Person per = new Person();
        per.setAddress(address);
        per.setPersonVoterId(voterId);
        per.setFirstName(firstName);
        per.setLastName(lastName);

        try {
            // We are not explicitly initializing a Transaction as Hibernate is expected to invoke the JTA TransactionManager
            // implicitly
            Session session = sessionFactory.openSession();
            session.persist(per);
            session.flush();
            session.close();
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while persisting student entity", e);
        }

        return per;
    }

    // fetch person reference
    public Person getPersonReference(String name, int voterid) {
        Person emp = sessionFactory.openSession().byNaturalId(Person.class).using("firstName", name)
                .using("personVoterId", voterid).getReference();
        return emp;
    }

    // load person
    public Person loadPerson(String name, int voterid) {
        Person emp = sessionFactory.openSession().byNaturalId(Person.class).using("firstName", name)
                .using("personVoterId", voterid).load();
        return emp;
    }

}
