/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.host.controller;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.xml.namespace.QName;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.as.host.controller.parsing.DomainXml;
import org.jboss.as.host.controller.parsing.HostXml;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.persistence.BackupXmlConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationFile;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.persistence.XmlConfigurationPersister;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;

/**
 * Factory methods to produce an {@link ExtensibleConfigurationPersister} for different Host Controller use cases.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ConfigurationPersisterFactory {

    static final String CACHED_DOMAIN_XML = "domain.cached-remote.xml";

    // host.xml
    public static ExtensibleConfigurationPersister createHostXmlConfigurationPersister(final ConfigurationFile file, String defaultHostControllerName) {
        HostXml hostXml = new HostXml(defaultHostControllerName);
        BackupXmlConfigurationPersister persister =  new BackupXmlConfigurationPersister(file, new QName(Namespace.CURRENT.getUriString(), "host"), hostXml, hostXml);
        for (Namespace namespace : Namespace.values()) {
            if (!namespace.equals(Namespace.CURRENT)) {
                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "host"), hostXml);
            }
        }
        return persister;
    }

    // domain.xml
    public static ExtensibleConfigurationPersister createDomainXmlConfigurationPersister(final ConfigurationFile file, ExecutorService executorService, ExtensionRegistry extensionRegistry) {
        DomainXml domainXml = new DomainXml(Module.getBootModuleLoader(), executorService, extensionRegistry);
        BackupXmlConfigurationPersister persister = new BackupXmlConfigurationPersister(file, new QName(Namespace.CURRENT.getUriString(), "domain"), domainXml, domainXml);
        for (Namespace namespace : Namespace.values()) {
            if (!namespace.equals(Namespace.CURRENT)) {
                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "domain"), domainXml);
            }
        }
        extensionRegistry.setWriterRegistry(persister);
        return persister;
    }

    // --backup
    public static ExtensibleConfigurationPersister createRemoteBackupDomainXmlConfigurationPersister(final File configDir, ExecutorService executorService, ExtensionRegistry extensionRegistry) {
        DomainXml domainXml = new DomainXml(Module.getBootModuleLoader(), executorService, extensionRegistry);
        File file = new File(configDir, CACHED_DOMAIN_XML);
        BackupRemoteDomainXmlPersister persister = new BackupRemoteDomainXmlPersister(file, new QName(Namespace.CURRENT.getUriString(), "domain"), domainXml, domainXml);
        for (Namespace namespace : Namespace.values()) {
            if (!namespace.equals(Namespace.CURRENT)) {
                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "domain"), domainXml);
            }
        }
        extensionRegistry.setWriterRegistry(persister);
        return persister;
    }

    // --cached-dc
    public static ExtensibleConfigurationPersister createCachedRemoteDomainXmlConfigurationPersister(final File configDir, ExecutorService executorService, ExtensionRegistry extensionRegistry) {
        DomainXml domainXml = new DomainXml(Module.getBootModuleLoader(), executorService, extensionRegistry);
        File file = new File(configDir, CACHED_DOMAIN_XML);
        CachedRemoteDomainXmlPersister persister = new CachedRemoteDomainXmlPersister(file, new QName(Namespace.CURRENT.getUriString(), "domain"), domainXml, domainXml);
        for (Namespace namespace : Namespace.values()) {
            if (!namespace.equals(Namespace.CURRENT)) {
                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "domain"), domainXml);
            }
        }
        extensionRegistry.setWriterRegistry(persister);
        return persister;
    }

    // slave=true
    public static ExtensibleConfigurationPersister createTransientDomainXmlConfigurationPersister(ExecutorService executorService, ExtensionRegistry extensionRegistry) {
        DomainXml domainXml = new DomainXml(Module.getBootModuleLoader(), executorService, extensionRegistry);
        ExtensibleConfigurationPersister persister =  new NullConfigurationPersister(domainXml);
        extensionRegistry.setWriterRegistry(persister);
        return persister;
    }

    /**
     * --backup
     */
    static class BackupRemoteDomainXmlPersister extends XmlConfigurationPersister {

        BackupRemoteDomainXmlPersister(File fileName, QName rootElement, XMLElementReader<List<ModelNode>> rootParser, XMLElementWriter<ModelMarshallingContext> rootDeparser) {
            super(fileName, rootElement, rootParser, rootDeparser);
        }

        @Override
        public List<ModelNode> load() throws ConfigurationPersistenceException {
            // TODO throw an exception
            HostControllerLogger.ROOT_LOGGER.invalidRemoteBackupPersisterState();
            return Collections.emptyList();
        }

    }

    /**
     * --cached-dc
     */
    static class CachedRemoteDomainXmlPersister extends XmlConfigurationPersister {

        volatile boolean started = false;
        CachedRemoteDomainXmlPersister(File fileName, QName rootElement, XMLElementReader<List<ModelNode>> rootParser, XMLElementWriter<ModelMarshallingContext> rootDeparser) {
            super(fileName, rootElement, rootParser, rootDeparser);
        }

        @Override
        public PersistenceResource store(ModelNode model, Set<PathAddress> affectedAddresses) throws ConfigurationPersistenceException {
            if(started) {
                // TODO throw an exception
                HostControllerLogger.ROOT_LOGGER.invalidRemoteBackupPersisterState();
            }
            return NULL_PERSISTENCE_RESOURCE;
        }

        @Override
        public void successfulBoot() throws ConfigurationPersistenceException {
            started = true;
        }
    }

    static ConfigurationPersister.PersistenceResource NULL_PERSISTENCE_RESOURCE = new ConfigurationPersister.PersistenceResource() {

        @Override
        public void commit() {
            //
        }

        @Override
        public void rollback() {
            //
        }
    };

}
