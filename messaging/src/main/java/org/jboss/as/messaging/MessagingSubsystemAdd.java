/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.messaging;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.CoreQueueConfiguration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.security.Role;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.as.messaging.jms.JMSService;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.services.net.SocketBinding;
import org.jboss.as.services.path.RelativePathService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * General messaging subsystem update.
 *
 * @author Emanuel Muckenhuber
 */
public class MessagingSubsystemAdd extends AbstractSubsystemAdd<MessagingSubsystemElement> {

    private static final long serialVersionUID = -1306547303259739030L;
    private static final ServiceName PATH_BASE = MessagingSubsystemElement.JBOSS_MESSAGING.append("paths");
    private static final String PATH_RELATIVE_TO = "jboss.server.data.dir";

    private DirectoryElement bindingsDirectory;
    private DirectoryElement journalDirectory;
    private DirectoryElement largeMessagesDirectory;
    private DirectoryElement pagingDirectory;
    private Boolean clustered;
    private Boolean persistenceEnabled;
    private Integer journalMinFiles;
    private Integer journalFileSize;
    private JournalType journalType;

    private Set<AbstractTransportElement<?>> acceptors = new HashSet<AbstractTransportElement<?>>();
    private Set<AbstractTransportElement<?>> connectors = new HashSet<AbstractTransportElement<?>>();
    private Set<SecuritySettingsElement> securitySettings = new HashSet<SecuritySettingsElement>();
    private Set<AddressSettingsElement> addressSettings = new HashSet<AddressSettingsElement>();

    private Set<QueueElement> queues = new HashSet<QueueElement>();

    public MessagingSubsystemAdd() {
        super(Namespace.MESSAGING_1_0.getUriString());
    }

    @Override
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final HornetQService hqservice = new HornetQService();
        final Configuration hqConfig = new ConfigurationImpl();

        if (clustered != null) {
            hqConfig.setClustered(clustered);
        }
        if (journalMinFiles != null) {
            hqConfig.setJournalMinFiles(journalMinFiles);
        }
        if (journalFileSize != null) {
            hqConfig.setJournalFileSize(journalFileSize);
        }
        if (journalType != null) {
            hqConfig.setJournalType(journalType);
        }
        if(persistenceEnabled != null) {
            hqConfig.setPersistenceEnabled(persistenceEnabled);
        }

        // Configure address settings
        final Map<String, AddressSettings> configAddressSettings = hqConfig.getAddressesSettings();
        for(AddressSettingsElement addressSpec : addressSettings) {
            final AddressSettings settings = new AddressSettings();
            settings.setAddressFullMessagePolicy(addressSpec.getAddressFullMessagePolicy());
            settings.setDeadLetterAddress(addressSpec.getDeadLetterAddress());
            settings.setExpiryAddress(addressSpec.getExpiryAddress());
            if (addressSpec.isLastValueQueue() != null) {
                settings.setLastValueQueue(addressSpec.isLastValueQueue());
            }
            if (addressSpec.getMaxDeliveryAttempts() != null) {
                settings.setMaxDeliveryAttempts(addressSpec.getMaxDeliveryAttempts());
            }
            if (addressSpec.getMaxSizeBytes() != null) {
                settings.setMaxSizeBytes(addressSpec.getMaxSizeBytes());
            }
            if (addressSpec.getMessageCounterHistoryDayLimit() != null) {
                settings.setMessageCounterHistoryDayLimit(addressSpec.getMessageCounterHistoryDayLimit());
            }
            if (addressSpec.getPageSizeBytes() != null) {
                settings.setPageSizeBytes(addressSpec.getPageSizeBytes());
            }
            if (addressSpec.getRedeliveryDelay() != null) {
                settings.setRedeliveryDelay(addressSpec.getRedeliveryDelay());
            }
            if (addressSpec.getRedistributionDelay() != null) {
                settings.setRedistributionDelay(addressSpec.getRedistributionDelay());
            }
            if (addressSpec.isSendToDLAOnNoRoute() != null) {
                settings.setSendToDLAOnNoRoute(addressSpec.isSendToDLAOnNoRoute());
            }
            configAddressSettings.put(addressSpec.getMatch(), settings);
        }
        //  Configure security roles
        final Map<String, Set<Role>> hqSecurityRoles = hqConfig.getSecurityRoles();
        for(SecuritySettingsElement securitySetting : securitySettings) {
            hqSecurityRoles.put(securitySetting.getMatch(), securitySetting.getRoles());
        }

        // Configure queues
        for(final QueueElement queue : queues) {
            hqConfig.getQueueConfigurations().add(new CoreQueueConfiguration(queue.getAddress(), queue.getName(), queue.getFilter(), queue.isDurable()));
        }

        hqservice.setConfiguration(hqConfig);

        final BatchBuilder batchBuilder = updateContext.getBatchBuilder();
        final ServiceBuilder<HornetQServer> serviceBuilder = batchBuilder.addService(MessagingSubsystemElement.JBOSS_MESSAGING, hqservice)
                .addOptionalDependency(ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, hqservice.getMBeanServer());

        // FIXME move the JMSService into the jms subsystem
        JMSService.addService(batchBuilder);

        // Create path services
        createRelativePathService("bindings", bindingsDirectory, batchBuilder);
        addPathDependency("bindings", hqservice, serviceBuilder);
        createRelativePathService("journal", journalDirectory, batchBuilder);
        addPathDependency("journal", hqservice, serviceBuilder);
        createRelativePathService("largemessages", largeMessagesDirectory, batchBuilder);
        addPathDependency("largemessages", hqservice, serviceBuilder);
        createRelativePathService("paging", pagingDirectory, batchBuilder);
        addPathDependency("paging", hqservice, serviceBuilder);

        final Map<String, TransportConfiguration> connectors = hqConfig.getConnectorConfigurations();
        for(AbstractTransportElement<?> connectorSpec : this.connectors) {
            final TransportConfiguration transport = new TransportConfiguration(connectorSpec.getFactoryClassName(), new HashMap<String, Object>(), connectorSpec.getName());
            // Process parameters
            connectorSpec.processHQConfig(transport);
            connectors.put(connectorSpec.getName(), transport);
            // Add a dependency on a SocketBinding if there is a socket-ref
            final String socketRef = connectorSpec.getSocketBindingRef();
            if (socketRef != null) {
                final ServiceName socketName = SocketBinding.JBOSS_BINDING_NAME.append(socketRef);
                serviceBuilder.addDependency(socketName, SocketBinding.class, hqservice.getSocketBindingInjector(socketRef));
            }
        }

        final Collection<TransportConfiguration> acceptors = hqConfig.getAcceptorConfigurations();
        for(AbstractTransportElement<?> acceptorSpec : this.acceptors) {
            final TransportConfiguration transport = new TransportConfiguration(acceptorSpec.getFactoryClassName(), new HashMap<String, Object>(), acceptorSpec.getName());
            // Process parameters
            acceptorSpec.processHQConfig(transport);
            acceptors.add(transport);
            // Add a dependency on a SocketBinding if there is a socket-ref
            final String socketRef = acceptorSpec.getSocketBindingRef();
            if (socketRef != null) {
                final ServiceName socketName = SocketBinding.JBOSS_BINDING_NAME.append(socketRef);
                serviceBuilder.addDependency(socketName, SocketBinding.class, hqservice.getSocketBindingInjector(socketRef));
            }
        }
        serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
    }

    @Override
    protected MessagingSubsystemElement createSubsystemElement() {
        final MessagingSubsystemElement element = new MessagingSubsystemElement();
        if (bindingsDirectory != null) element.setBindingsDirectory(getBindingsDirectory());
        if (journalDirectory != null) element.setJournalDirectory(getJournalDirectory());
        if (largeMessagesDirectory != null) element.setLargeMessagesDirectory(getLargeMessagesDirectory());
        if (pagingDirectory != null) element.setPagingDirectory(getPagingDirectory());
        if (clustered != null) element.setClustered(isClustered());
        if (persistenceEnabled != null) element.setPersistenceEnabled(persistenceEnabled);
        if (journalMinFiles != null) element.setJournalMinFiles(getJournalMinFiles());
        if (journalFileSize != null) element.setJournalFileSize(getJournalFileSize());
        if (journalType != null) element.setJournalType(getJournalType());

        for (AbstractTransportElement<?> acceptorSpec : acceptors) {
            element.addAcceptor(acceptorSpec);
        }
        for (AddressSettingsElement addressSpec : addressSettings) {
            element.addAddressSettings(addressSpec);
        }
        for (AbstractTransportElement<?> connectorSpec : connectors) {
            element.addConnector(connectorSpec);
        }
        for (SecuritySettingsElement securitySetting : securitySettings) {
            element.addSecuritySetting(securitySetting);
        }
        for(QueueElement queue : queues) {
            element.addQueue(queue);
        }
        return element;
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

    public Boolean isClustered() {
        return clustered;
    }

    public void setClustered(boolean clustered) {
        this.clustered = clustered;
    }

    public Integer getJournalMinFiles() {
        return journalMinFiles;
    }

    public void setJournalMinFiles(int journalMinFiles) {
        this.journalMinFiles = journalMinFiles;
    }

    public Integer getJournalFileSize() {
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

    public void setPersistenceEnabled(Boolean enabled) {
        persistenceEnabled = enabled;
    }

    void addAcceptor(final AbstractTransportElement<?> transportSpecification) {
        acceptors.add(transportSpecification);
    }

    void addConnector(final AbstractTransportElement<?> transportSpecification) {
        connectors.add(transportSpecification);
    }

    void addAddressSettings(final AddressSettingsElement addressSettingsSpecification) {
        addressSettings.add(addressSettingsSpecification);
    }

    void addSecuritySettings(final SecuritySettingsElement securitySettingsSpecification) {
        securitySettings.add(securitySettingsSpecification);
    }

    void addQueue(final QueueElement queue) {
        queues.add(queue);
    }

    static void addPathDependency(String name, HornetQService hqService, ServiceBuilder<?> serviceBuilder) {
        serviceBuilder.addDependency(PATH_BASE.append(name), String.class, hqService.getPathInjector(name));
    }

    static void createRelativePathService(String name, DirectoryElement dir, BatchBuilder builder) {
        if(dir != null) {
            createRelativePathService(name, dir.getRelativeTo(), dir.getPath(), builder);
        } else {
            createRelativePathService(name, PATH_RELATIVE_TO, "hornetq/"+name, builder);
        }
    }

    static void createRelativePathService(String name, String relativeTo, String relativePath, BatchBuilder builder) {
        RelativePathService.addService(PATH_BASE.append(name),
                // default to hornetq/name
                relativePath != null ? relativePath : "hornetq/" + name,
                // default to data dir
                relativeTo != null ? relativeTo : PATH_RELATIVE_TO, builder);
    }
}
