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

package org.jboss.as.test.integration.hibernate.envers;

import java.io.File;
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
import org.hibernate.service.BootstrapServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

/**
 * Test that Hibernate Envers is working over Native Hibernate API in AS7 container without any JPA assistance
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
    private static Session session;

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
            configuration.setProperty("org.hibernate.envers.audit_strategy",
                    "org.hibernate.envers.strategy.DefaultAuditStrategy");
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
            // Session session = sessionFactory.openSession();

        } catch (Throwable ex) { // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
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
            Transaction trans = session.beginTransaction();
            session.save(student);
            session.flush();
            trans.commit();
            session.close();
        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException("Failure while persisting student entity", e);

        }

        return student;
    }

    // update student
    public Student updateStudent(String address, int id) {

        Student student;

        try {
            Session session = sessionFactory.openSession();
            Transaction trans = session.beginTransaction();
            student = (Student) session.load(Student.class, id);
            student.setAddress(address);
            session.save(student);
            session.flush();
            trans.commit();
            session.close();

        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException("Failure while persisting student entity", e);

        }

        // session.close();
        return student;
    }

    // fetch Audited entity from Audit tables
    public Student retrieveOldStudentVersion(int id) {
        AuditReader reader = AuditReaderFactory.get(sessionFactory.openSession());
        Student student_rev = reader.find(Student.class, id, 1);
        List<Number> revlist = reader.getRevisions(Student.class, id);
        // this is for checking revision size hence not removing this S.o.p
        System.out.println("Size of revisionList:--" + revlist.size());
        return student_rev;
    }

}