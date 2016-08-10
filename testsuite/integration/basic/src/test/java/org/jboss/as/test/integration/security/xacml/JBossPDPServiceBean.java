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

import java.io.IOException;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.jboss.security.xacml.core.JBossPDP;
import org.jboss.security.xacml.jaxb.LocatorType;
import org.jboss.security.xacml.jaxb.LocatorsType;
import org.jboss.security.xacml.jaxb.PDP;
import org.jboss.security.xacml.jaxb.PoliciesType;
import org.jboss.security.xacml.jaxb.PolicySetType;
import org.jboss.security.xacml.jaxb.PolicyType;
import org.jboss.security.xacml.locators.JBossPolicySetLocator;

/**
 * Helper class, which initializes a {@link JBossPDP} instance with policies from Oasis XACML Interoperability use-cases.
 *
 * @author Josef Cacek
 */
public class JBossPDPServiceBean {

    private final JBossPDP jbossPDP;

    // Constructors ----------------------------------------------------------

    /**
     * Create a new {@link JBossPDPServiceBean}.
     *
     * @throws IOException
     */
    public JBossPDPServiceBean() {
        super();
        jbossPDP = createPDPForInterop();
    }

    // Public methods --------------------------------------------------------

    /**
     * Get the PDP for Interop tests.
     *
     * @return the JBossPDP instance.
     */
    public JBossPDP getJBossPDP() {
        return jbossPDP;
    }

    // Private methods -------------------------------------------------------

    /**
     * Creates a {@link JBossPDP} instance filled with policies from XACML interoperability tests.
     *
     * @return
     */
    private JBossPDP createPDPForInterop() {
        final String policiesPath = getClass().getPackage().getName().replace('.', '/') + "/"
                + XACMLTestUtils.TESTOBJECTS_POLICIES;
        //create XML configuration for the JBossPDP (as a JAXBElement)
        PDP pdp = new PDP();
        final PoliciesType policies = new PoliciesType();
        final PolicySetType policySet = new PolicySetType();
        policySet.setLocation(policiesPath + "/xacml-policySet.xml");
        for (short i = 2; i <= 5; i++) {
            final PolicyType policy = new PolicyType();
            policy.setLocation(policiesPath + "/xacml-policy" + i + ".xml");
            policySet.getPolicy().add(policy);
        }
        policies.getPolicySet().add(policySet);
        pdp.setPolicies(policies);
        final LocatorType locator = new LocatorType();
        locator.setName(JBossPolicySetLocator.class.getName());
        final LocatorsType locators = new LocatorsType();
        locators.getLocator().add(locator);
        pdp.setLocators(locators);
        return new JBossPDP(new JAXBElement<PDP>(new QName("urn:jboss:xacml:2.0", "jbosspdp"), PDP.class, pdp));
    }
}