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

import org.jboss.logging.Logger;
import org.jboss.security.xacml.core.JBossPDP;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.jboss.shrinkwrap.api.container.ResourceContainer;

/**
 * Constants and helper methods for {@link JBossPDP} and XACMLAuthorizationModule tests.
 *
 * @author Josef Cacek
 */
public abstract class XACMLTestUtils {

    private static Logger LOGGER = Logger.getLogger(XACMLTestUtils.class);

    /**
     * The TESTOBJECTS_REQUESTS
     */
    public static final String TESTOBJECTS_REQUESTS = "testobjects/requests";
    /**
     * The TESTOBJECTS_POLICIES
     */
    public static final String TESTOBJECTS_POLICIES = "testobjects/policies";
    /**
     * The TESTOBJECTS_CONFIG
     */
    public static final String TESTOBJECTS_CONFIG = "testobjects/config";
    /**
     * The property name to replace in the Medical XACML request template.
     */
    public static final String SUBST_SUBJECT_ID = "subjectId";
    /**
     * The MED_EXAMPLE_POLICY_SET
     */
    public static final String MED_EXAMPLE_POLICY_SET = "med-example-policySet.xml";
    /**
     * The MED_EXAMPLE_POLICY
     */
    public static final String MED_EXAMPLE_POLICY_SET2 = "med-example-policySet2.xml";

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
                TESTOBJECTS_POLICIES + "/xacml-policySet.xml", //
                TESTOBJECTS_POLICIES + "/xacml-policy2.xml",//
                TESTOBJECTS_POLICIES + "/xacml-policy3.xml", //
                TESTOBJECTS_POLICIES + "/xacml-policy4.xml", //
                TESTOBJECTS_POLICIES + "/xacml-policy5.xml");
        rc.addAsResources(JBossPDPServletInitializationTestCase.class.getPackage(), //
                TESTOBJECTS_POLICIES + "/" + MED_EXAMPLE_POLICY_SET, //
                TESTOBJECTS_POLICIES + "/" + MED_EXAMPLE_POLICY_SET2);
    }

    /**
     * Adds common classes to the given archive.
     *
     * @param cc
     */
    protected static <T extends Archive<T>> void addCommonClassesToArchive(final ClassContainer<T> cc) {
        LOGGER.debug("Adding common classes to the Archive");
        cc.addClasses(JBossPDPServiceBean.class, XACMLTestUtils.class);
    }

}