package org.jboss.as.weld.deployment;


import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.as.weld.WeldLogger;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.ClassAvailableActivation;
import org.jboss.weld.bootstrap.spi.Filter;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.bootstrap.spi.SystemPropertyActivation;
import org.jboss.weld.exceptions.DefinitionException;
import org.jboss.weld.metadata.BeansXmlImpl;
import org.jboss.weld.metadata.ClassAvailableActivationImpl;
import org.jboss.weld.metadata.FilterImpl;
import org.jboss.weld.metadata.ScanningImpl;
import org.jboss.weld.metadata.SystemPropertyActivationImpl;
import org.jboss.weld.xml.XmlMetadata;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import static java.util.Arrays.asList;
import static org.jboss.weld.logging.messages.XmlMessage.MULTIPLE_ALTERNATIVES;
import static org.jboss.weld.logging.messages.XmlMessage.MULTIPLE_DECORATORS;
import static org.jboss.weld.logging.messages.XmlMessage.MULTIPLE_INTERCEPTORS;
import static org.jboss.weld.logging.messages.XmlMessage.MULTIPLE_SCANNING;

/**
 * An implementation of the beans.xml parser written using SAX
 * <p/>
 * This class is NOT threadsafe, and should only be called in a single thread
 *
 * @author Pete Muir
 */
public class BeansXmlHandler extends DefaultHandler {

    public abstract static class Container {

        private final String uri;
        private final String localName;
        private final Collection<String> nestedElements;

        public Container(String uri, String localName, String... nestedElements) {
            this.uri = uri;
            this.localName = localName;
            this.nestedElements = asList(nestedElements);
        }

        public String getLocalName() {
            return localName;
        }

        public String getUri() {
            return uri;
        }

        /**
         * Called by startElement, the nested content is not available
         */
        public void processStartChildElement(String uri, String localName, String qName, Attributes attributes) {
        }

        /**
         * Called by endElement, the nested content is not available
         */
        public void processEndChildElement(String uri, String localName, String qName, String nestedText) {
        }

        public void handleMultiple() {

        }

        public Collection<String> getNestedElements() {
            return nestedElements;
        }

        @Override
        public String toString() {
            return "<" + localName + " />";
        }

        protected boolean isInNamespace(String uri) {
            return uri.length() == 0 || uri.equals(getUri());
        }

        protected static String trim(String str) {
            if (str == null) {
                return null;
            } else {
                return str.trim();
            }
        }

    }

    public static final String WELD_URI = "http://jboss.org/schema/weld/beans";
    public static final String JAVAEE_URI = "http://java.sun.com/xml/ns/javaee";

    /*
    * The containers we are parsing
    */
    private final Collection<Container> containers;

    /*
    * Storage for parsed info
    */
    private final List<Metadata<String>> interceptors;
    private final List<Metadata<String>> decorators;
    private final List<Metadata<String>> alternativeClasses;
    private final List<Metadata<String>> alternativeStereotypes;
    private final List<Metadata<Filter>> includes;
    private final List<Metadata<Filter>> excludes;
    private final URL file;
    final PropertyReplacer propertyReplacer;

    /*
    * Parser State
    */
    private Collection<Container> seenContainers;
    private Container currentContainer;
    private StringBuilder buffer;
    private Locator locator;

    public BeansXmlHandler(final URL file, final PropertyReplacer propertyReplacer) {
        this.file = file;
        this.propertyReplacer = propertyReplacer;
        this.interceptors = new ArrayList<Metadata<String>>();
        this.decorators = new ArrayList<Metadata<String>>();
        this.alternativeClasses = new ArrayList<Metadata<String>>();
        this.alternativeStereotypes = new ArrayList<Metadata<String>>();
        this.includes = new ArrayList<Metadata<Filter>>();
        this.excludes = new ArrayList<Metadata<Filter>>();
        this.seenContainers = new ArrayList<Container>();
        this.containers = new ArrayList<Container>();
        containers.add(new Container(JAVAEE_URI, "interceptors", "class") {

            @Override
            public void processEndChildElement(String uri, String localName, String qName, String nestedText) {
                if (isInNamespace(uri) && "class".equals(localName)) {
                    interceptors.add(new XmlMetadata<String>(qName, propertyReplacer.replaceProperties(trim(nestedText)), file, locator.getLineNumber()));
                }
            }

            @Override
            public void handleMultiple() {
                throw new DefinitionException(MULTIPLE_INTERCEPTORS, file + "@" + locator.getLineNumber());
            }

        });
        containers.add(new Container(JAVAEE_URI, "decorators", "class") {

            @Override
            public void processEndChildElement(String uri, String localName, String qName, String nestedText) {
                if (isInNamespace(uri) && "class".equals(localName)) {
                    decorators.add(new XmlMetadata<String>(qName, propertyReplacer.replaceProperties(trim(nestedText)), file, locator.getLineNumber()));
                }
            }

            @Override
            public void handleMultiple() {
                throw new DefinitionException(MULTIPLE_DECORATORS, file + "@" + locator.getLineNumber());
            }

        });
        containers.add(new Container(JAVAEE_URI, "alternatives", "class", "stereotype") {

            @Override
            public void processEndChildElement(String uri, String localName, String qName, String nestedText) {
                if (isInNamespace(uri) && "class".equals(localName)) {
                    alternativeClasses.add(new XmlMetadata<String>(qName, propertyReplacer.replaceProperties(trim(nestedText)), file, locator.getLineNumber()));
                } else if (isInNamespace(uri) && "stereotype".equals(localName)) {
                    alternativeStereotypes.add(new XmlMetadata<String>(qName, propertyReplacer.replaceProperties(trim(nestedText)), file, locator.getLineNumber()));
                }
            }

            @Override
            public void handleMultiple() {
                throw new DefinitionException(MULTIPLE_ALTERNATIVES, file + "@" + locator.getLineNumber());
            }

        });
        containers.add(new Container(WELD_URI, "scan") {

            String name;
            String pattern;
            Collection<Metadata<SystemPropertyActivation>> systemPropertyActivations;
            Collection<Metadata<ClassAvailableActivation>> classAvailableActivations;

            @Override
            public void processStartChildElement(String uri, String localName, String qName, Attributes attributes) {
                if (isFilterElement(uri, localName)) {
                    name = trim(attributes.getValue("name"));
                    pattern = trim(attributes.getValue("pattern"));
                    systemPropertyActivations = new ArrayList<Metadata<SystemPropertyActivation>>();
                    classAvailableActivations = new ArrayList<Metadata<ClassAvailableActivation>>();
                } else if (isInNamespace(uri) && "if-system-property".equals(localName)) {
                    String systemPropertyName = trim(attributes.getValue("name"));
                    String systemPropertyValue = trim(attributes.getValue("value"));
                    Metadata<SystemPropertyActivation> systemPropertyActivation = new XmlMetadata<SystemPropertyActivation>(qName, new SystemPropertyActivationImpl(systemPropertyName, systemPropertyValue), file, locator.getLineNumber());
                    systemPropertyActivations.add(systemPropertyActivation);
                } else if (isInNamespace(uri) && "if-class-available".equals(localName)) {
                    String className = trim(attributes.getValue("name"));
                    Metadata<ClassAvailableActivation> classAvailableActivation = new XmlMetadata<ClassAvailableActivation>(qName, new ClassAvailableActivationImpl(className), file, locator.getLineNumber());
                    classAvailableActivations.add(classAvailableActivation);
                }
            }

            @Override
            public void processEndChildElement(String uri, String localName, String qName, String nestedText) {
                if (isFilterElement(uri, localName)) {
                    Metadata<Filter> filter = new XmlMetadata<Filter>(qName, new FilterImpl(pattern, name, systemPropertyActivations, classAvailableActivations), file, locator.getLineNumber());
                    if ("include".equals(localName)) {
                        includes.add(filter);
                    } else if ("exclude".equals(localName)) {
                        excludes.add(filter);
                    }
                    // reset
                    name = null;
                    pattern = null;
                    systemPropertyActivations = null;
                    classAvailableActivations = null;
                }
            }

            private boolean isFilterElement(String uri, String localName) {
                return isInNamespace(uri) && ("include".equals(localName) || "exclude".equals(localName));
            }

            @Override
            public void handleMultiple() {
                throw new DefinitionException(MULTIPLE_SCANNING, file + "@" + locator.getLineNumber());
            }

        });
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (currentContainer == null) {
            Container container = getContainer(uri, localName);
            if (container != null) {
                if (seenContainers.contains(container)) {
                    container.handleMultiple();
                }
                currentContainer = container;
            }
        } else {
            currentContainer.processStartChildElement(uri, localName, qName, attributes);
            // The current container is interested in the content of this element
            if (currentContainer.getNestedElements().contains(localName)) {
                buffer = new StringBuilder();
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (currentContainer != null) {
            currentContainer.processEndChildElement(uri, localName, qName, buffer != null ? buffer.toString() : null);

            // The current container was interested in this element
            if (currentContainer.getNestedElements().contains(localName)) {
                buffer = null;
            }
            Container container = getContainer(uri, localName);
            if (container != null) {
                // We are exiting the container, record it so we know it's already been declared (for error reporting)
                seenContainers.add(container);
                // And stop work until we find another container of interest
                currentContainer = null;
            }
        }
    }

    private Container getContainer(String uri, String localName) {
        return getContainer(uri, localName, containers);
    }

    private static Container getContainer(String uri, String localName, Collection<Container> containers) {
        for (Container container : containers) {
            if (uri.length() == 0) {
                if (container.getLocalName().equals(localName)) {
                    return container;
                }
            } else {
                if (container.getLocalName().equals(localName) && container.getUri().equals(uri)) {
                    return container;
                }
            }
        }
        return null;
    }

    public BeansXml createBeansXml() {
        return new BeansXmlImpl(alternativeClasses, alternativeStereotypes, decorators, interceptors, new ScanningImpl(includes, excludes));
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (buffer != null) {
            buffer.append(ch, start, length);
        }
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        WeldLogger.DEPLOYMENT_LOGGER.beansXmlValidationWarning(file, e.getLineNumber(), e.getMessage());
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        if (e.getMessage().equals("cvc-elt.1: Cannot find the declaration of element 'beans'.")) {
            // Ignore the errors we get when there is no schema defined
            return;
        }
        WeldLogger.DEPLOYMENT_LOGGER.beansXmlValidationError(file, e.getLineNumber(), e.getMessage());
    }

}
