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
package org.jboss.as.test.integration.security.xacml;

import org.apache.log4j.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.jboss.shrinkwrap.api.container.ResourceContainer;

/**
 * An abstract parent for JBossPDP tests.
 * 
 * @author Josef Cacek
 */
public abstract class AbstractJBossPDPTest {

    private static Logger LOGGER = Logger.getLogger(AbstractJBossPDPTest.class);

    // Protected methods -----------------------------------------------------

    /**
     * Adds jboss-deployment-structure.xml configuration file which enables "org.jboss.security.xacml" module to the given
     * archive.
     * 
     * @param mc
     */
    protected static <T extends Archive<T>> void addJBossDeploymentStructureToArchive(final ManifestContainer<T> mc) {
        LOGGER.debug("Adding jboss-deployment-structure.xml to the Archive");
        mc.addAsManifestResource(new StringAsset("<jboss-deployment-structure><deployment><dependencies>" //
                + "<module name='org.jboss.security.xacml'/>" //
                + "<module name='org.apache.commons.lang'/>" //
                + "<module name='org.apache.commons.io'/>" //
                + "</dependencies></deployment></jboss-deployment-structure>"), //
                "jboss-deployment-structure.xml");
    }

    /**
     * Adds XACML policies from Oasis XACML Interoperability use-cases to the given archive.
     * 
     * @param rc
     */
    protected static <T extends Archive<T>> void addXACMLPoliciesToArchive(final ResourceContainer<T> rc) {
        LOGGER.debug("Adding files with XACML policies to the Archive");
        rc.addAsResources(JBossPDPServletInitializationTestCase.class.getPackage(),// 
                JBossPDPTestUtils.TESTOBJECTS_POLICIES + "/xacml-policySet.xml", // 
                JBossPDPTestUtils.TESTOBJECTS_POLICIES + "/xacml-policy2.xml",//
                JBossPDPTestUtils.TESTOBJECTS_POLICIES + "/xacml-policy3.xml", //
                JBossPDPTestUtils.TESTOBJECTS_POLICIES + "/xacml-policy4.xml", //
                JBossPDPTestUtils.TESTOBJECTS_POLICIES + "/xacml-policy5.xml");
        rc.addAsResources(JBossPDPServletInitializationTestCase.class.getPackage(), //
                JBossPDPTestUtils.TESTOBJECTS_POLICIES + "/med-example-policySet.xml", //
                JBossPDPTestUtils.TESTOBJECTS_POLICIES + "/med-example-policy.xml");
    }

    /**
     * Adds common classes to the given archive.
     * 
     * @param rc
     */
    protected static <T extends Archive<T>> void addCommonClassesToArchive(final ClassContainer<T> cc) {
        LOGGER.debug("Adding common classes to the Archive");
        cc.addClasses(JBossPDPServiceBean.class, JBossPDPTestUtils.class, AbstractJBossPDPTest.class);
    }

}
