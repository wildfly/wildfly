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
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Mock {@link AbstractModelElement} that can be used in tests of other
 * elements that accept children of type xs:any.
 *
 * @author Brian Stansberry
 */
public class MockSubsystemElement extends AbstractSubsystemElement<MockSubsystemElement> {

    private static final long serialVersionUID = -649277969243521207L;

    public static final String NAMESPACE = "urn:jboss:domain:mock-extension:1.0";

    public static final String ANOTHER_NAMESPACE = "urn:jboss:domain:another-mock-extension:1.0";

    public static final String SUBSYSTEM = "subsystem";

    public static final QName MOCK_ELEMENT_QNAME = new QName(NAMESPACE, SUBSYSTEM);
    public static final QName ANOTHER_MOCK_ELEMENT_QNAME = new QName(ANOTHER_NAMESPACE, SUBSYSTEM);

    public static String getSingleSubsystemXmlContent() {
        return "<subsystem xmlns=\"" + NAMESPACE + "\"/>";
    }

    public static String getAnotherSubsystemXmlContent() {
        return "<subsystem xmlns=\"" + ANOTHER_NAMESPACE + "\"/>";
    }

    public static String getFullXmlContent() {
        return getSingleSubsystemXmlContent() + getAnotherSubsystemXmlContent();
    }

    /**
     * Creates a new MockSubsystemElement
     */
    public MockSubsystemElement(String namespaceURI) {
        super(namespaceURI);
    }

    @Override
    protected Class<MockSubsystemElement> getElementClass() {
        return MockSubsystemElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEndElement();
    }

    @Override
    protected void getUpdates(List<? super AbstractSubsystemUpdate<MockSubsystemElement, ?>> list) {
    }

    @Override
    protected boolean isEmpty() {
        return true;
    }

    @Override
    protected AbstractSubsystemAdd getAdd() {
        return null;
    }

    @Override
    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
    }
}
