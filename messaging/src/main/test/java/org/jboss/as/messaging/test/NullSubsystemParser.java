package org.jboss.as.messaging.test;

import java.util.List;

import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;

/**
 * A noop implementation of XMLElementReader that simply skips over the content
 * that is passed to the NullSubsystemParser#readElement method.
 *
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class NullSubsystemParser<Object> implements XMLElementReader<List<?>> {

    /** {@inheritDoc} */
    public void readElement(XMLExtendedStreamReader arg0, List<?> arg1) throws XMLStreamException {
        // TODO Auto-generated method stub

    }

}
