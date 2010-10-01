package org.jboss.as.messaging;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The subsystem element for the messaging configuration and activation.
 *
 * @author scott.stark@jboss.org
 * @author Emanuel Muckenhuber
 */
public class MessagingSubsystemElement extends AbstractSubsystemElement<MessagingSubsystemElement> {

    private static final long serialVersionUID = 8225457441023207312L;

    /** The service name "jboss.messaging". */
    public static final ServiceName JBOSS_MESSAGING = ServiceName.JBOSS.append("messaging");

    /** The HornetQ Configuration. */
    private final Configuration configuration;

    public MessagingSubsystemElement() {
        super(Namespace.MESSAGING_1_0.getUriString());
        this.configuration = new ConfigurationImpl();
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<MessagingSubsystemElement> getElementClass() {
        return MessagingSubsystemElement.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        final ConfigurationElementWriter configElement = new ConfigurationElementWriter(configuration);
        configElement.writeContent(streamWriter);
        streamWriter.writeEndElement();
    }

    /** {@inheritDoc} */
    protected void getClearingUpdates(List<? super AbstractSubsystemUpdate<MessagingSubsystemElement, ?>> list) {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    protected boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

}
