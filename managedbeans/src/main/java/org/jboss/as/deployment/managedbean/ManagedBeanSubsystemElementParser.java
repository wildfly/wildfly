package org.jboss.as.deployment.managedbean;

import javax.xml.stream.XMLStreamException;
import org.jboss.as.model.ParseResult;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;


public class ManagedBeanSubsystemElementParser implements XMLElementReader<ParseResult<ManagedBeansSubsystemElement>> {

    /** {@inheritDoc} */
    public void readElement(final XMLExtendedStreamReader xmlExtendedStreamReader, final ParseResult<ManagedBeansSubsystemElement> result) throws XMLStreamException {
        result.setResult(new ManagedBeansSubsystemElement(xmlExtendedStreamReader));
    }
}
