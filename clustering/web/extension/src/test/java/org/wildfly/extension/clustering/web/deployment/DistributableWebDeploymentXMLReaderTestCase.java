/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.property.PropertyReplacers;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.web.infinispan.routing.RankedRoutingConfiguration;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionManagementConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;
import org.wildfly.extension.clustering.web.routing.NullRouteLocatorServiceConfiguratorFactory;
import org.wildfly.extension.clustering.web.routing.infinispan.RankedRouteLocatorServiceConfiguratorFactory;
import org.wildfly.extension.clustering.web.session.hotrod.HotRodSessionManagementConfiguration;
import org.wildfly.extension.clustering.web.session.hotrod.HotRodSessionManagementProvider;
import org.wildfly.extension.clustering.web.session.infinispan.InfinispanSessionManagementProvider;

/**
 * @author Paul Ferraro
 */
@RunWith(value = Parameterized.class)
public class DistributableWebDeploymentXMLReaderTestCase {
    @Parameters
    public static Iterable<DistributableWebDeploymentSchema> parameters() {
        return EnumSet.allOf(DistributableWebDeploymentSchema.class);
    }

    private final DistributableWebDeploymentSchema schema;

    public DistributableWebDeploymentXMLReaderTestCase(DistributableWebDeploymentSchema schema) {
        this.schema = schema;
    }

    @Test
    public void test() throws IOException, XMLStreamException {
        URL url = this.getClass().getResource(String.format("distributable-web-%d.%d.xml", this.schema.getVersion().major(), this.schema.getVersion().minor()));
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(this.schema.getQualifiedName(), this.schema);
        try (InputStream input = url.openStream()) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            MutableDistributableWebDeploymentConfiguration config = new MutableDistributableWebDeploymentConfiguration(PropertyReplacers.noop());
            mapper.parseDocument(config, reader);

            Assert.assertNull(config.getSessionManagement());
            Assert.assertEquals("foo", config.getSessionManagementName());

            Assert.assertNotNull(config.getImmutableClasses());
            Assert.assertEquals(Arrays.asList(Locale.class.getName(), UUID.class.getName()), config.getImmutableClasses());
        } finally {
            mapper.unregisterRootAttribute(this.schema.getQualifiedName());
        }
    }

    @Test
    public void testInfinispan() throws IOException, XMLStreamException {
        URL url = this.getClass().getResource(String.format("distributable-web-infinispan-%d.%d.xml", this.schema.getVersion().major(), this.schema.getVersion().minor()));
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(this.schema.getQualifiedName(), this.schema);
        try (InputStream input = url.openStream()) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            MutableDistributableWebDeploymentConfiguration config = new MutableDistributableWebDeploymentConfiguration(PropertyReplacers.noop());
            mapper.parseDocument(config, reader);

            Assert.assertNull(config.getSessionManagementName());
            DistributableSessionManagementProvider<? extends DistributableSessionManagementConfiguration<DeploymentUnit>> result = config.getSessionManagement();
            Assert.assertNotNull(result);
            Assert.assertTrue(result instanceof InfinispanSessionManagementProvider);
            InfinispanSessionManagementProvider provider = (InfinispanSessionManagementProvider) result;

            InfinispanSessionManagementConfiguration<DeploymentUnit> configuration = provider.getSessionManagementConfiguration();
            Assert.assertEquals("foo", configuration.getContainerName());
            Assert.assertEquals("bar", configuration.getCacheName());
            Assert.assertSame(SessionAttributePersistenceStrategy.FINE, configuration.getAttributePersistenceStrategy());

            if (this.schema.since(DistributableWebDeploymentSchema.VERSION_2_0)) {
                Assert.assertTrue(provider.getRouteLocatorServiceConfiguratorFactory() instanceof RankedRouteLocatorServiceConfiguratorFactory);
                RankedRoutingConfiguration routing = ((RankedRouteLocatorServiceConfiguratorFactory<InfinispanSessionManagementConfiguration<DeploymentUnit>>) provider.getRouteLocatorServiceConfiguratorFactory()).getConfiguration();
                Assert.assertEquals(":", routing.getDelimiter());
                Assert.assertEquals(4, routing.getMaxRoutes());
            } else {
                Assert.assertTrue(provider.getRouteLocatorServiceConfiguratorFactory() instanceof NullRouteLocatorServiceConfiguratorFactory);
            }

            Assert.assertNotNull(config.getImmutableClasses());
            Assert.assertEquals(Arrays.asList(Locale.class.getName(), UUID.class.getName()), config.getImmutableClasses());
        } finally {
            mapper.unregisterRootAttribute(this.schema.getQualifiedName());
        }
    }

    @Test
    public void testHotRod() throws IOException, XMLStreamException {
        URL url = this.getClass().getResource(String.format("distributable-web-hotrod-%d.%d.xml", this.schema.getVersion().major(), this.schema.getVersion().minor()));
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(this.schema.getQualifiedName(), new DistributableWebDeploymentXMLReader(this.schema));
        try (InputStream input = url.openStream()) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            MutableDistributableWebDeploymentConfiguration config = new MutableDistributableWebDeploymentConfiguration(PropertyReplacers.noop());
            mapper.parseDocument(config, reader);

            Assert.assertNull(config.getSessionManagementName());
            DistributableSessionManagementProvider<? extends DistributableSessionManagementConfiguration<DeploymentUnit>> result = config.getSessionManagement();
            Assert.assertNotNull(result);
            Assert.assertTrue(result instanceof HotRodSessionManagementProvider);
            HotRodSessionManagementConfiguration<DeploymentUnit> configuration = ((HotRodSessionManagementProvider) result).getSessionManagementConfiguration();
            Assert.assertEquals("foo", configuration.getContainerName());
            Assert.assertSame(SessionAttributePersistenceStrategy.FINE, configuration.getAttributePersistenceStrategy());
        } finally {
            mapper.unregisterRootAttribute(this.schema.getQualifiedName());
        }
    }
}
