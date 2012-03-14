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

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.jboss.security.xacml.core.JBossPDP;
import org.jboss.security.xacml.jaxb.PDP;

/**
 * Constants and helper methods for {@link JBossPDP} tests.
 * 
 * @author Josef Cacek
 */
public abstract class JBossPDPTestUtils {

    /** The TESTOBJECTS_REQUESTS */
    public static final String TESTOBJECTS_REQUESTS = "testobjects/requests";
    /** The TESTOBJECTS_POLICIES */
    public static final String TESTOBJECTS_POLICIES = "testobjects/policies";
    /** The property name to replace in the Medical XACML request template. */
    public static final String SUBST_SUBJECT_ID = "subjectId";
    /** The MED_EXAMPLE_POLICY_SET */
    public static final String MED_EXAMPLE_POLICY_SET = "med-example-policySet.xml";
    /** The MED_EXAMPLE_POLICY */
    public static final String MED_EXAMPLE_POLICY = "med-example-policy.xml";

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link JAXBElement} instance for given {@link PDP}.
     * 
     * @param pdp
     * @return
     */
    public static JAXBElement<PDP> createJAXBElementPDP(final PDP pdp) {
        return new JAXBElement<PDP>(new QName("urn:jboss:xacml:2.0", "jbosspdp"), PDP.class, pdp);
    }

}
