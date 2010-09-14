/**
 *
 */
package org.jboss.as.model.base.util;

import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Mock {@link AbstractModelElement} that can be used in tests of other
 * elements that accept children of type xs:any.
 *
 * @author Brian Stansberry
 */
public class MockAnyElement extends AbstractSubsystemElement<MockAnyElement> {

    private static final long serialVersionUID = -649277969243521207L;

    public static final String NAMESPACE = "urn:jboss:domain:mock-extension:1.0";

    public static final String MOCK_ELEMENT = "mock";
    public static final String ANOTHER_MOCK_ELEMENT = "another-mock";

    public static final QName MOCK_ELEMENT_QNAME = new QName(NAMESPACE, MOCK_ELEMENT);
    public static final QName ANOTHER_MOCK_ELEMENT_QNAME = new QName(NAMESPACE, ANOTHER_MOCK_ELEMENT);

    public static String getSimpleXmlContent() {
        return "<mock:mock/>";
    }

    public static String getFullXmlContent() {
        return "<mock:mock/><mock:another-mock/>";
    }

    /**
     * Creates a new MockAnyElement by parsing an xml stream
     *
     * @param reader stream reader used to read the xml
     *
     * @throws XMLStreamException if an error occurs
     */
    public MockAnyElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        requireNoAttributes(reader);
        requireNoContent(reader);
    }

    @Override
    protected Class<MockAnyElement> getElementClass() {
        return MockAnyElement.class;
    }

    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<MockAnyElement>> target,
            MockAnyElement other) {
        // no mutable state
    }

    @Override
    public long elementHash() {
        return 19;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEndElement();
    }

    @Override
    public void activate(ServiceActivatorContext context) {
        // no-op
    }
}
