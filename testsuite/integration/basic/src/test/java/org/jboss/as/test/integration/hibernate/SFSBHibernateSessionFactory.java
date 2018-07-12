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

package org.jboss.as.test.integration.hibernate;

import java.util.Properties;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * Test that a Hibernate sessionfactory can be inititated from hibernate.cfg.xml and properties added to Hibernate Configuration
 * in AS7 container without any JPA assistance
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
            Configuration configuration = new Configuration().setProperty(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS,
                    "true");
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

            Environment.verifyProperties(properties);
            ConfigurationHelper.resolvePlaceHolders(properties);

            sessionFactory = configuration.buildSessionFactory();
        } catch (Throwable ex) { // Make sure you log the exception, as it might be swallowed
            throw new RuntimeException("Could not setup config", ex);
        }

    }

    // create student
    public Student createStudent(String firstName, String lastName, String address, int id) {

        Student student = new Student();
        student.setStudentId(id);
        student.setAddress(address);
        student.setFirstName(firstName);
        student.setLastName(lastName);

        try {
            // We are not explicitly initializing a Transaction as Hibernate is expected to invoke the JTA TransactionManager
            // implicitly
            Session session = sessionFactory.openSession();
            session.save(student);
            session.flush();
            session.close();
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while persisting student entity", e);
        }

        return student;
    }

    // fetch student
    public Student getStudent(int id) {
        Student emp = sessionFactory.openSession().load(Student.class, id);
        return emp;
    }

}