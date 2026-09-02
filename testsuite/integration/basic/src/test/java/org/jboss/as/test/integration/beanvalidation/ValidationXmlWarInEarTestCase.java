/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that {@code validation.xml} inside WAR sub-deployments within an EAR is properly
 * located and processed by the Bean Validation provider, and that each WAR's constraints
 * are isolated from other WARs in the same EAR.
 * <p>
 * Deploys an EAR containing three WARs, each constraining a <em>different</em> field:
 * <ul>
 *   <li><b>war1</b> — constrains {@code name} via {@code @NotNull}. Tested in-container.</li>
 *   <li><b>war2</b> — constrains {@code description} via {@code @NotNull}. Tested via HTTP.</li>
 *   <li><b>war3</b> — has <em>no</em> {@code validation.xml}. Tested via HTTP.</li>
 * </ul>
 * Each test verifies that only the constraints defined in <em>that</em> WAR's
 * {@code validation.xml} are applied, and that constraints from other WARs do not leak
 * across classloader boundaries.
 *
 * @see <a href="https://issues.redhat.com/browse/WFLY-21093">WFLY-21093</a>
 */
@RunWith(Arquillian.class)
public class ValidationXmlWarInEarTestCase {

    @Inject
    private ValidatorFactory validatorFactory;

    @Inject
    private Validator validator;

    private static final String WAR1_CONTEXT = "validation-war1";
    private static final String WAR2_CONTEXT = "validation-war2";
    private static final String WAR3_CONTEXT = "no-validation-war3";

    private static final String WAR1_MESSAGE = "Name must not be null (war1)";
    private static final String WAR2_MESSAGE = "Description is required (war2)";

    private static final String VALIDATION_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <validation-config xmlns="https://jakarta.ee/xml/ns/validation/configuration"
                               version="3.0">
                <constraint-mapping>META-INF/constraint-mapping.xml</constraint-mapping>
            </validation-config>
            """;

    private static final String CONSTRAINT_MAPPING_XML_WAR1 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <constraint-mappings xmlns="https://jakarta.ee/xml/ns/validation/mapping"
                                 version="3.0">
                <bean class="org.jboss.as.test.integration.beanvalidation.ValidationXmlEarBean">
                    <field name="name">
                        <constraint annotation="jakarta.validation.constraints.NotNull">
                            <message>%s</message>
                        </constraint>
                    </field>
                </bean>
            </constraint-mappings>
            """.formatted(WAR1_MESSAGE);

    private static final String CONSTRAINT_MAPPING_XML_WAR2 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <constraint-mappings xmlns="https://jakarta.ee/xml/ns/validation/mapping"
                                 version="3.0">
                <bean class="org.jboss.as.test.integration.beanvalidation.ValidationXmlEarBean">
                    <field name="description">
                        <constraint annotation="jakarta.validation.constraints.NotNull">
                            <message>%s</message>
                        </constraint>
                    </field>
                </bean>
            </constraint-mappings>
            """.formatted(WAR2_MESSAGE);

    @Deployment
    public static EnterpriseArchive deployment() {
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, WAR1_CONTEXT + ".war")
                .addClasses(ValidationXmlWarInEarTestCase.class, ValidationXmlEarBean.class)
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml")
                .addAsResource(new StringAsset(VALIDATION_XML), "META-INF/validation.xml")
                .addAsResource(new StringAsset(CONSTRAINT_MAPPING_XML_WAR1), "META-INF/constraint-mapping.xml");

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, WAR2_CONTEXT + ".war")
                .addClasses(ValidationCheckServlet.class, ValidationXmlEarBean.class)
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml")
                .addAsResource(new StringAsset(VALIDATION_XML), "META-INF/validation.xml")
                .addAsResource(new StringAsset(CONSTRAINT_MAPPING_XML_WAR2), "META-INF/constraint-mapping.xml");

        WebArchive war3 = ShrinkWrap.create(WebArchive.class, WAR3_CONTEXT + ".war")
                .addClasses(ValidationCheckServlet.class, ValidationXmlEarBean.class)
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");

        return ShrinkWrap.create(EnterpriseArchive.class, "validation-xml-multi-war.ear")
                .addAsModule(war1)
                .addAsModule(war2)
                .addAsModule(war3);
    }

    @Test
    public void testCdiInjection() {
        assertNotNull("ValidatorFactory should be injectable via CDI", validatorFactory);
        assertNotNull("Validator should be injectable via CDI", validator);
        assertNotNull("CDI ValidatorFactory should produce a Validator", validatorFactory.getValidator());
    }

    /**
     * WAR1 constrains {@code name} — a bean with null name should produce exactly one violation.
     */
    @Test
    public void testWar1NameConstraintApplied() {
        ValidationXmlEarBean bean = new ValidationXmlEarBean();
        Set<ConstraintViolation<ValidationXmlEarBean>> violations = validateBean(bean);
        assertEquals("Expected one constraint violation from war1 validation.xml", 1, violations.size());

        ConstraintViolation<ValidationXmlEarBean> violation = violations.iterator().next();
        assertEquals(WAR1_MESSAGE, violation.getMessage());
        assertEquals("name", violation.getPropertyPath().toString());
    }

    /**
     * WAR1 does NOT constrain {@code description} — that is WAR2's rule.
     * A bean with null description but non-null name should have zero violations in WAR1.
     */
    @Test
    public void testWar1DescriptionConstraintNotLeaked() {
        ValidationXmlEarBean bean = new ValidationXmlEarBean();
        bean.setName("present");

        Set<ConstraintViolation<ValidationXmlEarBean>> violations = validateBean(bean);
        assertTrue("WAR1 should not enforce WAR2's description constraint", violations.isEmpty());
    }

    /**
     * A fully valid bean (both fields set) should pass WAR1 validation.
     */
    @Test
    public void testWar1ValidBeanPassesValidation() {
        ValidationXmlEarBean bean = new ValidationXmlEarBean();
        bean.setName("test");
        bean.setDescription("test");
        Set<ConstraintViolation<ValidationXmlEarBean>> violations = validateBean(bean);
        assertTrue("Expected no violations for valid bean", violations.isEmpty());
    }

    private static Set<ConstraintViolation<ValidationXmlEarBean>> validateBean(ValidationXmlEarBean bean) {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        try {
            return validatorFactory.getValidator().validate(bean);
        } finally {
            validatorFactory.close();
        }
    }

    /**
     * WAR2 constrains {@code description} — should produce exactly one violation on that field,
     * and must NOT contain WAR1's {@code name} constraint.
     */
    @Test
    @RunAsClient
    public void testWar2DescriptionConstraintApplied(@ArquillianResource URL url) throws Exception {
        String response = fetchValidation(url, WAR2_CONTEXT);
        String[] lines = response.trim().split("\\R");

        assertEquals("WAR2 should report exactly one violation", "1", lines[0].trim());
        assertTrue("WAR2 violation should be on 'description' field", response.contains("description="));
        assertTrue("WAR2 should use its own constraint message", response.contains(WAR2_MESSAGE));
        assertFalse("WAR2 must not contain WAR1's name constraint", response.contains("name="));
        assertFalse("WAR2 must not contain WAR1's constraint message", response.contains(WAR1_MESSAGE));
    }

    /**
     * WAR3 has no {@code validation.xml} — should report zero violations for any field.
     */
    @Test
    @RunAsClient
    public void testWar3WithoutValidationXmlHasNoConstraints(@ArquillianResource URL url) throws Exception {
        String response = fetchValidation(url, WAR3_CONTEXT);
        assertEquals("WAR3 without validation.xml should have no violations", "0",
                response.trim().split("\\R")[0].trim());
    }

    private static String fetchValidation(URL url, String context) throws Exception {
        URL targetUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(),
                "/" + context + "/validate");
        return HttpRequest.get(targetUrl.toString(), 10, TimeUnit.SECONDS);
    }
}
