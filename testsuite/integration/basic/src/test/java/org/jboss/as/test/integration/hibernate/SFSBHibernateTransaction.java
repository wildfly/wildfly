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
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * Test operations including rollback using Hibernate transaction and Sessionfactory inititated from hibernate.cfg.xml and
 * properties added to Hibernate Configuration in AS7 container without any Jakarta Persistence assistance
 *
 * @author Madhumita Sadhukhan
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSBHibernateTransaction {

    private static SessionFactory sessionFactory;

    public void cleanup() {
        sessionFactory.close();
    }

    public void setupConfig() {
        // static {
        try {

            // prepare the configuration
            Configuration configuration = new Configuration();
            configuration.getProperties().put(AvailableSettings.JTA_PLATFORM, JBossAppServerJtaPlatform.class);
            configuration.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
            configuration.setProperty(Environment.DATASOURCE, "java:jboss/datasources/ExampleDS");
            configuration.setProperty("hibernate.listeners.envers.autoRegister", "false");

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
    public Student createStudent(String firstName, String lastName, String address, boolean rollback) {

        // setupConfig();
        Student student = new Student();
        student.setAddress(address);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        Session session = sessionFactory.openSession();

        try {
            Transaction trans = session.beginTransaction();
            session.persist(student);
            if (rollback) {
                trans.rollback();
            } else {
                trans.commit();
            }
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while persisting student entity", e);
        }

        session.close();
        return student;
    }

    // update student
    public Student updateStudent(String address, int id) {

        Session session = sessionFactory.openSession();
        Student student = session.get(Student.class, id);
        student.setAddress(address);

        try {
            // invoking the Hibernate transaction
            Transaction trans = session.beginTransaction();
            student = session.merge(student);
            trans.commit();
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while persisting student entity", e);
        }

        session.close();
        return student;
    }

    // fetch student
    public Student getStudentNoTx(int id) {
        // Transaction trans = sessionFactory.openSession().beginTransaction();
        Student emp = sessionFactory.openSession().get(Student.class, id);
        return emp;
    }

}
