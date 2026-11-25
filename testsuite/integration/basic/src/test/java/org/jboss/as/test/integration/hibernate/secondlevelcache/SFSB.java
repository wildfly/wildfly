/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.hibernate.secondlevelcache;

import java.util.Properties;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * @author Madhumita Sadhukhan
 */
@Stateful
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SFSB {

    private static SessionFactory sessionFactory;

    public void cleanup() {
        sessionFactory.close();
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void setupConfig() {
        // static {
        try {

            // prepare the configuration
            Configuration configuration = new Configuration();
            configuration.getProperties().put(AvailableSettings.JTA_PLATFORM, JBossAppServerJtaPlatform.class);
            configuration.getProperties().put(AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta");
            configuration.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
            configuration.setProperty(Environment.DATASOURCE, "java:jboss/datasources/ExampleDS");
            // fetch the properties
            Properties properties = new Properties();
            configuration = configuration.configure("hibernate.cfg.xml");
            properties.putAll(configuration.getProperties());
            ConfigurationHelper.resolvePlaceHolders(properties);

            sessionFactory = configuration.buildSessionFactory();

        } catch (Throwable ex) { // Make sure you log the exception, as it might be swallowed
            ex.printStackTrace();
            throw new ExceptionInInitializerError(ex);
        }
    }

    // create student
    public Student createStudent(String firstName, String lastName, String address) {
        // setupConfig();
        Student student = new Student();
        student.setAddress(address);
        student.setFirstName(firstName);
        student.setLastName(lastName);

        try {
            Session session = sessionFactory.openSession();
            // Hibernate ORM 5.2+ doesn't allow beginTransaction in an active Jakarta Transactions transaction, as openSession
            // will automatically join the Jakarta Transactions transaction.
            // See https://github.com/hibernate/hibernate-orm/wiki/Migration-Guide---5.2
            //Transaction ormTransaction = session.beginTransaction(); // join the current Jakarta Transactions transaction
            //TransactionStatus status = ormTransaction.getStatus();
            //if(status.isNotOneOf(TransactionStatus.ACTIVE)) {
            //    throw new RuntimeException("Hibernate Transaction is not active after joining Hibernate to Jakarta Transactions transaction: " + status.name());
            //}

            session.persist(student);
            // trans.commit();
            session.close();
        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException("Failure while persisting student entity", e);

        }
        return student;
    }

    // get student
    public Student getStudent(int id) {
        Student student;

        try {
            Session session = sessionFactory.openSession();
            // Hibernate ORM 5.2+ doesn't allow beginTransaction in an active Jakarta Transactions transaction, as openSession
            // will automatically join the Jakarta Transactions transaction.
            // See https://github.com/hibernate/hibernate-orm/wiki/Migration-Guide---5.2
            // Transaction ormTransaction = session.beginTransaction(); // join the current Jakarta Transactions transaction
            // TransactionStatus status = ormTransaction.getStatus();
            // if(status.isNotOneOf(TransactionStatus.ACTIVE)) {
            //    throw new RuntimeException("Hibernate Transaction is not active after joining Hibernate to Jakarta Transactions transaction: " + status.name());
            // }
            student = session.get(Student.class, id);
            session.close();

        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException("Failure while loading student entity", e);

        }
        return student;
    }


    public void clearCache() {
        sessionFactory.getCache().evictAllRegions();

    }
}
