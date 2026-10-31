/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
 * Regression test for <a href="https://issues.redhat.com/browse/WFLY-21093">WFLY-21093</a>: a WAR
 * packaged inside an EAR declares its Bean Validation configuration in
 * {@code WEB-INF/classes/META-INF/validation.xml}, and the <em>CDI-managed</em> ({@code @Inject}ed)
 * {@link ValidatorFactory}/{@link Validator} must honour it rather than silently falling back to
 * annotation-only configuration.
 * <p>
 * The CDI {@code ValidatorFactory} is a single {@code @ApplicationScoped} bean shared across the whole
 * EAR; it is created once and reads {@code validation.xml} through the thread context class loader in
 * effect at creation time. WildFly must create it under a sub-deployment's class loader (so the WAR's
 * {@code WEB-INF/classes/META-INF/validation.xml} is visible), not under the top-level EAR class
 * loader (which cannot see {@code WEB-INF/classes}). Before the fix the EAR and the WAR raced to
 * create that shared factory; when the EAR won, {@code validation.xml} was invisible and the injected
 * validator applied no constraints.
 * <p>
 * {@link ValidationXmlEarBean} carries <em>no</em> constraint annotations; its {@code name} field is
 * constrained to {@code @NotNull} solely through {@code validation.xml} &#8594; {@code constraint-mapping.xml}.
 * So a single violation proves {@code validation.xml} was applied, and zero violations reproduces the
 * bug. The {@code validation.xml} additionally declares a WAR-local {@code <value-extractor>}
 * ({@link NameHolderValueExtractor}), mirroring the original reproducer, so the test also covers
 * loading a {@code validation.xml}-referenced class with the WAR class loader.
 */
@RunWith(Arquillian.class)
public class ValidationXmlWarInEarTestCase {

    private static final String WAR_CONTEXT = "validation-xml-war-in-ear";
    private static final String EXPECTED_MESSAGE = "name must not be null (from validation.xml)";

    @Inject
    private Validator validator;

    @Inject
    private ValidatorFactory validatorFactory;

    private static final String VALIDATION_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <validation-config xmlns="https://jakarta.ee/xml/ns/validation/configuration" version="3.1">
                <value-extractor>org.jboss.as.test.integration.beanvalidation.NameHolderValueExtractor</value-extractor>
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
     * Builds an EAR containing a single CDI-enabled WAR whose {@code validation.xml} constrains
     * {@link ValidationXmlEarBean#getName()}.
     *
     * @return the deployment archive
     */
    @Deployment
    public static EnterpriseArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_CONTEXT + ".war")
                .addClasses(ValidationXmlWarInEarTestCase.class, ValidationXmlEarBean.class,
                        ValidationCheckServlet.class, NameHolder.class, NameHolderValueExtractor.class)
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml")
                .addAsResource(new StringAsset(VALIDATION_XML), "META-INF/validation.xml")
                .addAsResource(new StringAsset(CONSTRAINT_MAPPING_XML), "META-INF/constraint-mapping.xml");

        return ShrinkWrap.create(EnterpriseArchive.class, WAR_CONTEXT + ".ear")
                .addAsModule(war);
    }

    /**
     * The core WFLY-21093 assertion: the CDI-injected {@link Validator} must apply the {@code @NotNull}
     * constraint defined in the WAR's {@code validation.xml}. Zero violations means the EAR class loader
     * was used and {@code validation.xml} was not found.
     */
    @Test
    public void testInjectedValidatorUsesWarValidationXml() {
        assertNotNull("Validator should be injectable via CDI", validator);
        Set<ConstraintViolation<ValidationXmlEarBean>> violations = validator.validate(new ValidationXmlEarBean());
        assertEquals("Injected Validator must apply the @NotNull constraint from the WAR's validation.xml; "
                + "0 means validation.xml was not found (WFLY-21093)", 1, violations.size());
        assertEquals(EXPECTED_MESSAGE, violations.iterator().next().getMessage());
    }

    /**
     * The CDI-injected {@link ValidatorFactory} must likewise honour the WAR's {@code validation.xml}.
     */
    @Test
    public void testInjectedValidatorFactoryUsesWarValidationXml() {
        assertNotNull("ValidatorFactory should be injectable via CDI", validatorFactory);
        Set<ConstraintViolation<ValidationXmlEarBean>> violations =
                validatorFactory.getValidator().validate(new ValidationXmlEarBean());
        assertEquals("Injected ValidatorFactory must apply the WAR's validation.xml (WFLY-21093)",
                1, violations.size());
    }

    /**
     * Baseline: the default (TCCL-based) {@link ValidatorFactory} obtained from
     * {@link Validation#buildDefaultValidatorFactory()} always sees the WAR's {@code validation.xml},
     * because in-container test methods run with the WAR class loader as TCCL. This path worked before
     * the fix too, so it isolates the CDI-specific regression from a general discovery failure.
     */
    @Test
    public void testBuildDefaultValidatorFactoryUsesWarValidationXml() {
        try (ValidatorFactory vf = Validation.buildDefaultValidatorFactory()) {
            Set<ConstraintViolation<ValidationXmlEarBean>> violations = vf.getValidator().validate(new ValidationXmlEarBean());
            assertEquals(1, violations.size());
        }
    }

    /**
     * The injected {@link Validator} must also honour {@code validation.xml} when used from the web
     * tier. {@link ValidationCheckServlet} validates the bean with the injected validator and returns
     * the violation count on its first line.
     *
     * @param url the deployment base URL
     * @throws Exception if the HTTP request fails
     */
    @Test
    @RunAsClient
    public void testInjectedValidatorFromServlet(@ArquillianResource URL url) throws Exception {
        URL target = new URL(url.getProtocol(), url.getHost(), url.getPort(), "/" + WAR_CONTEXT + "/validate");
        String response = HttpRequest.get(target.toString(), 10, TimeUnit.SECONDS);
        assertEquals("Servlet-injected Validator must apply the WAR's validation.xml (WFLY-21093)",
                "1", response.trim().split("\\R")[0].trim());
    }
}
