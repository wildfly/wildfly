package org.jboss.as.messaging;

import java.util.List;

import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import javax.xml.stream.XMLStreamException;

import org.hornetq.core.security.Role;
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

    private String bindingsDirectory;
    private String journalDirectory;
    private String largeMessagesDirectory;
    private String pagingDirectory;
    private boolean clustered;
    private int journalMinFiles = -1;
    private int journalFileSize = -1;
    private JournalType journalType;

    private final NavigableMap<String, TransportElement> acceptors = new TreeMap<String, TransportElement>();
    private final NavigableMap<String, TransportElement> connectors = new TreeMap<String, TransportElement>();
    private final NavigableMap<String, AddressSettingsElement> addressSettings = new TreeMap<String, AddressSettingsElement>();
    private final NavigableMap<String, SecuritySettingElement> securitySettings = new TreeMap<String, SecuritySettingElement>();

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
        ElementUtils.writeSimpleElement(Element.BINDINGS_DIRECTORY, getBindingsDirectory(), streamWriter);

       // Note we have to write this even if it wasn't in the original content
       // since the "null" possibility isn't preserved
       ElementUtils.writeSimpleElement(Element.CLUSTERED, String.valueOf(isClustered()), streamWriter);

       ElementUtils.writeSimpleElement(Element.JOURNAL_DIRECTORY, getJournalDirectory(), streamWriter);

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

       ElementUtils.writeSimpleElement(Element.LARGE_MESSAGES_DIRECTORY, getLargeMessagesDirectory(), streamWriter);

       ElementUtils.writeSimpleElement(Element.PAGING_DIRECTORY, getPagingDirectory(), streamWriter);

        if (acceptors.size() > 0) {
            streamWriter.writeStartElement(Element.ACCEPTORS.getLocalName());
            for(TransportElement acceptor : acceptors.values()) {
                streamWriter.writeStartElement(Element.ACCEPTOR.getLocalName());
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
            for(TransportElement connector : connectors.values()) {
                streamWriter.writeStartElement(Element.CONNECTOR.getLocalName());
                connector.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }

        if (securitySettings.size() > 0) {
            streamWriter.writeStartElement(Element.SECURITY_SETTINGS.getLocalName());
            for(SecuritySettingElement securitySettingElement : securitySettings.values()) {
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

    public String getBindingsDirectory() {
        return bindingsDirectory;
    }

    public void setBindingsDirectory(String bindingDirectory) {
        this.bindingsDirectory = bindingDirectory;
    }

    public String getJournalDirectory() {
        return journalDirectory;
    }

    public void setJournalDirectory(String journalDirectory) {
        this.journalDirectory = journalDirectory;
    }

    public String getLargeMessagesDirectory() {
        return largeMessagesDirectory;
    }

    public void setLargeMessagesDirectory(String largeMessagesDirectory) {
        this.largeMessagesDirectory = largeMessagesDirectory;
    }

    public String getPagingDirectory() {
        return pagingDirectory;
    }

    public void setPagingDirectory(String pagingDirectory) {
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

    TransportElement addAcceptor(final String name) {
        final TransportElement acceptorElement = new TransportElement(name);
        acceptors.put(name, acceptorElement);
        return acceptorElement;
    }

    void removeAcceptor(final String name) {
        acceptors.remove(name);
    }

    TransportElement getAcceptor(final String name) {
        return acceptors.get(name);
    }

    TransportElement addConnector(final String name) {
        final TransportElement acceptorElement = new TransportElement(name);
        connectors.put(name, acceptorElement);
        return acceptorElement;
    }

    void removeConnector(final String name) {
        connectors.remove(name);
    }

    TransportElement getConnector(final String name) {
        return connectors.get(name);
    }

    AddressSettingsElement addAddressSettings(final String match) {
        final AddressSettingsElement addressSettingsElement = new AddressSettingsElement(match);
        addressSettings.put(match, addressSettingsElement);
        return addressSettingsElement;
    }

    void removeAddressSettings(final String match) {
        addressSettings.remove(match);
    }

    AddressSettingsElement getAddressSettingsElement(final String match) {
        return addressSettings.get(match);
    }

    public SecuritySettingElement addSecuritySetting(final String match, final Set<Role> roles) {
        final SecuritySettingElement securitySettingElement = new SecuritySettingElement(match, roles);
        securitySettings.put(match, securitySettingElement);
        return securitySettingElement;
    }

    void removeSecuritySetting(final String match) {
        securitySettings.remove(match);
    }

    SecuritySettingElement getSecuritySetting(final String match) {
        return securitySettings.get(match);
    }
}
