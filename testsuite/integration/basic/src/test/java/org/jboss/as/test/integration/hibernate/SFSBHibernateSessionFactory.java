/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.hibernate;

import java.util.Properties;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * Test that a Hibernate sessionfactory can be inititated from hibernate.cfg.xml and properties added to Hibernate Configuration
 * in AS7 container without any Jakarta Persistence assistance
 *
 * @author Madhumita Sadhukhan
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSBHibernateSessionFactory {

    private static SessionFactory sessionFactory;

    public void cleanup() {
        sessionFactory.close();
    }

    public void setupConfig() {
        // static {
        try {

            // prepare the configuration
            Configuration configuration = new Configuration();
            configuration.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
            configuration.setProperty(Environment.DATASOURCE, "java:jboss/datasources/ExampleDS");
            configuration.setProperty("hibernate.listeners.envers.autoRegister", "false");
            /* Hibernate 5.2+ (see https://hibernate.atlassian.net/browse/HHH-10877 +
                 https://hibernate.atlassian.net/browse/HHH-12665) no longer defaults
                 to allowing a DML operation outside of a started transaction.
                 The application workaround is to configure new property hibernate.allow_update_outside_transaction=true.
            */
            configuration.setProperty("hibernate.allow_update_outside_transaction","true");

            // fetch the properties
            Properties properties = new Properties();
            configuration = configuration.configure("hibernate.cfg.xml");
            properties.putAll(configuration.getProperties());

            ConfigurationHelper.resolvePlaceHolders(properties);

            sessionFactory = configuration.buildSessionFactory();
        } catch (Throwable ex) { // Make sure you log the exception, as it might be swallowed
            throw new RuntimeException("Could not setup config", ex);
        }

    }

    // create student
    public Student createStudent(String firstName, String lastName, String address) {

        Student student = new Student();
        student.setAddress(address);
        student.setFirstName(firstName);
        student.setLastName(lastName);

        try {
            // We are not explicitly initializing a Transaction as Hibernate is expected to invoke the Jakarta Transactions TransactionManager
            // implicitly
            Session session = sessionFactory.openSession();
            session.persist(student);
            session.flush();
            session.close();
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while persisting student entity", e);
        }

        return student;
    }

    // fetch student
    public Student getStudent(int id) {
        Student emp = sessionFactory.openSession().get(Student.class, id);
        return emp;
    }

}
