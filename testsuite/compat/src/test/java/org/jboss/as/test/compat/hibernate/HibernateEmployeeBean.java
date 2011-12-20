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

package org.jboss.as.test.compat.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.jboss.as.test.compat.common.Employee;
import org.jboss.as.test.compat.common.EmployeeBean;

import javax.annotation.Resource;
import javax.ejb.Stateful;

/**
 *
 * @author Madhumita Sadhukhan
 */
@Stateful
public class HibernateEmployeeBean implements EmployeeBean {

    public static final String DATASOURCE = "java:jboss/datasources/ExampleDS";

    private static SessionFactory sessionFactory;

    @Resource(mappedName = "java:jboss/infinispan/container/hibernate")
    private Object container;

    static {
        final Configuration configuration = new Configuration()
                .setProperty(Configuration.USE_NEW_ID_GENERATOR_MAPPINGS, "true")
                .setProperty(Environment.HBM2DDL_AUTO, "create-drop")
                .setProperty(Environment.DATASOURCE, "java:jboss/datasources/ExampleDS");

        sessionFactory = configuration
                .configure()
                .buildSessionFactory();
    }

    public void createEmployee(final int id, final String name, final String address) {
        final Employee employee = new Employee();
        employee.setAddress(address);
        employee.setName(name);

        final Session session = sessionFactory.openSession();
        session.save(employee);
        session.flush();
        session.close();
    }

    public Employee getEmployee(final int id) {
        final Session session = sessionFactory.openSession();
        final Employee employee = (Employee) session.load(Employee.class, id);
        session.close();
        return employee;
    }
}
