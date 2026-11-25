/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.EnumSet;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.metadata.property.PropertyReplacers;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Unit test to validate parsing of singleton deployment descriptor namespace.
 * @author Paul Ferraro
 */
@RunWith(value = Parameterized.class)
public class SingletonDeploymentSchemaTestCase {
    @Parameters
    public static Iterable<SingletonDeploymentSchema> parameters() {
        return EnumSet.allOf(SingletonDeploymentSchema.class);
    }

    private final SingletonDeploymentSchema schema;

    public SingletonDeploymentSchemaTestCase(SingletonDeploymentSchema schema) {
        this.schema = schema;
    }

    @Test
    public void test() throws IOException, XMLStreamException {
        URL url = this.getClass().getResource(String.format("singleton-deployment-%d.%d.xml", this.schema.getVersion().major(), this.schema.getVersion().minor()));
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(this.schema.getQualifiedName(), this.schema);
        try (InputStream input = url.openStream()) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            MutableSingletonDeploymentConfiguration config = new MutableSingletonDeploymentConfiguration(PropertyReplacers.noop());
            mapper.parseDocument(config, reader);

            Assert.assertEquals("foo", config.getPolicy());
        } finally {
            mapper.unregisterRootAttribute(this.schema.getQualifiedName());
        }
    }
}
