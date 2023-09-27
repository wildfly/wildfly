/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.basic.multiplepersistenceunittest;

import java.util.Map;
import jakarta.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.jpa.basic.SLSBAmbiguousPU;
import org.jboss.as.test.integration.jpa.basic.SLSBPU1;
import org.jboss.as.test.integration.jpa.basic.SLSBPU2;
import org.jboss.as.test.integration.jpa.basic.SLSBPersistenceContexts;
import org.jboss.as.test.integration.jpa.basic.SLSBPersistenceUnits;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Ensure that both pu definitions can be used.
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class MultiplePuTestCase {

    private static final String ARCHIVE_NAME = "MultiplePuTestCase";

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(MultiplePuTestCase.class, SLSBPU1.class, SLSBPU2.class, SLSBPersistenceContexts.class, SLSBPersistenceUnits.class, SLSBAmbiguousPU.class);
        jar.addAsManifestResource(MultiplePuTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @EJB(mappedName = "java:global/" + ARCHIVE_NAME + "/SLSBPU1")
    private SLSBPU1 slsbpu1;

    @EJB(mappedName = "java:global/" + ARCHIVE_NAME + "/SLSBPU2")
    private SLSBPU2 slsbpu2;

    @EJB(mappedName = "java:global/" + ARCHIVE_NAME + "/SLSBAmbiguousPU")
    private SLSBAmbiguousPU slsbAmbiguousPU;

    @EJB(mappedName = "java:global/" + ARCHIVE_NAME + "/SLSBPersistenceContexts")
    private SLSBPersistenceContexts slsbPersistenceContexts;

    @EJB(mappedName = "java:global/" + ARCHIVE_NAME + "/SLSBPersistenceUnits")
    private SLSBPersistenceUnits slsbPersistenceUnits;

    @Test
    public void testBothPersistenceUnitDefinitions() throws Exception {
        Map<String, Object> sl1Props = slsbpu1.getEMInfo();
        Map<String, Object> sl2Props = slsbpu2.getEMInfo();
        Map<String, Object> slsbAmbiguousPUEMInfo = slsbAmbiguousPU.getEMInfo();

        Assert.assertEquals("wrong pu ", sl1Props.get("PersistenceUnitName"), "pu1");
        Assert.assertEquals("wrong pu ", sl2Props.get("PersistenceUnitName"), "pu2");
        // pu2 is the default pu
        Assert.assertEquals("wrong pu ", slsbAmbiguousPUEMInfo.get("PersistenceUnitName"), "pu2");
    }

    /**
     * test that jakarta.persistence.PersistenceContexts binds two persistence contexts (pu1 + pu2) to
     * the SLSBBothPUs class, which we lookup from JNDI and use to get the persistence unit properties.
     * Get the "PersistenceUnitName" property that we added in persistence.xml to verify that the expected
     * persistence unit is used.
     *
     * @throws Exception
     */
    @Test
    public void testPersistenceContextsAnnotation() throws Exception {
        Map<String, Object> sl1Props = slsbPersistenceContexts.getPU1Info();
        Map<String, Object> sl2Props = slsbPersistenceContexts.getPU2Info();

        Assert.assertEquals("wrong pu ", sl1Props.get("PersistenceUnitName"), "pu1");
        Assert.assertEquals("wrong pu ", sl2Props.get("PersistenceUnitName"), "pu2");
    }

    /**
     * test that jakarta.persistence.PersistenceUnits binds two persistence units (pu1 + pu2) to
     * the SLSBBothPUs class, which we lookup from JNDI and use to get the persistence unit properties.
     * Get the "PersistenceUnitName" property that we added in persistence.xml to verify that the expected
     * persistence unit is used.
     *
     * @throws Exception
     */
    @Test
    public void testPersistenceUnitsAnnotation() throws Exception {
        Map<String, Object> sl1Props = slsbPersistenceUnits.getPU1Info();
        Map<String, Object> sl2Props = slsbPersistenceUnits.getPU2Info();

        Assert.assertEquals("wrong pu ", sl1Props.get("PersistenceUnitName"), "pu1");
        Assert.assertEquals("wrong pu ", sl2Props.get("PersistenceUnitName"), "pu2");
    }

}
