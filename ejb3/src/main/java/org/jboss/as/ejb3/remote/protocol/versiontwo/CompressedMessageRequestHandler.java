package org.jboss.as.ejb3.remote.protocol.versiontwo;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.protocol.AbstractMessageHandler;
import org.jboss.as.ejb3.remote.protocol.versionone.ChannelAssociation;

/**
 * A {@link org.jboss.as.ejb3.remote.protocol.MessageHandler} responsible for handling messages which have been compressed using the EJB protocol
 *
 * @author: Jaikiran Pai
 */
class CompressedMessageRequestHandler extends AbstractMessageHandler {

    private VersionTwoProtocolChannelReceiver ejbProtocolHandler;

    CompressedMessageRequestHandler(final VersionTwoProtocolChannelReceiver ejbProtocolHandler) {
        this.ejbProtocolHandler = ejbProtocolHandler;
    }

    @Override
    public void processMessage(final ChannelAssociation channelAssociation, final InputStream inputStream) throws IOException {
        EjbLogger.EJB3_INVOCATION_LOGGER.trace("Received a compressed message stream");
        // use an inflater inputstream to inflate the contents
        final InputStream inflaterInputStream = new InflaterInputStream(inputStream);
        // let the EJB protocol handler process the stream
        this.ejbProtocolHandler.processMessage(channelAssociation.getChannel(), inflaterInputStream);
    }
}
