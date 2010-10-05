package org.jboss.as.messaging.test;

import java.util.List;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class NullSubsystemElement<T> extends AbstractSubsystemElement<NullSubsystemElement<Object>> {

    public NullSubsystemElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super("");
        reader.discardRemainder();
    }

    @Override
    protected Class<NullSubsystemElement<Object>> getElementClass() {
        Class<NullSubsystemElement<Object>> c = (Class<NullSubsystemElement<Object>>) getClass();
        return c;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    /** {@inheritDoc} */
    protected void getUpdates(List<? super AbstractSubsystemUpdate<NullSubsystemElement<Object>, ?>> list) {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    protected boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    protected AbstractSubsystemAdd<NullSubsystemElement<Object>> getAdd() {
        return null;
    }

    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
    }
}
