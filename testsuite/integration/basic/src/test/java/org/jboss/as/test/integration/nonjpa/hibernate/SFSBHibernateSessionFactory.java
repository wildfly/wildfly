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

package org.jboss.as.test.integration.nonjpa.hibernate;

import java.io.File;
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
import org.hibernate.service.BootstrapServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

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
    private static Configuration configuration;
    private static ServiceRegistryBuilder builder;
    private static ServiceRegistry serviceRegistry;

    protected static final Class[] NO_CLASSES = new Class[0];
    protected static final String NO_MAPPINGS = new String();

    public void setupConfig() {
        // static {
        try {

            System.out.println("Current dir : " + (new File(".")).getCanonicalPath());

            // prepare the configuration
            Configuration configuration = new Configuration().setProperty(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS,
                    "true");
            configuration.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
            configuration.setProperty(Environment.DATASOURCE, "java:jboss/datasources/ExampleDS");
            // fetch the properties
            Properties properties = new Properties();
            properties.putAll(configuration.getProperties());
            Environment.verifyProperties(properties);
            ConfigurationHelper.resolvePlaceHolders(properties);

            // build the serviceregistry
            final BootstrapServiceRegistryBuilder bootstrapbuilder = new BootstrapServiceRegistryBuilder();
            builder = new ServiceRegistryBuilder(bootstrapbuilder.build()).applySettings(properties);
            serviceRegistry = builder.buildServiceRegistry();
            // Create the SessionFactory from Configuration
            sessionFactory = configuration.configure("hibernate.cfg.xml").buildSessionFactory(serviceRegistry);

        } catch (Throwable ex) { // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            // ex.printStackTrace();
            throw new ExceptionInInitializerError(ex);
        }

    }

    // create student
    public Student createStudent(String firstName, String lastName, String address, int id) {

        // setupConfig();
        Student student = new Student();
        student.setStudentId(id);
        student.setAddress(address);
        student.setFirstName(firstName);
        student.setLastName(lastName);

        try {
            Session session = sessionFactory.openSession();
            session.save(student);
            session.flush();
            session.close();
        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException("transactional failure while persisting student entity", e);

        }

        return student;
    }

    // fetch student
    public Student getStudent(int id) {
        Student emp = (Student) sessionFactory.openSession().load(Student.class, id);
        return emp;
    }

}