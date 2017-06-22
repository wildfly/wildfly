package org.jboss.as.test.integration.deployment.classloading.jaxp;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A servlet that instantiates some of the JAXP services and prints the class name of the resulting
 * instance.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@SuppressWarnings("serial")
@WebServlet("/factory")
public class FactoryServlet extends HttpServlet {

    static class TestHandler implements ContentHandler {

        private Class<?> attributesImpl;

        @Override
        public void setDocumentLocator(Locator locator) {
        }

        @Override
        public void startDocument() throws SAXException {
        }

        @Override
        public void endDocument() throws SAXException {
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
        }

        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            attributesImpl = atts.getClass();
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        }

        @Override
        public void processingInstruction(String target, String data) throws SAXException {
        }

        @Override
        public void skippedEntity(String name) throws SAXException {
        }

        public Class<?> getAttributesImpl() {
            return attributesImpl;
        }

    }

    public enum Factory {
        Datatype(DatatypeFactory.class) {
            @Override
            public void test(String implClass, PrintWriter out) throws Exception {
                DatatypeFactory f = implClass == null ? DatatypeFactory.newInstance()
                        : DatatypeFactory.newInstance(implClass, Thread.currentThread().getContextClassLoader());
                out.write(f.newXMLGregorianCalendar().getClass().getName());
            }
        }, //
        DocumentBuilder(DocumentBuilderFactory.class) {
            @Override
            public void test(String implClass, PrintWriter out) throws Exception {
                DocumentBuilderFactory f = implClass == null ? DocumentBuilderFactory.newInstance()
                        : DocumentBuilderFactory.newInstance(implClass, Thread.currentThread().getContextClassLoader());
                out.write(f.newDocumentBuilder().getClass().getName());
            }
        }, //
        SAXParser(SAXParserFactory.class) {
            @Override
            public void test(String implClass, PrintWriter out) throws Exception {
                SAXParserFactory f = implClass == null ? SAXParserFactory.newInstance()
                        : SAXParserFactory.newInstance(implClass, Thread.currentThread().getContextClassLoader());
                out.write(f.newSAXParser().getClass().getName());
            }
        }, //
        SAXTransformer(SAXTransformerFactory.class) {
            @Override
            public void test(String implClass, PrintWriter out) throws Exception {
                TransformerFactory f = implClass == null ? SAXTransformerFactory.newInstance()
                        : SAXTransformerFactory.newInstance(implClass, Thread.currentThread().getContextClassLoader());
                out.write(f.newTransformer().getClass().getName());
            }
        },
        Schema(SchemaFactory.class) {
            @Override
            public void test(String implClass, PrintWriter out) throws Exception {
                SchemaFactory f = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                URL url = this.getClass().getResource("/schema.xsd");
                out.write(f.newSchema(url).getClass().getName());
            }
        }, //
        XMLEvent(XMLEventFactory.class) {
            @Override
            public void test(String implClass, PrintWriter out) throws Exception {
                XMLEventFactory f = implClass == null ? XMLEventFactory.newInstance()
                        : XMLEventFactory.newFactory(implClass, Thread.currentThread().getContextClassLoader());
                out.write(f.createStartDocument().getClass().getName());
            }
        },
        XMLInput(XMLInputFactory.class) {
            @Override
            public void test(String implClass, PrintWriter out) throws Exception {
                XMLInputFactory f = implClass == null ? XMLInputFactory.newInstance()
                        : XMLInputFactory.newFactory(implClass, Thread.currentThread().getContextClassLoader());
                try (InputStream in = this.getClass().getResourceAsStream("/input.xml")) {
                    out.write(f.createXMLStreamReader(in).getClass().getName());
                }
            }
        },
        XMLOutput(XMLOutputFactory.class) {
            @Override
            public void test(String implClass, PrintWriter out) throws Exception {
                XMLOutputFactory f = implClass == null ? XMLOutputFactory.newInstance()
                        : XMLOutputFactory.newFactory(implClass, Thread.currentThread().getContextClassLoader());
                try (Writer w = new StringWriter()) {
                    out.write(f.createXMLStreamWriter(w).getClass().getName());
                }
            }
        },
        XMLReader(XMLReaderFactory.class) {
            @Override
            public void test(String implClass, PrintWriter out) throws Exception {
                org.xml.sax.XMLReader r = implClass == null ? XMLReaderFactory.createXMLReader()
                        : XMLReaderFactory.createXMLReader(implClass);
                TestHandler handler = new TestHandler();
                r.setContentHandler(handler);
                r.parse(new InputSource(new StringReader("<root key=\"val\"/>")));
                out.write(handler.getAttributesImpl().getName());
            }
        },
        XPath(XPathFactory.class) {
            @Override
            public void test(String implClass, PrintWriter out) throws Exception {
                XPathFactory f = implClass == null ? XPathFactory.newInstance()
                        : XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI, implClass,
                                Thread.currentThread().getContextClassLoader());
                out.write(f.newXPath().getClass().getName());
            }
        }; //
        public static Factory ofFqName(String fqName) {
            for (Factory factory : values()) {
                if (factory.cl.getName().equals(fqName)) {
                    return factory;
                }
            }
            return null;
        }

        private final Class<?> cl;

        private Factory(Class<?> cl) {
            this.cl = cl;
        }

        public abstract void test(String implClass, PrintWriter out) throws Exception;

        public String toString() {
            return cl.getName();
        }

    }

    private static Logger log = Logger.getLogger(FactoryServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        final String ctx = req.getContextPath();
        final String factoryClass = req.getParameter("factory");
        final String factoryImpl = req.getParameter("impl");
        log.debugf("Got factory class name %s and impl %s in context %s", factoryClass, factoryImpl, ctx);
        Factory factory = Factory.ofFqName(factoryClass);
        Assert.assertNotNull("Factory query string parameter either not specified or invalid", factory);
        try {
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("utf-8");
            factory.test(factoryImpl, resp.getWriter());
        } catch (Exception e) {
            e.printStackTrace(resp.getWriter());
        }
    }

}
