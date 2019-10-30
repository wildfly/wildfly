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

package org.jboss.as.test.clustering.cluster.ejb.xpc.bean;

import javax.ejb.Remove;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.hibernate.Session;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Paul Ferraro
 * @author Scott Marlow
 */
@javax.ejb.Stateful(name = "StatefulBean")
public class StatefulBean implements Stateful {

    private static final Logger log = Logger.getLogger(StatefulBean.class);
    public static final String MODULE = "stateful";

    @PersistenceContext(unitName = "mypc", type = PersistenceContextType.EXTENDED)
    EntityManager em;

    String version = "initial";
    Map<String, String> valueBag = new HashMap<>();

    /**
     * Create the employee but don't commit the change to the database, instead keep it in the
     * extended persistence context.
     *
     * @param name
     * @param address
     * @param id
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void createEmployee(String name, String address, int id) {

        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        em.persist(emp);
        logStats("createEmployee");
        version = "created";
        valueBag.put("version", "created");
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Employee getEmployee(int id) {
        logStats("getEmployee " + id);
        return em.find(Employee.class, id, LockModeType.NONE);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Employee getSecondBeanEmployee(int id) {
        logStats("getSecondBeanEmployee");
        //return secondBean.getEmployee(id);
        return em.find(Employee.class, id, LockModeType.NONE);
    }

    @Override
    @Remove
    public void destroy() {
        logStats("destroy");
        version = "destroyed";
        valueBag.put("version", version);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteEmployee(int id) {
        Employee employee = em.find(Employee.class, id, LockModeType.NONE);
        em.remove(employee);
        logStats("deleteEmployee");
        version = "deletedEmployee";
        valueBag.put("version", version);
    }


    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void flush() {
        logStats("flush");
        version = "flushed";
        valueBag.put("version", version);
    }

    @Override
    public void clear() {
        em.clear();
        logStats("clear");
        version = "cleared";
        valueBag.put("version", version);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void echo(String message) {
        log.trace("echo entered for " + message);
        logStats("echo " + message);
        log.trace("echo completed for " + message);
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public int executeNativeSQL(String nativeSql) {
        logStats("executeNativeSQL");
        return em.createNativeQuery(nativeSql).executeUpdate();
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public long getEmployeesInMemory() {
        Session session = em.unwrap(Session.class);
        String[] entityRegionNames = session.getSessionFactory().getStatistics().getSecondLevelCacheRegionNames();
        for (String name : entityRegionNames) {
            if (name.contains(Employee.class.getName())) {
                SecondLevelCacheStatistics stats = session.getSessionFactory().getStatistics().getSecondLevelCacheStatistics(name);
                return stats.getElementCountInMemory();
            }
        }
        return -1;

    }

    private void logStats(String methodName) {
        Session session = em.unwrap(Session.class);
        log.trace(methodName + "(version=" + version + ", HashMap version=" + valueBag.get("version") + ") logging statistics for session = " + session);
        session.getSessionFactory().getStatistics().setStatisticsEnabled(true);
        session.getSessionFactory().getStatistics().logSummary();
        String[] entityRegionNames = session.getSessionFactory().getStatistics().getSecondLevelCacheRegionNames();
        for (String name : entityRegionNames) {
            log.trace("cache entity region name = " + name);
            SecondLevelCacheStatistics stats = session.getSessionFactory().getStatistics().getSecondLevelCacheStatistics(name);
            log.trace("2lc for " + name + ": " + stats.toString());

        }
        // we will want to return the SecondLevelCacheStatistics for Employee

    }

}
