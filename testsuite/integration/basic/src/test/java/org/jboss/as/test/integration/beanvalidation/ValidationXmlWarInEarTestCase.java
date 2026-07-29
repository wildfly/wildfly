/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidatorFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that {@code validation.xml} inside a WAR sub-deployment within an EAR is properly
 * located and processed by the Bean Validation provider.
 * <p>
 * The bean {@link ValidationXmlEarBean} has no annotation-based constraints. A constraint
 * mapping declared in {@code validation.xml} adds a {@code @NotNull} constraint to its
 * {@code name} field. If {@code validation.xml} is found, validating a bean with a null
 * name produces a violation. If not found (the pre-fix behavior), no violation occurs.
 *
 * @see <a href="https://issues.redhat.com/browse/WFLY-21093">WFLY-21093</a>
 */
@RunWith(Arquillian.class)
public class ValidationXmlWarInEarTestCase {

    private static final String VALIDATION_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<validation-config xmlns=\"https://jakarta.ee/xml/ns/validation/configuration\"\n" +
            "                   version=\"3.0\">\n" +
            "    <constraint-mapping>META-INF/constraint-mapping.xml</constraint-mapping>\n" +
            "</validation-config>";

    private static final String CONSTRAINT_MAPPING_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<constraint-mappings xmlns=\"https://jakarta.ee/xml/ns/validation/mapping\"\n" +
            "                     version=\"3.0\">\n" +
            "    <bean class=\"org.jboss.as.test.integration.beanvalidation.ValidationXmlEarBean\">\n" +
            "        <field name=\"name\">\n" +
            "            <constraint annotation=\"jakarta.validation.constraints.NotNull\">\n" +
            "                <message>Name must not be null (configured via validation.xml)</message>\n" +
            "            </constraint>\n" +
            "        </field>\n" +
            "    </bean>\n" +
            "</constraint-mappings>";

    @Inject
    private ValidatorFactory validatorFactory;

    @Deployment
    public static EnterpriseArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "validation-xml-war-in-ear.war")
                .addClasses(ValidationXmlWarInEarTestCase.class, ValidationXmlEarBean.class)
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml")
                .addAsResource(new StringAsset(VALIDATION_XML), "META-INF/validation.xml")
                .addAsResource(new StringAsset(CONSTRAINT_MAPPING_XML), "META-INF/constraint-mapping.xml");

        return ShrinkWrap.create(EnterpriseArchive.class, "validation-xml-war-in-ear.ear")
                .addAsModule(war);
    }

    /**
     * Validates that the {@code @NotNull} constraint declared in the XML constraint mapping
     * (referenced by {@code validation.xml}) is applied when the {@code name} field is null.
     */
    @Test
    public void testValidationXmlConstraintsApplied() {
        ValidationXmlEarBean bean = new ValidationXmlEarBean();
        Set<ConstraintViolation<ValidationXmlEarBean>> violations = validatorFactory.getValidator().validate(bean);
        assertEquals("Expected one constraint violation from validation.xml mapping", 1, violations.size());
        assertEquals("Name must not be null (configured via validation.xml)",
                violations.iterator().next().getMessage());
    }

    /**
     * Validates that a bean with a non-null name passes validation (no violations).
     */
    @Test
    public void testValidBeanPassesValidation() {
        ValidationXmlEarBean bean = new ValidationXmlEarBean();
        bean.setName("test");
        Set<ConstraintViolation<ValidationXmlEarBean>> violations = validatorFactory.getValidator().validate(bean);
        assertTrue("Expected no violations for valid bean", violations.isEmpty());
    }
}
