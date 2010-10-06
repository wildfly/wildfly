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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.security.Role;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.as.messaging.hornetq.HornetQService;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.services.net.SocketBinding;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * General messaging subsystem update.
 *
 * @author Emanuel Muckenhuber
 */
public class MessagingSubsystemAdd extends AbstractSubsystemAdd<MessagingSubsystemElement> {

    private static final long serialVersionUID = -1306547303259739030L;

    private String bindingsDirectory;
    private String journalDirectory;
    private String largeMessagesDirectory;
    private String pagingDirectory;
    private boolean clustered;
    private int journalMinFiles = -1;
    private int journalFileSize = -1;
    private JournalType journalType;

    private Set<TransportSpecification> acceptors = new HashSet<TransportSpecification>();
    private Set<TransportSpecification> connectors = new HashSet<TransportSpecification>();
    private Set<SecuritySettingsSpecification> securitySettings = new HashSet<SecuritySettingsSpecification>();
    private Set<AddressSettingsSpecification> addressSettings = new HashSet<AddressSettingsSpecification>();

    public MessagingSubsystemAdd() {
        super(Namespace.MESSAGING_1_0.getUriString());
    }

    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final HornetQService hqservice = new HornetQService();
        final Configuration hqConfig = new ConfigurationImpl();

        hqConfig.setBindingsDirectory(getBindingsDirectory());
        hqConfig.setJournalDirectory(getJournalDirectory());
        hqConfig.setLargeMessagesDirectory(getLargeMessagesDirectory());
        hqConfig.setPagingDirectory(getPagingDirectory());
        hqConfig.setClustered(isClustered());
        hqConfig.setJournalMinFiles(getJournalMinFiles());
        hqConfig.setJournalFileSize(getJournalFileSize());
        hqConfig.setJournalType(getJournalType());

        // Configure address settings
        final Map<String, AddressSettings> configAddressSettings = hqConfig.getAddressesSettings();
        for(AddressSettingsSpecification addressSpec : addressSettings) {
            final AddressSettings settings = new AddressSettings();
            settings.setDeadLetterAddress(addressSpec.getDeadLetterAddress());
            settings.setExpiryAddress(addressSpec.getExpiryAddress());
            settings.setRedeliveryDelay(addressSpec.getRedeliveryDelay());
            settings.setRedeliveryDelay(addressSpec.getRedeliveryDelay());
            settings.setMessageCounterHistoryDayLimit(addressSpec.getMessageCounterHistoryDayLimit());
            settings.setAddressFullMessagePolicy(addressSpec.getAddressFullMessagePolicy());
            settings.setLastValueQueue(addressSpec.isLastValueQueue());
            settings.setMaxDeliveryAttempts(addressSpec.getMaxDeliveryAttempts());
            settings.setRedistributionDelay(addressSpec.getRedistributionDelay());
            settings.setSendToDLAOnNoRoute(addressSpec.isSendToDLAOnNoRoute());
            settings.setMaxSizeBytes(addressSpec.getMaxSizeBytes());
            settings.setPageSizeBytes(addressSpec.getPageSizeBytes());
            configAddressSettings.put(addressSpec.getMatch(), settings);
        }
        //  Configure security roles
        final Map<String, Set<Role>> hqSecurityRoles = hqConfig.getSecurityRoles();
        for(SecuritySettingsSpecification securitySetting : securitySettings) {
            hqSecurityRoles.put(securitySetting.getMatch(), securitySetting.getRoles());
        }

        hqservice.setConfiguration(hqConfig);

        final BatchBuilder batchBuilder = updateContext.getBatchBuilder();
        final BatchServiceBuilder<HornetQServer> serviceBuilder = batchBuilder.addService(MessagingSubsystemElement.JBOSS_MESSAGING, hqservice);

        final Collection<TransportConfiguration> connectors = hqConfig.getConnectorConfigurations().values();
        for(TransportSpecification connectorSpec : this.connectors) {
            connectors.add(new TransportConfiguration(connectorSpec.getFactoryClassName(), connectorSpec.getParams(), connectorSpec.getName()));

            final Object socketRef = connectorSpec.getParams().get("socket-ref");
            // Add a dependency on a SocketBinding if there is a socket-ref
            if (socketRef != null) {
                final String name = socketRef.toString();
                final ServiceName socketName = SocketBinding.JBOSS_BINDING_NAME.append(name);
                serviceBuilder.addDependency(socketName, SocketBinding.class, hqservice.getSocketBindingInjector(name));
            }
        }

        final Collection<TransportConfiguration> acceptors = hqConfig.getAcceptorConfigurations();
        for(TransportSpecification acceptorSpec : this.acceptors) {
            acceptors.add(new TransportConfiguration(acceptorSpec.getFactoryClassName(), acceptorSpec.getParams(), acceptorSpec.getName()));
            final Object socketRef = acceptorSpec.getParams().get("socket-ref");
            // Add a dependency on a SocketBinding if there is a socket-ref
            if (socketRef != null) {
                final String name = socketRef.toString();
                final ServiceName socketName = SocketBinding.JBOSS_BINDING_NAME.append(name);
                serviceBuilder.addDependency(socketName, SocketBinding.class, hqservice.getSocketBindingInjector(name));
            }
        }
        serviceBuilder.setInitialMode(ServiceController.Mode.IMMEDIATE);
    }

    protected MessagingSubsystemElement createSubsystemElement() {
        final MessagingSubsystemElement element = new MessagingSubsystemElement();
        if (bindingsDirectory != null) element.setBindingsDirectory(getBindingsDirectory());
        if (journalDirectory != null) element.setJournalDirectory(getJournalDirectory());
        if (largeMessagesDirectory != null) element.setLargeMessagesDirectory(getLargeMessagesDirectory());
        if (pagingDirectory != null) element.setPagingDirectory(getPagingDirectory());
        element.setClustered(isClustered());
        if (journalMinFiles != -1) element.setJournalMinFiles(getJournalMinFiles());
        if (journalFileSize != -1) element.setJournalFileSize(getJournalFileSize());
        if (journalType != null) element.setJournalType(getJournalType());
        return element;
    }

    public String getBindingsDirectory() {
        return bindingsDirectory;
    }

    public void setBindingsDirectory(String bindingsDirectory) {
        this.bindingsDirectory = bindingsDirectory;
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

    void addAcceptor(final TransportSpecification transportSpecification) {
        acceptors.add(transportSpecification);
    }

    void addConnector(final TransportSpecification transportSpecification) {
        connectors.add(transportSpecification);
    }

    void addAddressSettings(final AddressSettingsSpecification addressSettingsSpecification) {
        addressSettings.add(addressSettingsSpecification);
    }

    void addSecuritySettings(final SecuritySettingsSpecification securitySettingsSpecification) {
        securitySettings.add(securitySettingsSpecification);
    }
}
