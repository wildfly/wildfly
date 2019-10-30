/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.dist.subsystem.xml;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jboss.metadata.parser.util.XMLResourceResolver;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * A XSDValidationUnitTestCase.
 *
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @version $Revision: 1.1 $
 */
public class XSDValidationUnitTestCase extends AbstractValidationUnitTest {
    @Test
    public void testJBossXsds() throws Exception {
        for (File xsdFile : jbossSchemaFiles(false))
            validateXsd(xsdFile);
    }

    private void validateXsd(final File xsdFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document document = parser.parse(xsdFile);

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setErrorHandler(new ErrorHandlerImpl());
        schemaFactory.setResourceResolver(new XMLResourceResolver());

        Schema schema = schemaFactory.newSchema(resource("schema/XMLSchema.xsd"));
        Validator validator = schema.newValidator();
        validator.validate(new DOMSource(document));
    }

    private URL resource(final String name) {
        final ClassLoader classLoader = getClass().getClassLoader();
        final URL resource = classLoader.getResource(name);
        assertNotNull("Can't locate resource " + name + " on " + classLoader, resource);
        return resource;
    }
}
