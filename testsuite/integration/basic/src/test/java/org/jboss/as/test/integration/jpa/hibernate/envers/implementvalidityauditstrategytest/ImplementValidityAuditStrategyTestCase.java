/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.hibernate.envers.implementvalidityauditstrategytest;

import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.envers.DefaultRevisionEntity;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jpa.hibernate.envers.Organization;
import org.jboss.as.test.integration.jpa.hibernate.envers.SLSBValidityStrategyOrg;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This testcase verifies whether Envers/Auditing functions properly when implemented with Validity Audit Strategy
 *
 * @author Madhumita Sadhukhan
 */
@ExtendWith(ArquillianExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ImplementValidityAuditStrategyTestCase {
    private static final String ARCHIVE_NAME = "jpa_ImplementValidityAuditStrategyTestCase";

    @ArquillianResource
    private static InitialContext iniCtx;

    @BeforeAll
    public static void beforeClass() throws NamingException {

        iniCtx = new InitialContext();
    }

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(Organization.class, SLSBValidityStrategyOrg.class);
        jar.addAsManifestResource(ImplementValidityAuditStrategyTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    protected static <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType
                .cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    @Order(1)
    public void testEnversforValidityStrategy() throws Exception {

        SLSBValidityStrategyOrg slsbvalidityOrg = lookup("SLSBValidityStrategyOrg", SLSBValidityStrategyOrg.class);

        Organization o1 = slsbvalidityOrg.createOrg("REDHAT", "Software Co", "10/10/1994", "eternity", "Raleigh");
        Organization o2 = slsbvalidityOrg.createOrg("HALDIRAMS", "Food Co", "10/10/1974", "eternity", "Delhi");
        o2.setStartDate("10/10/1924");
        o2.setName("BIKANER");

        slsbvalidityOrg.updateOrg(o2);

        Organization ret1 = slsbvalidityOrg.retrieveOldOrgbyId(o2.getId());
        // check that property startDate is audited
        Assertions.assertEquals("10/10/1974", ret1.getStartDate());
        Assertions.assertEquals("HALDIRAMS", ret1.getName());
        // check that property location is notaudited
        Assertions.assertNull(ret1.getLocation());

    }

    @Test
    @Order(2)
    public void testValidityStrategyActivationforEnvers() throws Exception {
        SLSBValidityStrategyOrg slsbvalidityOrg = lookup("SLSBValidityStrategyOrg", SLSBValidityStrategyOrg.class);

        Organization o1 = slsbvalidityOrg.createOrg("REDHAT", "Software Co", "10/10/1994", "eternity", "Raleigh");
        Organization o2 = slsbvalidityOrg.createOrg("HALDIRAMS", "Food Co", "10/10/1974", "eternity", "Delhi");
        o2.setStartDate("10/10/1924");
        o2.setName("BIKANER");

        slsbvalidityOrg.updateOrg(o2);

        // check if REV END Date is populated

        List<Map<String, Object>> orgHistory = slsbvalidityOrg.verifyEndRevision(new Integer(o2.getId()));

        for (Map<String, Object> revisionEntity : orgHistory) {

            Assertions.assertNotNull(revisionEntity);

            Date revendTimestamp = (Date) revisionEntity.get("REVEND_VALIDITY");
            DefaultRevisionEntity revEnd = (DefaultRevisionEntity) revisionEntity.get("REVEND");

            if (revendTimestamp != null) {
                Assertions.assertEquals(revendTimestamp.getTime(), revEnd.getTimestamp());
            }

        }

    }

}
