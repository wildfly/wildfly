/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.delivery;

import org.jboss.as.ejb3.delivery.parser.EJBBoundMdbDeliveryMetaDataParser;
import org.jboss.as.ejb3.delivery.parser.EjbBoundMdbDeliveryMetaDataSchema;
import org.jboss.metadata.ejb.parser.jboss.ejb3.JBossEjb3MetaDataParser;
import org.jboss.metadata.ejb.parser.spec.AbstractMetaDataParser;
import org.jboss.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.property.PropertyReplacers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@RunWith(Parameterized.class)
public class EJBBoundMdbDeliveryMetaDataParserTestCase {

    private final EjbBoundMdbDeliveryMetaDataSchema schema;
    private final JBossEjb3MetaDataParser parser;

    @Parameterized.Parameters
    public static Iterable<EjbBoundMdbDeliveryMetaDataSchema> parameters() {
        return EnumSet.allOf(EjbBoundMdbDeliveryMetaDataSchema.class);
    }

    public EJBBoundMdbDeliveryMetaDataParserTestCase(EjbBoundMdbDeliveryMetaDataSchema schema) {
        this.schema = schema;
        Map<String, AbstractMetaDataParser<?>> parsers = new HashMap<>();
        parsers.put(schema.getNamespace().getUri(), new EJBBoundMdbDeliveryMetaDataParser(schema));
        parser = new JBossEjb3MetaDataParser(parsers);
    }

    @Test
    public void test() throws Exception {
        String filename = "jboss-ejb3-delivery-" +
                schema.getNamespace().getVersion().major() + "." + schema.getNamespace().getVersion().minor() + ".xml";
        try (InputStream is = this.getClass().getResourceAsStream(filename)) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
            parser.parse(reader, new MetaDataElementParser.DTDInfo(), PropertyReplacers.noop());
        }
    }
}
