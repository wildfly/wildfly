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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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
import org.hibernate.service.jta.platform.internal.JBossAppServerJtaPlatform;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.stat.Statistics;
import org.infinispan.manager.CacheContainer;

/**
 * 
 * @author Madhumita Sadhukhan
 */
@Stateful
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SFSBHibernate2LcacheStats {

    private static SessionFactory sessionFactory;
    // private static Configuration configuration;
    private static ServiceRegistryBuilder builder;
    private static ServiceRegistry serviceRegistry;
    private static Session session;

    /**
     * Lookup the Infinispan cache container to start it.
     * 
     * We also could change the following line in standalone.xml: <cache-container name="hibernate" default-cache="local-query">
     * To: <cache-container name="hibernate" default-cache="local-query" start="EAGER">
     */
    private static final String CONTAINER_JNDI_NAME = "java:jboss/infinispan/container/hibernate";
    @Resource(lookup = CONTAINER_JNDI_NAME)
    private CacheContainer container;

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void setupConfig() {
        // static {
        try {

            System.out.println("setupConfig:  Current dir = " + (new File(".")).getCanonicalPath());

            // prepare the configuration
            Configuration configuration = new Configuration().setProperty(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS,
                    "true");
            configuration.getProperties().put(AvailableSettings.JTA_PLATFORM, JBossAppServerJtaPlatform.class);
            configuration.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
            configuration.setProperty(Environment.DATASOURCE, "java:jboss/datasources/ExampleDS");
            // set property to enable statistics
            configuration.setProperty("hibernate.generate_statistics", "true");

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

        System.out.println("setupConfig: done");

    }

    // create planet
    public Planet prepareData(String planetName, String galaxyName, String starName, Set<Satellite> satellites, Integer id) {

        Session session = sessionFactory.openSession();
        Planet planet = new Planet();
        planet.setPlanetId(id);
        planet.setPlanetName(planetName);
        planet.setGalaxy(galaxyName);
        planet.setStar(starName);
        // Transaction trans = session.beginTransaction();
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
            SessionStatistics stats = session.getStatistics();
            assertEquals(2, stats.getEntityKeys().size());
            assertEquals(2, stats.getEntityCount());

            // session.flush();
            // session.close();
        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException("transactional failure while persisting planet entity", e);

        }
        // trans.commit();
        session.close();
        return planet;
    }

    // fetch planet
    public Planet getPlanet(Integer id) {
        Planet planet = (Planet) sessionFactory.openSession().get(Planet.class, id);
        return planet;
    }

    // fetch satellites
    public boolean isSatellitesPresentInCache(Integer id) {

        boolean indicator = sessionFactory.getCache().containsCollection(
                org.jboss.as.test.integration.hibernate.Planet.class.getName() + ".satellites", id);

        return indicator;
    }

    // fetch statistics
    public Statistics getStatistics() {
        Statistics sessionStats = sessionFactory.getStatistics();
        return sessionStats;
    }

    // fetch statistics after eviction of collection from cache
    public Statistics getStatisticsAfterEviction() {
        sessionFactory.getCache().evictCollection(
                org.jboss.as.test.integration.hibernate.Planet.class.getName() + ".satellites", new Integer(1));
        Statistics sessionStats = sessionFactory.getStatistics();
        return sessionStats;
    }
}
