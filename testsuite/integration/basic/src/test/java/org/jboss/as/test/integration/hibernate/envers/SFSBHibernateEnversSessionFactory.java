/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.hibernate.envers;

import java.util.List;
import java.util.Properties;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * Test that Hibernate Envers is working over Native Hibernate API in AS7 container without any Jakarta Persistence assistance
 *
 * @author Madhumita Sadhukhan
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSBHibernateEnversSessionFactory {

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
    public StudentAudited createStudent(String firstName, String lastName, String address) {

        // setupConfig();
        StudentAudited student = new StudentAudited();
        student.setAddress(address);
        student.setFirstName(firstName);
        student.setLastName(lastName);

        try {
            Session session = sessionFactory.openSession();
            Transaction trans = session.beginTransaction();
            session.persist(student);
            session.flush();
            trans.commit();
            session.close();
        } catch (Exception e) {
            throw new RuntimeException("Failure while persisting student entity", e);
        }

        return student;
    }

    // update student
    public StudentAudited updateStudent(String address, int id) {
        StudentAudited student;
        try {
            Session session = sessionFactory.openSession();
            Transaction trans = session.beginTransaction();
            student = session.get(StudentAudited.class, id);
            student.setAddress(address);
            student = session.merge(student);
            session.flush();
            trans.commit();
            session.close();
        } catch (Exception e) {
            throw new RuntimeException("Failure while persisting student entity", e);
        }

        // session.close();
        return student;
    }

    // fetch Audited entity from Audit tables
    public StudentAudited retrieveOldStudentVersion(int id) {
        AuditReader reader = AuditReaderFactory.get(sessionFactory.openSession());
        StudentAudited student_rev = reader.find(StudentAudited.class, id, 1);
        List<Number> revlist = reader.getRevisions(StudentAudited.class, id);
        // this is for checking revision size hence not removing this S.o.p
        return student_rev;
    }

}
