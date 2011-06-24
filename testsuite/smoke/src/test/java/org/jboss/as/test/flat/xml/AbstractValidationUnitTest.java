/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.flat.xml;

import org.apache.xerces.dom.DOMInputImpl;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.EntityResolver2;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

/**
 * Date: 23.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class AbstractValidationUnitTest {
    private static final Map<String, File> JBOSS_SCHEMAS = new HashMap<String, File>();
    private static Map<String, String> NAMESPACE_MAP = new HashMap<String, String>();
    static final File MOD_DIR = new File(System.getProperty("user.dir"));
    static final File TARGET_DIR = new File(MOD_DIR, "../../build/target/");
    private static File BASE_DIR = null;

    static {
        NAMESPACE_MAP.put("http://java.sun.com/xml/ns/javaee/javaee_6.xsd", "schema/javaee_6.xsd");
        NAMESPACE_MAP.put("http://www.w3.org/2001/xml.xsd", "schema/xml.xsd");
        final File[] children = TARGET_DIR.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.getName().startsWith("jboss-")) {
                    BASE_DIR = child;
                    break;
                }
            }
            if (BASE_DIR != null) {
                final File schemaDir = new File(BASE_DIR, "docs/schema");
                final File[] xsds = schemaDir.listFiles(SchemaFilter.FILTER);
                for (File xsd : xsds) {
                    JBOSS_SCHEMAS.put(xsd.getName(), xsd);
                }
            }
        }
    }

    static final EntityResolver DEFAULT_ENTITY_RESOLVER = new EntityResolver() {
        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            if (systemId == null)
                fail("Failed to resolve schema: systemId is null");
            int lastSlash = systemId.lastIndexOf('/');
            if (lastSlash > 0)
                systemId = systemId.substring(lastSlash + 1);
            URL xsdUrl = discoverXsd(systemId);
            return new InputSource(xsdUrl.openStream());
        }
    };

    static final LSResourceResolver DEFAULT_RESOURCE_RESOLVER = new LSResourceResolver() {
        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
            LSInput input = new DOMInputImpl();

            final URL url;
            if (NAMESPACE_MAP.containsKey(systemId)) {
                url = discoverXsd(NAMESPACE_MAP.get(systemId));
            } else {
                url = discoverXsd(systemId);
            }
            try {
                input.setByteStream(url.openStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return input;
        }
    };

    /**
     * The base directory, e.g. {@literal $user.home/../../build/target/jboss-*}.
     * <p/>
     * Executes {@link junit.framework.Assert#fail()} if the base directory is null.
     *
     * @return the base directory.
     */
    File baseDir() {
        if (BASE_DIR == null)
            fail("Server not built");
        return BASE_DIR;
    }

    /**
     * A map of the schemas keyed by the schema name.
     *
     * @return a map of the schemas.
     */
    Map<String, File> jbossSchemas() {
        assertFalse("No schemas found.", JBOSS_SCHEMAS.isEmpty());
        return Collections.unmodifiableMap(JBOSS_SCHEMAS);
    }

    /**
     * A collection of the schema names.
     *
     * @return a collection of schema names.
     */
    Set<String> jbossSchemaNames() {
        assertFalse("No schemas found.", JBOSS_SCHEMAS.isEmpty());
        return JBOSS_SCHEMAS.keySet();
    }

    /**
     * A collections of the schema files.
     *
     * @return a collection of the schema files.
     */
    static Collection<File> jbossSchemaFiles() {
        assertFalse("No schemas found.", JBOSS_SCHEMAS.isEmpty());
        return JBOSS_SCHEMAS.values();
    }

    /**
     * Attempts to discover the path to the XSD file.
     *
     * @param xsdName the xsd file name.
     *
     * @return the file.
     */
    static URL discoverXsd(final String xsdName) {
        final File file = JBOSS_SCHEMAS.get(xsdName);
        URL url = null;
        try {
            if (file != null) {
                url = file.toURI().toURL();
            }
        } catch (IOException e) {
            url = null;
        }
        // Search
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (url == null)
            url = classLoader.getResource("docs/schema" + xsdName);
        if (url == null)
            url = classLoader.getResource("docs/" + xsdName);
        if (url == null)
            url = classLoader.getResource("schema/" + xsdName);
        if (url == null)
            url = classLoader.getResource(xsdName);
        assertNotNull(xsdName + " not found", url);
        return url;
    }

    /**
     * Simple JBoss XSD filter
     */
    private static class SchemaFilter implements FilenameFilter {
        private static final Pattern PATTERN = Pattern.compile("jboss.*\\.xsd$");
        static final SchemaFilter FILTER = new SchemaFilter();

        @Override
        public boolean accept(final File dir, final String name) {
            return PATTERN.matcher(name).find();
        }
    }

    protected static final class ErrorHandlerImpl implements ErrorHandler {
        @Override
        public void error(SAXParseException e) throws SAXException {
            fail(formatMessage(e));
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            fail(formatMessage(e));
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            System.out.println(formatMessage(e));
        }

        private String formatMessage(SAXParseException e) {
            StringBuffer sb = new StringBuffer();
            sb.append(e.getLineNumber()).append(':').append(e.getColumnNumber());
            if (e.getPublicId() != null)
                sb.append(" publicId='").append(e.getPublicId()).append('\'');
            if (e.getSystemId() != null)
                sb.append(" systemId='").append(e.getSystemId()).append('\'');
            sb.append(' ').append(e.getLocalizedMessage());
            return sb.toString();
        }
    }
}
