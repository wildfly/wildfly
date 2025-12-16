/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.version.Stability;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.metadata.property.PropertyReplacers;
import org.jboss.metadata.property.SystemPropertyResolver;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.session.cache.affinity.NarySessionAffinityConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.extension.clustering.web.routing.NullRouteLocatorProvider;
import org.wildfly.extension.clustering.web.routing.infinispan.RankedRouteLocatorProvider;
import org.wildfly.extension.clustering.web.session.hotrod.HotRodSessionManagementProvider;
import org.wildfly.extension.clustering.web.session.infinispan.InfinispanSessionManagementProvider;
import org.xml.sax.SAXException;

/**
 * Parameterized test validating distributable-web deployment descriptor XSD schemas and parsing logic across all schema versions.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
@RunWith(value = Parameterized.class)
public class DistributableWebDeploymentSchemaTestCase {
    @Parameters
    public static Iterable<DistributableWebDeploymentSchema> parameters() {
        return EnumSet.allOf(DistributableWebDeploymentSchema.class);
    }

    private static final PropertyReplacer PROPERTY_REPLACER = PropertyReplacers.resolvingExpressionReplacer(SystemPropertyResolver.INSTANCE);

    private final DistributableWebDeploymentSchema schema;

    public DistributableWebDeploymentSchemaTestCase(DistributableWebDeploymentSchema schema) {
        this.schema = schema;
    }

    protected URL getDeploymentXmlURL(String additionalQualifier) {
        String format = String.format("distributable-web-%s%s%d.%d.xml",
                additionalQualifier == null ? "" : additionalQualifier + "-",
                this.schema.getStability() == Stability.DEFAULT ? "" : this.schema.getStability() + "-",
                this.schema.getVersion().major(),
                this.schema.getVersion().minor()
        );
        return this.getClass().getResource(format);
    }

    protected URL getDeploymentSchemaURL() {
        String schemaFileName = String.format("distributable-web%s_%d_%d.xsd",
                this.schema.getStability() == Stability.DEFAULT ? "" : "_" + this.schema.getStability(),
                this.schema.getVersion().major(),
                this.schema.getVersion().minor()
        );
        return this.getClass().getResource("/schema/" + schemaFileName);
    }

    @Test
    public void testSessionManagerSchema() throws Exception {
        URL url = getDeploymentXmlURL(null);
        this.validateAgainstSchema(url);
    }

    @Test
    public void testSessionManager() throws IOException, XMLStreamException {
        URL url = getDeploymentXmlURL(null);
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(this.schema.getQualifiedName(), this.schema);
        try (InputStream input = url.openStream()) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            MutableDistributableWebDeploymentConfiguration config = new MutableDistributableWebDeploymentConfiguration(PROPERTY_REPLACER);
            mapper.parseDocument(config, reader);

            Assert.assertNull(config.getSessionManagementProvider());
            Assert.assertEquals("foo", config.getSessionManagementName());

            Assert.assertNotNull(config.getImmutableClasses());
            Assert.assertEquals(Arrays.asList(Locale.class.getName(), UUID.class.getName()), config.getImmutableClasses());
        } finally {
            mapper.unregisterRootAttribute(this.schema.getQualifiedName());
        }
    }

    @Test
    public void testInfinispanSessionManagerSchema() throws Exception {
        URL url = getDeploymentXmlURL("infinispan");
        this.validateAgainstSchema(url);
    }

    @Test
    public void testInfinispanSessionManager() throws IOException, XMLStreamException {
        URL url = getDeploymentXmlURL("infinispan");
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(this.schema.getQualifiedName(), this.schema);
        try (InputStream input = url.openStream()) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            MutableDistributableWebDeploymentConfiguration config = new MutableDistributableWebDeploymentConfiguration(PROPERTY_REPLACER);
            mapper.parseDocument(config, reader);

            Assert.assertNull(config.getSessionManagementName());
            DistributableSessionManagementProvider result = config.getSessionManagementProvider();
            Assert.assertNotNull(result);
            Assert.assertTrue(result instanceof InfinispanSessionManagementProvider);
            InfinispanSessionManagementProvider provider = (InfinispanSessionManagementProvider) result;

            DistributableSessionManagementConfiguration<DeploymentUnit> configuration = provider.getSessionManagementConfiguration();
            BinaryServiceConfiguration cacheConfiguration = provider.getCacheConfiguration();
            Assert.assertEquals("foo", cacheConfiguration.getParentName());
            Assert.assertEquals("bar", cacheConfiguration.getChildName());
            Assert.assertSame(SessionAttributePersistenceStrategy.FINE, configuration.getAttributePersistenceStrategy());

            if (this.schema.since(DistributableWebDeploymentSchema.VERSION_2_0)) {
                Assert.assertTrue(provider.getRouteLocatorProvider() instanceof RankedRouteLocatorProvider);
                NarySessionAffinityConfiguration routing = ((RankedRouteLocatorProvider) provider.getRouteLocatorProvider()).getNarySessionAffinityConfiguration();
                Assert.assertEquals(":", routing.getDelimiter());
                Assert.assertEquals(4, routing.getMaxMembers());
            } else {
                Assert.assertTrue(provider.getRouteLocatorProvider() instanceof NullRouteLocatorProvider);
            }

            if (this.schema.since(DistributableWebDeploymentSchema.VERSION_5_0_COMMUNITY)) {
                Assert.assertTrue(configuration.getIdleThreshold().isPresent());
                Assert.assertEquals(Duration.ofMinutes(10), configuration.getIdleThreshold().get());
            }

            Assert.assertNotNull(config.getImmutableClasses());
            Assert.assertEquals(Arrays.asList(Locale.class.getName(), UUID.class.getName()), config.getImmutableClasses());
        } finally {
            mapper.unregisterRootAttribute(this.schema.getQualifiedName());
        }
    }

    @Test
    public void testHotRodSessionManagerSchema() throws Exception {
        URL url = getDeploymentXmlURL("hotrod");
        this.validateAgainstSchema(url);
    }

    @Test
    public void testHotRodSessionManager() throws IOException, XMLStreamException {
        URL url = getDeploymentXmlURL("hotrod");
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(this.schema.getQualifiedName(), this.schema);
        try (InputStream input = url.openStream()) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            MutableDistributableWebDeploymentConfiguration config = new MutableDistributableWebDeploymentConfiguration(PROPERTY_REPLACER);
            mapper.parseDocument(config, reader);

            Assert.assertNull(config.getSessionManagementName());
            DistributableSessionManagementProvider result = config.getSessionManagementProvider();
            Assert.assertNotNull(result);
            Assert.assertTrue(result instanceof HotRodSessionManagementProvider);
            HotRodSessionManagementProvider provider = (HotRodSessionManagementProvider) result;
            DistributableSessionManagementConfiguration<DeploymentUnit> configuration = provider.getSessionManagementConfiguration();
            Assert.assertEquals("foo", provider.getCacheConfiguration().getParentName());
            Assert.assertSame(SessionAttributePersistenceStrategy.FINE, configuration.getAttributePersistenceStrategy());
        } finally {
            mapper.unregisterRootAttribute(this.schema.getQualifiedName());
        }
    }

    protected void validateAgainstSchema(URL xmlURL) throws IOException, SAXException {
        URL schemaURL = getDeploymentSchemaURL();

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaURL);
        Validator validator = schema.newValidator();

        // Read the XML and "resolve" property expressions
        try (InputStream input = xmlURL.openStream()) {
            String xmlContent = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            // Replace property expressions with their default values for validation
            // otherwise all properties would fail to resolve
            // n.b. the parsing tests are using proper PropertyReplacer
            xmlContent = xmlContent.replaceAll("\\$\\{[^:}]*:([^}]+)\\}", "$1");
            xmlContent = xmlContent.replaceAll("\\$\\{([^}]+)\\}", "$1");
            validator.validate(new StreamSource(new StringReader(xmlContent)));
        }
    }
}
