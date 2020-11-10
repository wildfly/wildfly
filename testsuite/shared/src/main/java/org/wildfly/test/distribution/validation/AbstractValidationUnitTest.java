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

package org.wildfly.test.distribution.validation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jboss.metadata.parser.util.XMLResourceResolver;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
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

    private static final Set<String> EXCLUDED_SCHEMA_FILES = new HashSet<>();
    private static final Set<String> FUTURE_SCHEMA_FILES = new HashSet<>();
    private static final Map<String, File> JBOSS_SCHEMAS_MAP = new HashMap<>();
    private static final Map<String, File> CURRENT_JBOSS_SCHEMAS_MAP = new HashMap<>();
    private static final Source[] SCHEMA_SOURCES;
    private static final Map<String, String> NAMESPACE_MAP = new HashMap<>();
    private static final Map<String, String> OUTDATED_NAMESPACES = new HashMap<>();


    private static final File JBOSS_DIST_DIR;

    static {
        // exclude JBoss EJB specific files which redefine the javaee namespace
        // triggering the https://issues.apache.org/jira/browse/XERCESJ-1130 bug
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb3-2_0.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb3-2_1.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb3-spec-2_0.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb3-spec-2_1.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-cache_1_0.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-container-interceptors_1_0.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-delivery-active_1_0.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-delivery-active_1_1.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-delivery-active_1_2.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-clustering_1_1.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-iiop_1_0.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-iiop_1_1.xsd");
        EXCLUDED_SCHEMA_FILES.add("jboss-ejb-iiop_1_2.xsd");
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

            final File[] xsds = schemaDir.listFiles(new SchemaFilter(EXCLUDED_SCHEMA_FILES.toArray(new String[0])));
            for (File xsd : xsds) {
                JBOSS_SCHEMAS_MAP.put(xsd.getName(), xsd);
            }
            Map<String, BigDecimal> mostRecentVersions = new HashMap<>();
            Map<String, String> mostRecentNames = new HashMap<>();
            Pattern pattern = Pattern.compile("(.*?)_(\\d+)_(\\d+).xsd");
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

        final List<Source> sources = new LinkedList<>();
        for (File file : CURRENT_JBOSS_SCHEMAS_MAP.values()) {
            sources.add(new StreamSource(file));
        }
        SCHEMA_SOURCES = sources.toArray(new Source[0]);
    }

    @SuppressWarnings("Convert2Lambda")
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
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }

        if (url == null && xsdName != null) {
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
            url = classLoader.getResource("docs/schema/" + fileName);
            if (url == null)
                url = classLoader.getResource("docs/" + fileName);
            if (url == null)
                url = classLoader.getResource("schema/" + fileName);
            if (url == null)
                url = classLoader.getResource(fileName);
            if (url == null) {
                final File schemaDir = new File(JBOSS_DIST_DIR, SCHEMAS_LOCATION);
                final File schema = new File(schemaDir, fileName);
                if (schema.exists()) {
                    try {
                        url = schema.toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        assertNotNull(xsdName + " not found", url);
        return url;
    }

    /**
     * Simple JBoss XSD filter
     */
    private static class SchemaFilter implements FilenameFilter {
        private static final Pattern PATTERN = Pattern.compile("(jboss|wildfly)-.*\\.xsd$");
        private final String[] exclusions;

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
        private final Path file;

        ErrorHandlerImpl() {
            this(null);
        }

        ErrorHandlerImpl(final Path file) {
            this.file = file;
        }

        @Override
        public void error(SAXParseException e) {
            fail(formatMessage(e));
        }

        @Override
        public void fatalError(SAXParseException e) {
            fail(formatMessage(e));
        }

        @Override
        public void warning(SAXParseException e) {
            System.out.println(formatMessage(e));
        }

        private String formatMessage(SAXParseException e) {
            StringBuilder sb = new StringBuilder();
            sb.append(e.getLineNumber()).append(':').append(e.getColumnNumber());
            if (e.getPublicId() != null)
                sb.append(" publicId='").append(e.getPublicId()).append('\'');
            if (e.getSystemId() != null)
                sb.append(" systemId='").append(e.getSystemId()).append('\'');
            sb.append(' ').append(e.getLocalizedMessage());
            sb.append(" a possible cause may be that a subsystem is not using the most up to date schema.");
            if (file != null) {
                try {
                    final List<String> lines = Files.readAllLines(file);
                    final int i = e.getLineNumber() - 1;
                    sb.append(System.lineSeparator());
                    sb.append("Possible line: ").append(lines.get(i));
                } catch (IOException ignore){}
            }
            return sb.toString();
        }
    }

    protected static void jbossXsdsTest() throws Exception {
        /* The test requires all modules to be loadable. Build a classloader
           with all modules.
        */
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader cl = initClassLoader()) {
            Thread.currentThread().setContextClassLoader(cl);
            for (File xsdFile : JBOSS_SCHEMAS_MAP.values()) {
                validateXsd(xsdFile);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    private static void validateXsd(final File xsdFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document document = parser.parse(xsdFile);

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setErrorHandler(new ErrorHandlerImpl());
        schemaFactory.setResourceResolver(new XMLResourceResolver());

        Schema schema = schemaFactory.newSchema(getXMLSchemaResource());
        Validator validator = schema.newValidator();
        validator.validate(new DOMSource(document));
    }

    private static URL getXMLSchemaResource() {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final URL resource = classLoader.getResource("schema/XMLSchema.xsd");
        assertNotNull("Can't locate resource schema/XMLSchema.xsd on " + classLoader, resource);
        return resource;
    }

    private static URLClassLoader initClassLoader() throws IOException {
        String path = System.getProperty("jboss.actual.dist");
        Path modules = Paths.get(path, "modules/system/layers/base");
        List<URL> urls = new ArrayList<>();
        Files.walkFileTree(modules, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (file.toString().endsWith(".jar")) {
                    urls.add(file.toFile().toURI().toURL());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        URL[] arr = new URL[urls.size()];
        urls.toArray(arr);
        return new URLClassLoader(arr);
    }

    protected void parseXml(String xmlName) throws SAXException, IOException {
        final File xmlFile = getXmlFile(xmlName);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setErrorHandler(new ErrorHandlerImpl(xmlFile.toPath()));
        schemaFactory.setResourceResolver(DEFAULT_RESOURCE_RESOLVER);
        Schema schema = schemaFactory.newSchema(SCHEMA_SOURCES);
        Validator validator = schema.newValidator();
        validator.setErrorHandler(new ErrorHandlerImpl(xmlFile.toPath()));
        validator.setFeature("http://apache.org/xml/features/validation/schema", true);
        validator.setResourceResolver(DEFAULT_RESOURCE_RESOLVER);
        validator.validate(new StreamSource(xmlFile));

        //noinspection ResultOfMethodCallIgnored
        xmlFile.delete();
    }

    private File getXmlFile(String xmlName) throws IOException {

        // Copy the input file to tmp, replacing system prop expressions on non-string fields
        // so they don't cause validation failures
        // TODO we should just pass an IS to Validator
        final File tmp = File.createTempFile(getClass().getSimpleName(), "xml");
        tmp.deleteOnExit();
        File target = new File(getBaseDir(), xmlName);
        try (BufferedWriter writer= Files.newBufferedWriter(tmp.toPath(), StandardCharsets.UTF_8)) {
            List<String> lines = Files.readAllLines(target.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                writer.write(fixExpressions(line));
                writer.newLine();
            }
        }
        return tmp;
    }

    private static String fixExpressions(String line) {
        String result = line.replace("${jboss.management.native.port:9999}", "9999");
        result = result.replace("${jboss.management.http.port:9990}", "9990");
        result = result.replace("${jboss.management.https.port:9993}", "9993");
        result = result.replace("${jboss.domain.master.protocol:remote}", "remote");
        result = result.replace("${jboss.domain.master.protocol:remote+http}", "remote+http");
        result = result.replace("${jboss.domain.master.port:9999}", "9999");
        result = result.replace("${jboss.domain.master.port:9990}", "9990");
        result = result.replace("${jboss.mail.server.host:localhost}", "localhost");
        result = result.replace("${jboss.mail.server.port:25}", "25");
        result = result.replace("${jboss.messaging.group.port:9876}", "9876");
        result = result.replace("${jboss.socket.binding.port-offset:0}", "0");
        result = result.replace("${jboss.http.port:8080}", "8080");
        result = result.replace("${jboss.https.port:8443}", "8443");
        result = result.replace("${jboss.remoting.port:4447}", "4447");
        result = result.replace("${jboss.ajp.port:8009}", "8009");
        result = result.replace("${jboss.mcmp.port:8090}", "8090");
        result = result.replace("${jboss.deployment.scanner.rollback.on.failure:false}", "false");
        result = result.replace("${wildfly.datasources.statistics-enabled:${wildfly.statistics-enabled:false}}", "false");
        result = result.replace("${wildfly.ejb.statistics-enabled:${wildfly.statistics-enabled:false}}", "false");
        result = result.replace("${wildfly.messaging-activemq.statistics-enabled:${wildfly.statistics-enabled:false}}", "false");
        result = result.replace("${wildfly.transactions.statistics-enabled:${wildfly.statistics-enabled:false}}", "false");
        result = result.replace("${wildfly.undertow.statistics-enabled:${wildfly.statistics-enabled:false}}", "false");
        result = result.replace("${wildfly.webservices.statistics-enabled:${wildfly.statistics-enabled:false}}", "false");
        result = result.replace("${env.MP_HEALTH_EMPTY_LIVENESS_CHECKS_STATUS:UP}", "UP");
        result = result.replace("${env.MP_HEALTH_EMPTY_READINESS_CHECKS_STATUS:UP}", "UP");
        return result;
    }
}
