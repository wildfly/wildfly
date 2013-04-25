/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.jpa.puparser;

import java.io.StringReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.metadata.property.PropertyReplacers;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class PersistenceUnitXmlParserTestCase {
    /**
     * See http://issues.jboss.org/browse/STXM-8
     */
    @Test
    public void testVersion() throws Exception {
        final String persistence_xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
                "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
                "  <persistence-unit name=\"mypc\">" +
                "    <description>Persistence Unit." +
                "    </description>" +
                "    <jta-data-source>java:/H2DS</jta-data-source>" +
                "    <class>org.jboss.as.test.integration.jpa.epcpropagation.MyEntity</class>" +
                "    <properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/></properties>" +
                "  </persistence-unit>" +
                "</persistence>";
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(persistence_xml));
        PersistenceUnitMetadataHolder metadataHolder = PersistenceUnitXmlParser.parse(reader, PropertyReplacers.noop());
        PersistenceUnitMetadata metadata = metadataHolder.getPersistenceUnits().get(0);
        String version = metadata.getPersistenceXMLSchemaVersion();
        assertEquals("1.0", version);
    }
}
