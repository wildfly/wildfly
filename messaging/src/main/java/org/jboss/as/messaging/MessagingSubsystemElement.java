package org.jboss.as.messaging;

import java.util.List;

import java.util.NavigableMap;
import java.util.TreeMap;
import javax.xml.stream.XMLStreamException;

import org.hornetq.core.server.JournalType;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
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

    private DirectoryElement bindingsDirectory;
    private DirectoryElement journalDirectory;
    private DirectoryElement largeMessagesDirectory;
    private DirectoryElement pagingDirectory;
    private boolean clustered;
    private int journalMinFiles = -1;
    private int journalFileSize = -1;
    private JournalType journalType;

    private final NavigableMap<String, AbstractTransportElement<?>> acceptors = new TreeMap<String, AbstractTransportElement<?>>();
    private final NavigableMap<String, AbstractTransportElement<?>> connectors = new TreeMap<String, AbstractTransportElement<?>>();
    private final NavigableMap<String, AddressSettingsElement> addressSettings = new TreeMap<String, AddressSettingsElement>();
    private final NavigableMap<String, SecuritySettingsElement> securitySettings = new TreeMap<String, SecuritySettingsElement>();

    public MessagingSubsystemElement() {
        super(Namespace.MESSAGING_1_0.getUriString());
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

        if(bindingsDirectory != null) {
            streamWriter.writeStartElement(Element.BINDINGS_DIRECTORY.getLocalName());
            bindingsDirectory.writeContent(streamWriter);
        }
        if(largeMessagesDirectory != null) {
            streamWriter.writeStartElement(Element.LARGE_MESSAGES_DIRECTORY.getLocalName());
            largeMessagesDirectory.writeContent(streamWriter);
        }
        if(pagingDirectory != null) {
            streamWriter.writeStartElement(Element.PAGING_DIRECTORY.getLocalName());
            pagingDirectory.writeContent(streamWriter);
        }
        if(journalDirectory != null) {
            streamWriter.writeStartElement(Element.JOURNAL_DIRECTORY.getLocalName());
            journalDirectory.writeContent(streamWriter);
        }

       // Note we have to write this even if it wasn't in the original content
       // since the "null" possibility isn't preserved
       ElementUtils.writeSimpleElement(Element.CLUSTERED, String.valueOf(isClustered()), streamWriter);

       // Note we have to write this even if it wasn't in the original content
       // since the "null" possibility isn't preserved
       ElementUtils.writeSimpleElement(Element.JOURNAL_MIN_FILES, String.valueOf(getJournalMinFiles()), streamWriter);

       JournalType jt = getJournalType();
       if (jt != null) {
           ElementUtils.writeSimpleElement(Element.JOURNAL_TYPE, jt.toString(), streamWriter);
       }

       // Note we have to write this even if it wasn't in the original content
       // since the "null" possibility isn't preserved
       ElementUtils.writeSimpleElement(Element.JOURNAL_FILE_SIZE, String.valueOf(getJournalFileSize()), streamWriter);

        if (acceptors.size() > 0) {
            streamWriter.writeStartElement(Element.ACCEPTORS.getLocalName());
            for(AbstractTransportElement<?> acceptor : acceptors.values()) {
                streamWriter.writeStartElement(acceptor.getElement().getLocalName());
                acceptor.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }

        if (addressSettings.size() > 0) {
            streamWriter.writeStartElement(Element.ADDRESS_SETTINGS.getLocalName());
            for(AddressSettingsElement addressSettingsElement : addressSettings.values()) {
                streamWriter.writeStartElement(Element.ADDRESS_SETTING.getLocalName());
                addressSettingsElement.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }

        if (connectors.size() > 0) {
            streamWriter.writeStartElement(Element.CONNECTORS.getLocalName());
            for(AbstractTransportElement<?> connector : connectors.values()) {
                streamWriter.writeStartElement(connector.getElement().getLocalName());
                connector.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }

        if (securitySettings.size() > 0) {
            streamWriter.writeStartElement(Element.SECURITY_SETTINGS.getLocalName());
            for(SecuritySettingsElement securitySettingElement : securitySettings.values()) {
                streamWriter.writeStartElement(Element.SECURITY_SETTING.getLocalName());
                securitySettingElement.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }

        streamWriter.writeEndElement();
    }

    /** {@inheritDoc} */
    @Override
    protected void getUpdates(List<? super AbstractSubsystemUpdate<MessagingSubsystemElement, ?>> list) {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected AbstractSubsystemAdd<MessagingSubsystemElement> getAdd() {
        return new MessagingSubsystemAdd();
    }

    @Override
    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
    }

    public DirectoryElement getBindingsDirectory() {
        return bindingsDirectory;
    }

    public void setBindingsDirectory(DirectoryElement bindingsDirectory) {
        this.bindingsDirectory = bindingsDirectory;
    }

    public DirectoryElement getJournalDirectory() {
        return journalDirectory;
    }

    public void setJournalDirectory(DirectoryElement journalDirectory) {
        this.journalDirectory = journalDirectory;
    }

    public DirectoryElement getLargeMessagesDirectory() {
        return largeMessagesDirectory;
    }

    public void setLargeMessagesDirectory(DirectoryElement largeMessagesDirectory) {
        this.largeMessagesDirectory = largeMessagesDirectory;
    }

    public DirectoryElement getPagingDirectory() {
        return pagingDirectory;
    }

    public void setPagingDirectory(DirectoryElement pagingDirectory) {
        this.pagingDirectory = pagingDirectory;
    }

    public boolean isClustered() {
        return clustered;
    }

    public void setClustered(boolean clustered) {
        this.clustered = clustered;
    }

    public int getJournalMinFiles() {
        return journalMinFiles;
    }

    public void setJournalMinFiles(int journalMinFiles) {
        this.journalMinFiles = journalMinFiles;
    }

    public int getJournalFileSize() {
        return journalFileSize;
    }

    public void setJournalFileSize(int journalFileSize) {
        this.journalFileSize = journalFileSize;
    }

    public JournalType getJournalType() {
        return journalType;
    }

    public void setJournalType(JournalType journalType) {
        this.journalType = journalType;
    }

    boolean addAcceptor(AbstractTransportElement<?> acceptor) {
        if(acceptors.containsKey(acceptor.getName())) return false;
        acceptors.put(acceptor.getName(), acceptor);
        return true;
    }

    boolean removeAcceptor(final String name) {
        return acceptors.remove(name) != null;
    }

    boolean addConnector(AbstractTransportElement<?> connector) {
        if(connectors.containsKey(connector.getName())) return false;
        connectors.put(connector.getName(), connector);
        return true;
    }

    boolean addAddressSettings(AddressSettingsElement spec) {
        if(addressSettings.containsKey(spec.getMatch())) return false;
        addressSettings.put(spec.getMatch(), spec);
        return true;
    }

    boolean removeAddressSettings(final String match) {
        return addressSettings.remove(match) != null;
    }

    boolean addSecuritySetting(SecuritySettingsElement spec) {
        if(securitySettings.containsKey(spec.getMatch())) return false;
        securitySettings.put(spec.getMatch(), spec);
        return true;
    }

    boolean removeSecuritySetting(final String match) {
        return securitySettings.remove(match) != null;
    }

}
