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
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.hibernate.Query;
import org.hibernate.stat.Statistics;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test Criteria API with native Hibernate and also test that Hibernate statistics is able to fetch all kinds of queries
 * 
 * 
 * @author Madhumita Sadhukhan
 */
@RunWith(Arquillian.class)
public class Hibernate4NativeSetupwithCriteriaTestCase {

    private static final String ARCHIVE_NAME = "hibernate4nativemetadata_test";

    public static final String hibernate_cfg = "<?xml version='1.0' encoding='utf-8'?>"
            + "<!DOCTYPE hibernate-configuration PUBLIC " + "\"//Hibernate/Hibernate Configuration DTD 3.0//EN\" "
            + "\"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd\">"
            + "<hibernate-configuration><session-factory>" + "<property name=\"show_sql\">true</property>"
            + "<property name=\"current_session_context_class\">thread</property>"
            + "<mapping resource=\"testmapping.hbm.xml\"/>" + "</session-factory></hibernate-configuration>";

    public static final String testmapping = "<?xml version=\"1.0\"?>" + "<!DOCTYPE hibernate-mapping PUBLIC "
            + "\"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" " + "\"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd\">"
            + "<hibernate-mapping package=\"org.jboss.as.test.integration.hibernate\">"
            + "<class name=\"org.jboss.as.test.integration.hibernate.Planet\" table=\"PLANET\">"
            + "<id name=\"planetId\" column=\"planetId\">" + "<generator class=\"native\"/>" + "</id>"
            + "<property name=\"planetName\" column=\"planet_name\"/>" + "<property name=\"galaxy\" column=\"galaxy_name\"/>"
            + "<property name=\"star\" column=\"star_name\"/>" + "<set name=\"satellites\">" + "<key column=\"id\"/>"
            + "<one-to-many class=\"org.jboss.as.test.integration.hibernate.Satellite\"/>" + "</set>" + "</class>"
            + "<class name=\"org.jboss.as.test.integration.hibernate.Satellite\" table=\"SATELLITE\">" + "<id name=\"id\">"
            + "<generator class=\"native\"/>" + "</id>" + "<property name=\"name\" column=\"satellite_name\"/>"
            + "</class></hibernate-mapping>";

    @ArquillianResource
    private static InitialContext iniCtx;

    @BeforeClass
    public static void beforeClass() throws NamingException {
        iniCtx = new InitialContext();
    }

    @Deployment
    public static Archive<?> deploy() throws Exception {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");
        // add required jars as manifest dependencies
        ear.addAsManifestResource(new StringAsset("Dependencies: org.hibernate \n"), "MANIFEST.MF");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "beans.jar");
        lib.addClasses(SFSBHibernatewithCriteriaSession.class);
        ear.addAsModule(lib);

        lib = ShrinkWrap.create(JavaArchive.class, "entities.jar");
        lib.addClasses(Planet.class);
        lib.addClasses(Satellite.class);
        lib.addAsResource(new StringAsset(testmapping), "testmapping.hbm.xml");
        lib.addAsResource(new StringAsset(hibernate_cfg), "hibernate.cfg.xml");
        ear.addAsLibraries(lib);

        final WebArchive main = ShrinkWrap.create(WebArchive.class, "main.war");
        main.addClasses(Hibernate4NativeSetupwithCriteriaTestCase.class);
        ear.addAsModule(main);

        // add application dependency on H2 JDBC driver, so that the Hibernate classloader (same as app classloader)
        // will see the H2 JDBC driver.
        // equivalent hack for use of shared Hibernate module, would be to add the H2 dependency directly to the
        // shared Hibernate module.
        // also add dependency on org.slf4j
        ear.addAsManifestResource(new StringAsset("<jboss-deployment-structure>" + " <deployment>" + " <dependencies>"
                + " <module name=\"com.h2database.h2\" />" + " <module name=\"org.slf4j\"/>" + " </dependencies>"
                + " </deployment>" + "</jboss-deployment-structure>"), "jboss-deployment-structure.xml");

        return ear;
    }

    protected static <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        try {
            return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + "beans/" + beanName + "!"
                    + interfaceType.getName()));
        } catch (NamingException e) {
            dumpJndi("");
            throw e;
        }
    }

    // TODO: move this logic to a common base class (might be helpful for writing new tests)
    private static void dumpJndi(String s) {
        try {
            dumpTreeEntry(iniCtx.list(s), s);
        } catch (NamingException ignore) {
        }
    }

    private static void dumpTreeEntry(NamingEnumeration<NameClassPair> list, String s) throws NamingException {
        System.out.println("\ndump " + s);
        while (list.hasMore()) {
            NameClassPair ncp = list.next();
            System.out.println(ncp.toString());
            if (s.length() == 0) {
                dumpJndi(ncp.getName());
            } else {
                dumpJndi(s + "/" + ncp.getName());
            }
        }
    }

    @Test
    public void testMetaData() throws Exception {
        SFSBHibernatewithCriteriaSession sfsb = lookup("SFSBHibernatewithCriteriaSession",
                SFSBHibernatewithCriteriaSession.class);
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        Set<Satellite> satellites1 = new HashSet<Satellite>();
        Satellite sat = new Satellite();
        sat.setId(new Integer(1));
        sat.setName("MOON");
        satellites1.add(sat);

        Set<Satellite> satellites2 = new HashSet<Satellite>();
        Satellite sat2 = new Satellite();
        sat2.setId(new Integer(2));
        sat2.setName("TRITON");
        satellites2.add(sat2);

        Planet s1 = sfsb.prepareData("EARTH", "MILKY WAY", "SUN", satellites1, new Integer(1));
        Planet s2 = sfsb.prepareData("NEPTUNE", "MILKY WAY", "SUN", satellites2, new Integer(2));

        Query data = sfsb.fetchwithHQL();
        assertNotNull(data);

        List critdata = sfsb.fetchwithCriteria();

        for (Iterator it = critdata.iterator(); it.hasNext();) {
            Planet planet = (Planet) it.next();
            assertEquals("EARTH", planet.getPlanetName());
        }

        // fetch statistics

        Statistics stats = sfsb.getStatistics();

        // fetch queries from statistics
        String[] queryList = stats.getQueries();

        // test list of queries obtained from statistics
        for (int i = 0; i < queryList.length; i++) {

            System.out.println("Query obtained from statistics::" + queryList[i]);

        }
    }
}
