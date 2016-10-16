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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.security.xacml.core.JBossPDP;
import org.jboss.security.xacml.core.model.context.ActionType;
import org.jboss.security.xacml.core.model.context.AttributeType;
import org.jboss.security.xacml.core.model.context.EnvironmentType;
import org.jboss.security.xacml.core.model.context.RequestType;
import org.jboss.security.xacml.core.model.context.ResourceType;
import org.jboss.security.xacml.core.model.context.SubjectType;
import org.jboss.security.xacml.factories.RequestAttributeFactory;
import org.jboss.security.xacml.factories.RequestResponseContextFactory;
import org.jboss.security.xacml.interfaces.PolicyDecisionPoint;
import org.jboss.security.xacml.interfaces.RequestContext;
import org.jboss.security.xacml.interfaces.XACMLConstants;
import org.jboss.security.xacml.jaxb.LocatorType;
import org.jboss.security.xacml.jaxb.LocatorsType;
import org.jboss.security.xacml.jaxb.PDP;
import org.jboss.security.xacml.jaxb.PoliciesType;
import org.jboss.security.xacml.jaxb.PolicySetType;
import org.jboss.security.xacml.locators.JBossPolicySetLocator;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testcases which test JBossPDP initialization a validates XACML Interoperability Use Cases. .
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
public class JBossPDPInteroperabilityTestCase {

    private static Logger LOGGER = Logger.getLogger(JBossPDPInteroperabilityTestCase.class);

    @Inject
    JBossPDPServiceBean pdpServiceBean;

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link JavaArchive} for the deployment.
     *
     * @return
     */
    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "pdp-service-bean.jar");
        jar.addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
        XACMLTestUtils.addCommonClassesToArchive(jar);
        XACMLTestUtils.addJBossDeploymentStructureToArchive(jar);
        XACMLTestUtils.addXACMLPoliciesToArchive(jar);

        //we need this because of "in-container" testing
        for (int i = 1; i <= 7; i++) {
            jar.addAsResources(JBossPDPServletInitializationTestCase.class.getPackage(), XACMLTestUtils.TESTOBJECTS_REQUESTS
                    + "/scenario2-testcase" + i + "-request.xml");
        }
        jar.addAsResource(JBossPDPServletInitializationTestCase.class.getPackage(), XACMLTestUtils.TESTOBJECTS_REQUESTS
                + "/med-example-request.xml");

        return jar;
    }

    /**
     * Validates the 7 Oasis XACML Interoperability Use Cases.
     *
     * @throws Exception
     */
    @Test
    public void testInteropTestWithXMLRequests() throws Exception {
        assertNotNull("PDPServiceBean should be injected.", pdpServiceBean);
        final PolicyDecisionPoint pdp = pdpServiceBean.getJBossPDP();
        assertNotNull("JBossPDP should be not-null", pdp);
        assertEquals("Case 1 should be deny", XACMLConstants.DECISION_DENY, getDecision(pdp, "scenario2-testcase1-request.xml"));
        assertEquals("Case 2 should be permit", XACMLConstants.DECISION_PERMIT,
                getDecision(pdp, "scenario2-testcase2-request.xml"));
        assertEquals("Case 3 should be permit", XACMLConstants.DECISION_PERMIT,
                getDecision(pdp, "scenario2-testcase3-request.xml"));
        assertEquals("Case 4 should be deny", XACMLConstants.DECISION_DENY, getDecision(pdp, "scenario2-testcase4-request.xml"));
        assertEquals("Case 5 should be deny", XACMLConstants.DECISION_DENY, getDecision(pdp, "scenario2-testcase5-request.xml"));
        assertEquals("Case 6 should be deny", XACMLConstants.DECISION_DENY, getDecision(pdp, "scenario2-testcase6-request.xml"));
        assertEquals("Case 7 should be permit", XACMLConstants.DECISION_PERMIT,
                getDecision(pdp, "scenario2-testcase7-request.xml"));
    }

    /**
     * Tests PDP evaluation of XACML requests provided as the objects (Oasis XACML Interoperability Use Cases).
     *
     * @throws Exception
     */
    @Test
    public void testInteropTestWithObjects() throws Exception {
        assertNotNull("PDPServiceBean should be injected.", pdpServiceBean);
        final PolicyDecisionPoint pdp = pdpServiceBean.getJBossPDP();
        assertNotNull("JBossPDP should be not-null", pdp);
        assertEquals("Case 1 should be deny", XACMLConstants.DECISION_DENY,
                getDecision(pdp, getRequestContext("false", "false", 10)));
        assertEquals("Case 2 should be permit", XACMLConstants.DECISION_PERMIT,
                getDecision(pdp, getRequestContext("false", "false", 1)));
        assertEquals("Case 3 should be permit", XACMLConstants.DECISION_PERMIT,
                getDecision(pdp, getRequestContext("true", "false", 5)));
        assertEquals("Case 4 should be deny", XACMLConstants.DECISION_DENY,
                getDecision(pdp, getRequestContext("false", "false", 9)));
        assertEquals("Case 5 should be deny", XACMLConstants.DECISION_DENY,
                getDecision(pdp, getRequestContext("true", "false", 10)));
        assertEquals("Case 6 should be deny", XACMLConstants.DECISION_DENY,
                getDecision(pdp, getRequestContext("true", "false", 15)));
        assertEquals("Case 7 should be permit", XACMLConstants.DECISION_PERMIT,
                getDecision(pdp, getRequestContext("true", "true", 10)));
    }

    /**
     * Tests loading XACML policies from a filesystem folder.
     *
     * @throws Exception
     */
    @Test
    public void testPoliciesLoadedFromDir() throws Exception {
        //create temporary folder for policies
        final File policyDir = new File("test-JBossPDP-Med-" + System.currentTimeMillis());
        final InputStream requestIS = getClass().getResourceAsStream(
                XACMLTestUtils.TESTOBJECTS_REQUESTS + "/med-example-request.xml");
        try {
            policyDir.mkdirs();
            final JBossPDP pdp = createPDPForMed(policyDir);
            final String requestTemplate = IOUtils.toString(requestIS, "UTF-8");
            LOGGER.trace("REQUEST template: " + requestTemplate);
            final Map<String, Object> substitutionMap = new HashMap<String, Object>();

            substitutionMap.put(XACMLTestUtils.SUBST_SUBJECT_ID, "josef@med.example.com");
            assertEquals("Decision for josef@med.example.com should be DECISION_PERMIT", XACMLConstants.DECISION_PERMIT,
                    getDecisionForStr(pdp, StrSubstitutor.replace(requestTemplate, substitutionMap)));

            substitutionMap.put(XACMLTestUtils.SUBST_SUBJECT_ID, "guest@med.example.com");
            assertEquals("Decision for guest@med.example.com should be DECISION_DENY", XACMLConstants.DECISION_DENY,
                    getDecisionForStr(pdp, StrSubstitutor.replace(requestTemplate, substitutionMap)));

            substitutionMap.put(XACMLTestUtils.SUBST_SUBJECT_ID, "hs@simpsons.com");
            assertEquals("Decision for hs@simpsons.com should be DECISION_DENY", XACMLConstants.DECISION_DENY,
                    getDecisionForStr(pdp, StrSubstitutor.replace(requestTemplate, substitutionMap)));

            substitutionMap.put(XACMLTestUtils.SUBST_SUBJECT_ID, "bs@simpsons.com");
            assertEquals("Decision for bs@simpsons.com should be DECISION_NOT_APPLICABLE",
                    XACMLConstants.DECISION_NOT_APPLICABLE,
                    getDecisionForStr(pdp, StrSubstitutor.replace(requestTemplate, substitutionMap)));

            substitutionMap.put(XACMLTestUtils.SUBST_SUBJECT_ID, "admin@acme.com");
            assertEquals("Decision for admin@acme.com should be DECISION_NOT_APPLICABLE",
                    XACMLConstants.DECISION_NOT_APPLICABLE,
                    getDecisionForStr(pdp, StrSubstitutor.replace(requestTemplate, substitutionMap)));

        } finally {
            FileUtils.deleteDirectory(policyDir);
            requestIS.close();
        }
    }

    // Private methods -------------------------------------------------------

    /**
     * Creates a {@link JBossPDP} instance filled with policies from Medical example (loaded from a directory in the
     * filesystem).
     *
     * @param policyDir
     * @return
     * @throws IOException
     */
    private JBossPDP createPDPForMed(final File policyDir) throws IOException {
        final File policySetFile = new File(policyDir, XACMLTestUtils.MED_EXAMPLE_POLICY_SET);
        final File policySetFile2 = new File(policyDir, XACMLTestUtils.MED_EXAMPLE_POLICY_SET2);

        //copy policy files to the temporary folder
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Copying policies to the " + policyDir.getAbsolutePath());
        }
        FileUtils.copyInputStreamToFile(
                getClass().getResourceAsStream(
                        XACMLTestUtils.TESTOBJECTS_POLICIES + "/" + XACMLTestUtils.MED_EXAMPLE_POLICY_SET), policySetFile);
        FileUtils.copyInputStreamToFile(
                getClass().getResourceAsStream(
                        XACMLTestUtils.TESTOBJECTS_POLICIES + "/" + XACMLTestUtils.MED_EXAMPLE_POLICY_SET2), policySetFile2);

        //create XML configuration for the PDP
        final PDP pdp = new PDP();
        final PoliciesType policies = new PoliciesType();
        final PolicySetType policySet = new PolicySetType();
        policySet.setLocation(policyDir.toURI().getPath());
        policies.getPolicySet().add(policySet);
        pdp.setPolicies(policies);
        final LocatorType locator = new LocatorType();
        locator.setName(JBossPolicySetLocator.class.getName());
        final LocatorsType locators = new LocatorsType();
        locators.getLocator().add(locator);
        pdp.setLocators(locators);

        return new JBossPDP(new JAXBElement<PDP>(new QName("urn:jboss:xacml:2.0", "jbosspdp"), PDP.class, pdp));
    }

    /**
     * Get the decision from the given PDP for the given request string (XML as a String).
     *
     * @param pdp
     * @param requestStr
     * @return
     * @throws Exception
     */
    private int getDecisionForStr(PolicyDecisionPoint pdp, String requestStr) throws Exception {
        final RequestContext request = RequestResponseContextFactory.createRequestCtx();
        request.readRequest(IOUtils.toInputStream(requestStr));
        return getDecision(pdp, request);
    }

    /**
     * Get the decision from the PDP.
     *
     * @param pdp
     * @param requestFileLoc a file where the xacml request is stored
     * @return
     * @throws Exception
     */
    private int getDecision(PolicyDecisionPoint pdp, String requestFileLoc) throws Exception {
        final RequestContext request = RequestResponseContextFactory.createRequestCtx();
        LOGGER.trace("Creating request from " + requestFileLoc);
        final InputStream requestStream = JBossPDPInteroperabilityTestCase.class
                .getResourceAsStream(XACMLTestUtils.TESTOBJECTS_REQUESTS + "/" + requestFileLoc);
        if (requestStream == null) {
            LOGGER.warn("INPUT IS NULL");
        }
        request.readRequest(requestStream);
        return getDecision(pdp, request);
    }

    /**
     * Gets the decision from the PDP.
     *
     * @param pdp
     * @param request
     * @return
     */
    private static int getDecision(PolicyDecisionPoint pdp, RequestContext request) {
        return pdp.evaluate(request).getDecision();
    }

    /**
     * Creates a single XACML request (instance of {@link RequestType}) with given parameters (subject's attribute values).
     *
     * @param reqTradeAppr
     * @param reqCreditAppr
     * @param buyPrice
     * @return
     * @throws Exception
     */
    private RequestContext getRequestContext(String reqTradeAppr, String reqCreditAppr, int buyPrice) throws Exception {
        final RequestType request = new RequestType();
        request.getSubject().add(createSubject(reqTradeAppr, reqCreditAppr, buyPrice));
        request.getResource().add(createResource());
        request.setAction(createAction());
        request.setEnvironment(new EnvironmentType());

        final RequestContext requestCtx = RequestResponseContextFactory.createRequestCtx();
        requestCtx.setRequest(request);

        return requestCtx;
    }

    /**
     * Creates a {@link SubjectType} with given attribute values. Some of the attribute values (as "subject-id" for instance)
     * are fixed.
     *
     * @param reqTradeAppr
     * @param reqCreditAppr
     * @param buyPrice
     * @return
     */
    private SubjectType createSubject(String reqTradeAppr, String reqCreditAppr, int buyPrice) {
        // Create a subject type
        SubjectType subject = new SubjectType();
        subject.setSubjectCategory("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
        // create the subject attributes
        AttributeType attSubjectID = RequestAttributeFactory.createStringAttributeType(
                "urn:oasis:names:tc:xacml:1.0:subject:subject-id", "xacml20.interop.com", "123456");
        subject.getAttribute().add(attSubjectID);

        AttributeType attUserName = RequestAttributeFactory.createStringAttributeType(
                "urn:xacml:2.0:interop:example:subject:user-name", "xacml20.interop.com", "John Smith");
        subject.getAttribute().add(attUserName);

        AttributeType attBuyNumShares = RequestAttributeFactory.createIntegerAttributeType(
                "urn:xacml:2.0:interop:example:subject:buy-num-shares", "xacml20.interop.com", 1000);
        subject.getAttribute().add(attBuyNumShares);

        AttributeType attBuyOfferShare = RequestAttributeFactory.createIntegerAttributeType(
                "urn:xacml:2.0:interop:example:subject:buy-offer-price", "xacml20.interop.com", buyPrice);
        subject.getAttribute().add(attBuyOfferShare);

        AttributeType attRequestExtCred = RequestAttributeFactory.createStringAttributeType(
                "urn:xacml:2.0:interop:example:subject:req-credit-ext-approval", "xacml20.interop.com", reqCreditAppr);
        subject.getAttribute().add(attRequestExtCred);

        AttributeType attRequestTradeApproval = RequestAttributeFactory.createStringAttributeType(
                "urn:xacml:2.0:interop:example:subject:req-trade-approval", "xacml20.interop.com", reqTradeAppr);
        subject.getAttribute().add(attRequestTradeApproval);

        return subject;
    }

    /**
     * Creates a {@link ResourceType} with several attributes.
     *
     * @return
     */
    private ResourceType createResource() {
        ResourceType resourceType = new ResourceType();

        AttributeType attResourceID = RequestAttributeFactory.createStringAttributeType(
                "urn:oasis:names:tc:xacml:1.0:resource:resource-id", "xacml20.interop.com", "CustomerAccount");
        resourceType.getAttribute().add(attResourceID);

        AttributeType attOwnerID = RequestAttributeFactory.createStringAttributeType(
                "urn:xacml:2.0:interop:example:resource:owner-id", "xacml20.interop.com", "123456");
        resourceType.getAttribute().add(attOwnerID);

        AttributeType attOwnerName = RequestAttributeFactory.createStringAttributeType(
                "urn:xacml:2.0:interop:example:resource:owner-name", "xacml20.interop.com", "John Smith");
        resourceType.getAttribute().add(attOwnerName);

        AttributeType attAccountStatus = RequestAttributeFactory.createStringAttributeType(
                "urn:xacml:2.0:interop:example:resource:account-status", "xacml20.interop.com", "Active");
        resourceType.getAttribute().add(attAccountStatus);

        AttributeType attCreditLine = RequestAttributeFactory.createIntegerAttributeType(
                "urn:xacml:2.0:interop:example:resource:credit-line", "xacml20.interop.com", 15000);
        resourceType.getAttribute().add(attCreditLine);

        AttributeType attCurrentCredit = RequestAttributeFactory.createIntegerAttributeType(
                "urn:xacml:2.0:interop:example:resource:current-credit", "xacml20.interop.com", 10000);
        resourceType.getAttribute().add(attCurrentCredit);

        AttributeType attTradeLimit = RequestAttributeFactory.createIntegerAttributeType(
                "urn:xacml:2.0:interop:example:resource:trade-limit", "xacml20.interop.com", 10000);
        resourceType.getAttribute().add(attTradeLimit);
        return resourceType;
    }

    /**
     * Creates a simple {@link ActionType} with a single attribute - action-id=Buy.
     *
     * @return
     */
    private ActionType createAction() {
        final ActionType actionType = new ActionType();
        final AttributeType attActionID = RequestAttributeFactory.createStringAttributeType(
                "urn:oasis:names:tc:xacml:1.0:action:action-id", "xacml20.interop.com", "Buy");
        actionType.getAttribute().add(attActionID);
        return actionType;
    }
}