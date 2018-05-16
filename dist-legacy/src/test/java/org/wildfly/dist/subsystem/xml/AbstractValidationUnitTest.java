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

package org.wildfly.dist.subsystem.xml;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Date: 23.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class AbstractValidationUnitTest {

    private static final String SCHEMAS_LOCATION = "docs/schema";
    private static final String JBOSS_DIST_PROP_NAME = "jboss.dist";
    private static final String FUTURE_SCHEMA_PROP_NAME = "jboss.test.xml.validation.future.schemas";

    private static final Set<String> EXCLUDED_SCHEMA_FILES = new HashSet<String>();
    private static final Set<String> FUTURE_SCHEMA_FILES = new HashSet<String>();
    private static final Map<String, File> JBOSS_SCHEMAS_MAP = new HashMap<String, File>();
    private static final Map<String, File> CURRENT_JBOSS_SCHEMAS_MAP = new HashMap<String, File>();
    private static Map<String, String> NAMESPACE_MAP = new HashMap<String, String>();
    private static Map<String, String> OUTDATED_NAMESPACES = new HashMap<>();


    private static final File JBOSS_DIST_DIR;

    static {
        // exclude JBoss EJB specific files which redefine the javaee namespace
        // triggering the https://issues.apache.org/jira/browse/XERCESJ-1130 bug
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb3-2_0.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb3-spec-2_0.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-cache_1_0.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-container-interceptors_1_0.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-delivery-active_1_0.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-delivery-active_1_1.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-clustering_1_1.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-iiop_1_0.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-iiop_1_1.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-pool_1_0.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-resource-adapter-binding_1_0.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-security_1_0.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-security_1_1.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-security-role_1_0.xsd");

        String coreVersion = System.getProperty("version.org.wildfly.core");
        if (coreVersion != null) {
            // We are testing a different version of core than was used in creating our standard configs
            // See if we are configured to specially handle newer schema versions in that core
            String excluded = System.getProperty(FUTURE_SCHEMA_PROP_NAME);
            if (excluded != null) {
                excluded = excluded.trim();
                String[] pairs = excluded.split(",");
                for (String pair : pairs) {
                    if (pair.length() > 0) {
                        // The format is <core_version>/<schema_file>
                        String[] tuple = pair.split("/");
                        // We only care about the pair if the <core_version> bit matches the
                        // value of the version.org.wildfly.core system property. This
                        // way if someone sets -Djboss.test.xml.validation.future.schemas
                        // in a CI test setup and then forgets to update the setup when
                        // the relevant core version gets released and integrated, the
                        // setting will no longer be effective and the no longer "future"
                        // xsd will get tested normally.
                        if (tuple.length == 2 && coreVersion.equals(tuple[0])) {
                            FUTURE_SCHEMA_FILES.add(tuple[1]);
                        }
                    }
                }
            }
        }

        NAMESPACE_MAP.put("http://java.sun.com/xml/ns/javaee/javaee_6.xsd", "schema/javaee_6.xsd");
        NAMESPACE_MAP.put("http://www.w3.org/2001/xml.xsd", "schema/xml.xsd");
        NAMESPACE_MAP.put("http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd", "schema/ejb-jar_3_1.xsd");
        NAMESPACE_MAP.put("http://www.jboss.org/j2ee/schema/jboss-common_7_0.xsd", "jboss-common_7_0.xsd");

        String asDir = System.getProperty(JBOSS_DIST_PROP_NAME);
        if (null == asDir) {
            JBOSS_DIST_DIR = null;
        } else {
            JBOSS_DIST_DIR = new File(asDir);
            if (!JBOSS_DIST_DIR.exists())
                throw new IllegalStateException("Directory set in '" + JBOSS_DIST_PROP_NAME + "' does not exist: " + JBOSS_DIST_DIR.getAbsolutePath());

            final File schemaDir = new File(JBOSS_DIST_DIR, SCHEMAS_LOCATION);

            final File[] xsds = schemaDir.listFiles(new SchemaFilter(EXCLUDED_SCHEMA_FILES.toArray(new String[EXCLUDED_SCHEMA_FILES.size()])));
            for (File xsd : xsds) {
                JBOSS_SCHEMAS_MAP.put(xsd.getName(), xsd);
            }
            Map<String, BigDecimal> mostRecentVersions = new HashMap<>();
            Map<String, String> mostRecentNames = new HashMap<>();
            Pattern pattern = Pattern.compile("(.*?)_(\\d)_(\\d).xsd");
            for(Map.Entry<String, File> entry : JBOSS_SCHEMAS_MAP.entrySet()) {
                if (FUTURE_SCHEMA_FILES.contains(entry.getKey())) {
                    // not "current"; it's future.
                    continue;
                }
                final Matcher match = pattern.matcher(entry.getKey());
                if(!match.matches()) {
                    continue;
                }
                String name = match.group(1);
                String major = match.group(2);
                String minor = match.group(3);
                BigDecimal version = new BigDecimal(major + "." + minor);
                BigDecimal current = mostRecentVersions.get(name);
                if(current == null || version.compareTo(current) > 0) {
                    mostRecentVersions.put(name, version);
                    mostRecentNames.put(name, entry.getKey());
                }
            }
            for (Map.Entry<String, File> entry : JBOSS_SCHEMAS_MAP.entrySet()) {
                if (FUTURE_SCHEMA_FILES.contains(entry.getKey())) {
                    // not "current" or "outdated"; it's future.
                    continue;
                }
                final Matcher match = pattern.matcher(entry.getKey());
                if (!match.matches()) {
                    continue;
                }
                String name = match.group(1);
                if (!mostRecentNames.get(name).equals(entry.getKey())) {
                    OUTDATED_NAMESPACES.put(entry.getKey(), mostRecentNames.get(name));
                } else {
                    CURRENT_JBOSS_SCHEMAS_MAP.put(entry.getKey(), entry.getValue());
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
            final DOMImplementationLS impl;
            try {
                impl = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS");
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("could not create LS input" ,e);
            }
            LSInput input = impl.createLSInput();

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
     * Executes {@link org.junit.Assert#fail()} if the base directory is null.
     *
     * @return the base directory.
     */
    protected static File getBaseDir() {
        assertNotNull("'" + JBOSS_DIST_PROP_NAME + "' is not set.", JBOSS_DIST_DIR);
        assertTrue("Directory set in '" + JBOSS_DIST_PROP_NAME + "' does not exist: " + JBOSS_DIST_DIR.getAbsolutePath(), JBOSS_DIST_DIR.exists());
        return JBOSS_DIST_DIR;
    }

    private static Map<String, File> getSchemas(boolean currentSchemaOnly) {
        assertFalse("No schemas found under " + getBaseDir().getAbsolutePath(), JBOSS_SCHEMAS_MAP.isEmpty());
        if(currentSchemaOnly) {
            return CURRENT_JBOSS_SCHEMAS_MAP;
        }
        return JBOSS_SCHEMAS_MAP;
    }

    /**
     * A collections of the schema files.
     *
     * @return a collection of the schema files.
     */
    static Collection<File> jbossSchemaFiles(boolean currentSchemasOnly) {
        return getSchemas(currentSchemasOnly).values();
    }

    /**
     * Attempts to discover the path to the XSD file.
     *
     * @param xsdName the xsd file name.
     * @return the file.
     */
    static URL discoverXsd(final String xsdName) {
        if (OUTDATED_NAMESPACES.containsKey(xsdName)) {
            throw new RuntimeException("Default configs are not in line with most recent schemas " + xsdName + " has been superseded by " + OUTDATED_NAMESPACES.get(xsdName));
        }
        final File file = JBOSS_SCHEMAS_MAP.get(xsdName);
        URL url = null;
        try {
            if (file != null) {
                url = file.toURI().toURL();
            }
        } catch (IOException e) {
            url = null;
        }
        String fileName = xsdName;
        int index = fileName.lastIndexOf("/");
        if(index == -1) {
            index = fileName.lastIndexOf("\\");
        }
        if(index != -1) {
            fileName = fileName.substring(index + 1);
        }
        // Search
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (url == null)
            url = classLoader.getResource("docs/schema/" + fileName);
        if (url == null)
            url = classLoader.getResource("docs/" + fileName);
        if (url == null)
            url = classLoader.getResource("schema/" + fileName);
        if (url == null)
            url = classLoader.getResource(fileName);
        assertNotNull(xsdName + " not found", url);
        return url;
    }

    /**
     * Simple JBoss XSD filter
     */
    private static class SchemaFilter implements FilenameFilter {
        private static final Pattern PATTERN = Pattern.compile("(jboss|wildfly)-.*\\.xsd$");
        private final String[] exclusions;

        SchemaFilter() {
            this.exclusions = new String[0];
        }

        SchemaFilter(final String[] exclusions) {
            this.exclusions = exclusions;
        }

        @Override
        public boolean accept(final File dir, final String name) {
            final boolean accepted = PATTERN.matcher(name).find();
            if (accepted) {
                // check for explicit excluded files
                for (final String excludedFile : exclusions) {
                    if (excludedFile.equals(name)) {
                        // file is in exclude list, so we don't accept this file
                        return false;
                    }
                }
            }
            return accepted;
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
            sb.append(" a possible cause may be that a subsystem is not using the most up to date schema.");
            return sb.toString();
        }
    }
}
