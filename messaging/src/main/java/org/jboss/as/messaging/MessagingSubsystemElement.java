package org.jboss.as.messaging;

import java.util.List;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
    /** The core queue name base. */
    public static final ServiceName CORE_QUEUE_BASE = JBOSS_MESSAGING.append("queue");

    private DirectoryElement bindingsDirectory;
    private DirectoryElement journalDirectory;
    private DirectoryElement largeMessagesDirectory;
    private DirectoryElement pagingDirectory;
    private Boolean clustered;
    private Boolean persistenceEnabled;
    private int journalMinFiles = -1;
    private int journalFileSize = -1;
    private JournalType journalType;

    private final NavigableMap<String, AbstractTransportElement<?>> acceptors = new TreeMap<String, AbstractTransportElement<?>>();
    private final NavigableMap<String, AbstractTransportElement<?>> connectors = new TreeMap<String, AbstractTransportElement<?>>();
    private final NavigableMap<String, AddressSettingsElement> addressSettings = new TreeMap<String, AddressSettingsElement>();
    private final NavigableMap<String, SecuritySettingsElement> securitySettings = new TreeMap<String, SecuritySettingsElement>();

    private final NavigableMap<String, QueueElement> queues = new TreeMap<String, QueueElement>();

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

        if (bindingsDirectory != null) {
            streamWriter.writeEmptyElement(Element.BINDINGS_DIRECTORY.getLocalName());
            bindingsDirectory.writeContent(streamWriter);
        }
        if (largeMessagesDirectory != null) {
            streamWriter.writeEmptyElement(Element.LARGE_MESSAGES_DIRECTORY.getLocalName());
            largeMessagesDirectory.writeContent(streamWriter);
        }
        if (pagingDirectory != null) {
            streamWriter.writeEmptyElement(Element.PAGING_DIRECTORY.getLocalName());
            pagingDirectory.writeContent(streamWriter);
        }
        if (journalDirectory != null) {
            streamWriter.writeEmptyElement(Element.JOURNAL_DIRECTORY.getLocalName());
            journalDirectory.writeContent(streamWriter);
        }
        if(persistenceEnabled != null) {
            ElementUtils.writeSimpleElement(Element.PERSISTENCE_ENABLED, String.valueOf(persistenceEnabled), streamWriter);
        }
        if(clustered != null) {
            ElementUtils.writeSimpleElement(Element.CLUSTERED, String.valueOf(isClustered()), streamWriter);
        }

        if(journalMinFiles != -1) {
            ElementUtils.writeSimpleElement(Element.JOURNAL_MIN_FILES, String.valueOf(getJournalMinFiles()), streamWriter);
        }

        JournalType jt = getJournalType();
        if (jt != null) {
            ElementUtils.writeSimpleElement(Element.JOURNAL_TYPE, jt.toString(), streamWriter);
        }

        if(journalFileSize != -1) {
            ElementUtils.writeSimpleElement(Element.JOURNAL_FILE_SIZE, String.valueOf(getJournalFileSize()), streamWriter);
        }

        if (connectors.size() > 0) {
            streamWriter.writeStartElement(Element.CONNECTORS.getLocalName());
            for (AbstractTransportElement<?> connector : connectors.values()) {
                streamWriter.writeStartElement(connector.getElement().getLocalName());
                connector.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }

        if (acceptors.size() > 0) {
            streamWriter.writeStartElement(Element.ACCEPTORS.getLocalName());
            for (AbstractTransportElement<?> acceptor : acceptors.values()) {
                streamWriter.writeStartElement(acceptor.getElement().getLocalName());
                acceptor.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }

        if(queues.size() > 0) {
            streamWriter.writeStartElement(Element.QUEUES.getLocalName());
            for(QueueElement queue : queues.values()) {
                streamWriter.writeStartElement(Element.QUEUE.getLocalName());
                queue.writeContent(streamWriter);
            }
        }

        if (addressSettings.size() > 0) {
            streamWriter.writeStartElement(Element.ADDRESS_SETTINGS.getLocalName());
            for (AddressSettingsElement addressSettingsElement : addressSettings.values()) {
                streamWriter.writeStartElement(Element.ADDRESS_SETTING.getLocalName());
                addressSettingsElement.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }

        if (securitySettings.size() > 0) {
            streamWriter.writeStartElement(Element.SECURITY_SETTINGS.getLocalName());
            for (SecuritySettingsElement securitySettingElement : securitySettings.values()) {
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
        MessagingSubsystemAdd element = new MessagingSubsystemAdd();
        if (bindingsDirectory != null) element.setBindingsDirectory(getBindingsDirectory());
        if (journalDirectory != null) element.setJournalDirectory(getJournalDirectory());
        if (largeMessagesDirectory != null) element.setLargeMessagesDirectory(getLargeMessagesDirectory());
        if (pagingDirectory != null) element.setPagingDirectory(getPagingDirectory());
        if (clustered != null) element.setClustered(isClustered());
        if (persistenceEnabled != null) element.setPersistenceEnabled(persistenceEnabled);
        if (journalMinFiles != -1) element.setJournalMinFiles(getJournalMinFiles());
        if (journalFileSize != -1) element.setJournalFileSize(getJournalFileSize());
        if (journalType != null) element.setJournalType(getJournalType());

        for (AbstractTransportElement<?> acceptorSpec : acceptors.values()) {
            element.addAcceptor(acceptorSpec);
        }
        for (AddressSettingsElement addressSpec : addressSettings.values()) {
            element.addAddressSettings(addressSpec);
        }
        for (AbstractTransportElement<?> connectorSpec : connectors.values()) {
            element.addConnector(connectorSpec);
        }
        for (SecuritySettingsElement securitySetting : securitySettings.values()) {
            element.addSecuritySettings(securitySetting);
        }
        for(QueueElement queue : queues.values()) {
            element.addQueue(queue);
        }
        return element;
    }

    @Override
    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler,
            final P param) {
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

    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    public void setPersistenceEnabled(boolean persistenceEnabled) {
        this.persistenceEnabled = persistenceEnabled;
    }

    boolean addAcceptor(AbstractTransportElement<?> acceptor) {
        if (acceptors.containsKey(acceptor.getName()))
            return false;
        acceptors.put(acceptor.getName(), acceptor);
        return true;
    }

    public Collection<AbstractTransportElement<?>> getAcceptors() {
        return Collections.unmodifiableCollection(new HashSet<AbstractTransportElement<?>>(acceptors.values()));
    }

    boolean removeAcceptor(final String name) {
        return acceptors.remove(name) != null;
    }

    boolean addConnector(AbstractTransportElement<?> connector) {
        if (connectors.containsKey(connector.getName()))
            return false;
        connectors.put(connector.getName(), connector);
        return true;
    }

    public Collection<AbstractTransportElement<?>> getConnectors() {
        return Collections.unmodifiableCollection(new HashSet<AbstractTransportElement<?>>(connectors.values()));
    }

    boolean removeConnector(final String name) {
        return connectors.remove(name) != null;
    }

    boolean addAddressSettings(AddressSettingsElement spec) {
        if (addressSettings.containsKey(spec.getMatch()))
            return false;
        addressSettings.put(spec.getMatch(), spec);
        return true;
    }

    public Collection<AddressSettingsElement> getAddressSettings() {
        return Collections.unmodifiableCollection(new HashSet<AddressSettingsElement>(addressSettings.values()));
    }

    boolean removeAddressSettings(final String match) {
        return addressSettings.remove(match) != null;
    }

    boolean addSecuritySetting(SecuritySettingsElement spec) {
        if (securitySettings.containsKey(spec.getMatch()))
            return false;
        securitySettings.put(spec.getMatch(), spec);
        return true;
    }

    public Collection<SecuritySettingsElement> getSecuritySettings() {
        return Collections.unmodifiableCollection(new HashSet<SecuritySettingsElement>(securitySettings.values()));
    }

    boolean removeSecuritySetting(final String match) {
        return securitySettings.remove(match) != null;
    }

    public QueueElement getQueue(final String name) {
        return queues.get(name);
    }

    QueueElement addQueue(final String name) {
        final QueueElement queue = new QueueElement(name);
        if(addQueue(queue)) return queue;
        return null;
    }

    boolean addQueue(final QueueElement queue) {
        if (queues.containsKey(queue.getName()))
            return false;
        queues.put(queue.getName(), queue);
        return true;
    }

    boolean removeQueue(final String name) {
        return queues.remove(name) != null;
    }
}
