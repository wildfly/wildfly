/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.hibernate.envers;

import java.util.List;
import java.util.Properties;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * Test that Hibernate Envers is working over Native Hibernate API in AS7 container without any JPA assistance
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
            Configuration configuration = new Configuration().setProperty(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS,
                    "true");
            configuration.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
            configuration.setProperty(Environment.DATASOURCE, "java:jboss/datasources/ExampleDS");
            // fetch the properties
            Properties properties = new Properties();
            configuration = configuration.configure("hibernate.cfg.xml");
            properties.putAll(configuration.getProperties());
            Environment.verifyProperties(properties);
            ConfigurationHelper.resolvePlaceHolders(properties);

            sessionFactory = configuration.buildSessionFactory();
        } catch (Throwable ex) { // Make sure you log the exception, as it might be swallowed
            throw new RuntimeException("Could not setup config", ex);
        }

    }

    // create student
    public StudentAudited createStudent(String firstName, String lastName, String address, int id) {

        // setupConfig();
        StudentAudited student = new StudentAudited();
        student.setStudentId(id);
        student.setAddress(address);
        student.setFirstName(firstName);
        student.setLastName(lastName);

        try {
            Session session = sessionFactory.openSession();
            Transaction trans = session.beginTransaction();
            session.save(student);
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
            student = session.load(StudentAudited.class, id);
            student.setAddress(address);
            session.save(student);
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
