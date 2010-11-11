package org.jboss.as.messaging.test;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class NullSubsystemElement extends AbstractSubsystemElement<NullSubsystemElement> {

    public NullSubsystemElement() {
        super("");
    }

    /** {@inheritDoc} */
    protected void getUpdates(List<? super AbstractSubsystemUpdate<NullSubsystemElement, ?>> list) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    protected boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    /** {@inheritDoc} */
    protected AbstractSubsystemAdd<NullSubsystemElement> getAdd() {
        return new NullSubsystemAdd();
    }

    /** {@inheritDoc} */
    protected <P> void applyRemove(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    protected Class<NullSubsystemElement> getElementClass() {
        return NullSubsystemElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        // TODO Auto-generated method stub

    }

    class NullSubsystemAdd extends AbstractSubsystemAdd<NullSubsystemElement> {

        protected NullSubsystemAdd() {
            super("");
        }

        /** {@inheritDoc} */
        protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        protected NullSubsystemElement createSubsystemElement() {
            return new NullSubsystemElement();
        }

    }
}
