/**
 *
 */
package org.jboss.as.model.base.util;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.util.List;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseUtils;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
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
        super(NAMESPACE);
        ParseUtils.requireNoAttributes(reader);
        ParseUtils.requireNoContent(reader);
    }

    @Override
    protected Class<MockAnyElement> getElementClass() {
        return MockAnyElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEndElement();
    }

    protected void getUpdates(final List<? super AbstractSubsystemUpdate<MockAnyElement, ?>> objects) {
    }

    protected boolean isEmpty() {
        return true;
    }

    protected AbstractSubsystemAdd getAdd() {
        return null;
    }

    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
    }
}
