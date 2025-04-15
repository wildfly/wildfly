/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.EnumSet;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.web.session.SharedSessionManagerConfig;
import org.jboss.metadata.property.PropertyReplacers;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Paul Ferraro
 */
@RunWith(value = Parameterized.class)
public class SharedSessionConfigXMLReaderTestCase {

    @Parameters
    public static Iterable<SharedSessionConfigSchema> parameters() {
        return EnumSet.allOf(SharedSessionConfigSchema.class);
    }

    private final SharedSessionConfigSchema schema;

    public SharedSessionConfigXMLReaderTestCase(SharedSessionConfigSchema schema) {
        this.schema = schema;
    }

    @Test
    public void test() throws IOException, XMLStreamException {
        URL url = this.getClass().getResource(String.format("shared-session-config-%d.%d.xml", this.schema.getVersion().major(), this.schema.getVersion().minor()));
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(this.schema.getQualifiedName(), new SharedSessionConfigXMLReader(this.schema, PropertyReplacers.noop()));
        try (InputStream input = url.openStream()) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            SharedSessionManagerConfig config = new SharedSessionManagerConfig();
            mapper.parseDocument(config, reader);

            Assert.assertTrue(config.isDistributable());
            Assert.assertEquals(10, config.getMaxActiveSessions().intValue());
            Assert.assertEquals("/", config.getSessionConfig().getCookieConfig().getPath());
        } finally {
            mapper.unregisterRootAttribute(this.schema.getQualifiedName());
        }
    }
}
