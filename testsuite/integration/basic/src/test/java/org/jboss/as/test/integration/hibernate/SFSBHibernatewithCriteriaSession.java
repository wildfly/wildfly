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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Property;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.internal.metamodel.MetamodelImpl;
import org.hibernate.stat.Statistics;

/**
 * Test Criteria API with native Hibernate and also test that Hibernate statistics is able to fetch all kinds of queries
 *
 * @author Madhumita Sadhukhan
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSBHibernatewithCriteriaSession {

    private static SessionFactory sessionFactory;

    protected static final Class[] NO_CLASSES = new Class[0];
    protected static final String NO_MAPPINGS = new String();

    public void cleanup() {
        sessionFactory.close();
    }

    public void setupConfig() {
        // static {
        try {

            // prepare the configuration
            Configuration configuration = new Configuration().setProperty(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS,
                    "true");
            configuration.getProperties().put(AvailableSettings.JTA_PLATFORM, JBossAppServerJtaPlatform.class);
            configuration.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
            configuration.setProperty(Environment.DATASOURCE, "java:jboss/datasources/ExampleDS");
            configuration.setProperty("hibernate.listeners.envers.autoRegister", "false");

            // fetch the properties
            Properties properties = new Properties();
            configuration = configuration.configure("hibernate.cfg.xml");
            properties.putAll(configuration.getProperties());

            Environment.verifyProperties(properties);
            ConfigurationHelper.resolvePlaceHolders(properties);

            // build the serviceregistry
            StandardServiceRegistryBuilder registry = new StandardServiceRegistryBuilder().applySettings(properties);
            sessionFactory = configuration.buildSessionFactory(registry.build());

            // build metamodel
            SessionFactoryImplementor sfi = (SessionFactoryImplementor) sessionFactory;
            MetamodelImpl.buildMetamodel(configuration.getClassMappings(), sfi);

            sessionFactory.getStatistics().setStatisticsEnabled(true);

        } catch (Throwable ex) { // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            // ex.printStackTrace();
            throw new ExceptionInInitializerError(ex);
        }

    }

    // create planet
    public Planet prepareData(String planetName, String galaxyName, String starName, Set<Satellite> satellites, Integer id) {

        Session session = sessionFactory.openSession();
        Planet planet = new Planet();
        planet.setPlanetId(id);
        planet.setPlanetName(planetName);
        planet.setGalaxy(galaxyName);
        planet.setStar(starName);
        Transaction trans = session.beginTransaction();
        try {

            session.save(planet);

            if (satellites != null && satellites.size() > 0) {
                Iterator<Satellite> itrSat = satellites.iterator();
                while (itrSat.hasNext()) {
                    Satellite sat = itrSat.next();
                    session.save(sat);
                }
                planet.setSatellites(new HashSet<Satellite>());
                planet.getSatellites().addAll(satellites);

            }

            session.saveOrUpdate(planet);

            // session.flush();
            // session.close();
        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException("transactional failure while persisting planet entity", e);

        }
        trans.commit();
        // session.close();
        return planet;
    }

    // fetch HQL for Planet
    public Query fetchwithHQL() {

        Session session = sessionFactory.openSession();
        String SQL_QUERY = "from Planet planet";
        Query query = session.createQuery(SQL_QUERY);
        for (Iterator it = query.iterate(); it.hasNext();) {
            Planet planet = (Planet) it.next();
            System.out.println("ID from HQL: " + planet.getPlanetId());
        }

        return query;
    }

    // fetch Criteria Query for Planet
    public List fetchwithCriteria() {

        Session session = sessionFactory.openSession();
        List query = session.createCriteria(Planet.class).add(Property.forName("planetName").like("E%")).list();
        return query;
    }

    // fetch statistics
    public Statistics getStatistics() {
        Statistics sessionStats = sessionFactory.getStatistics();
        return sessionStats;
    }

}
