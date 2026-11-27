/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Companion/control for {@link ValidationXmlWarInEarTestCase} and a scope guard for the WFLY-21093
 * fix. The fix skips the top-level EAR in the Bean Validation deployment processors, so it must not
 * change behaviour for a <em>standalone</em> WAR (which has no sub-deployments): the CDI-injected
 * {@link Validator} of a plain WAR must still honour the WAR's {@code validation.xml}.
 * <p>
 * {@link ValidationXmlEarBean#getName()} is constrained to {@code @NotNull} solely through
 * {@code validation.xml} &#8594; {@code constraint-mapping.xml}, so a single violation proves
 * {@code validation.xml} was applied by the injected validator.
 *
 * @see <a href="https://issues.redhat.com/browse/WFLY-21093">WFLY-21093</a>
 */
@RunWith(Arquillian.class)
public class ValidationXmlStandaloneWarTestCase {

    private static final String EXPECTED_MESSAGE = "name must not be null (standalone war validation.xml)";

    @Inject
    private Validator validator;

    private static final String VALIDATION_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <validation-config xmlns="https://jakarta.ee/xml/ns/validation/configuration" version="3.1">
                <constraint-mapping>META-INF/constraint-mapping.xml</constraint-mapping>
            </validation-config>
            """;

    private static final String CONSTRAINT_MAPPING_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <constraint-mappings xmlns="https://jakarta.ee/xml/ns/validation/mapping" version="3.1">
                <bean class="org.jboss.as.test.integration.beanvalidation.ValidationXmlEarBean" ignore-annotations="true">
                    <field name="name">
                        <constraint annotation="jakarta.validation.constraints.NotNull">
                            <message>%s</message>
                        </constraint>
                    </field>
                </bean>
            </constraint-mappings>
            """.formatted(EXPECTED_MESSAGE);

    /**
     * Builds a single CDI-enabled WAR whose {@code validation.xml} constrains
     * {@link ValidationXmlEarBean#getName()}.
     *
     * @return the deployment archive
     */
    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "validation-xml-standalone-war.war")
                .addClasses(ValidationXmlStandaloneWarTestCase.class, ValidationXmlEarBean.class)
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml")
                .addAsResource(new StringAsset(VALIDATION_XML), "META-INF/validation.xml")
                .addAsResource(new StringAsset(CONSTRAINT_MAPPING_XML), "META-INF/constraint-mapping.xml");
    }

    /**
     * The CDI-injected {@link Validator} of a standalone WAR must apply the {@code @NotNull} constraint
     * from the WAR's {@code validation.xml}, confirming the WFLY-21093 fix does not regress plain WARs.
     */
    @Test
    public void testInjectedValidatorUsesWarValidationXml() {
        assertNotNull("Validator should be injectable via CDI", validator);
        Set<ConstraintViolation<ValidationXmlEarBean>> violations = validator.validate(new ValidationXmlEarBean());
        assertEquals("Injected Validator must apply the standalone WAR's validation.xml", 1, violations.size());
        assertEquals(EXPECTED_MESSAGE, violations.iterator().next().getMessage());
    }
}
