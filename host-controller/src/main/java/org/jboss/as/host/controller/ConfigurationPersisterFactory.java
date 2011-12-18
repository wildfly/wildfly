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
import java.util.concurrent.ExecutorService;

import javax.xml.namespace.QName;

import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.host.controller.parsing.DomainXml;
import org.jboss.as.host.controller.parsing.HostXml;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.persistence.BackupXmlConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationFile;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.persistence.XmlConfigurationPersister;
import org.jboss.modules.Module;

/**
 * Factory methods to produce an {@link ExtensibleConfigurationPersister} for different Host Controller use cases.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ConfigurationPersisterFactory {

    public static ExtensibleConfigurationPersister createHostXmlConfigurationPersister(final ConfigurationFile file, ExecutorService executorService) {
        HostXml hostXml = new HostXml(Module.getBootModuleLoader(), executorService);
        BackupXmlConfigurationPersister persister =  new BackupXmlConfigurationPersister(file, new QName(Namespace.CURRENT.getUriString(), "host"), hostXml, hostXml);
        persister.registerAdditionalRootElement(new QName(Namespace.DOMAIN_1_0.getUriString(), "host"), hostXml);
        return persister;
    }

    public static ExtensibleConfigurationPersister createDomainXmlConfigurationPersister(final ConfigurationFile file, ExecutorService executorService, ExtensionRegistry extensionRegistry) {
        DomainXml domainXml = new DomainXml(Module.getBootModuleLoader(), executorService, extensionRegistry);
        BackupXmlConfigurationPersister persister = new BackupXmlConfigurationPersister(file, new QName(Namespace.CURRENT.getUriString(), "domain"), domainXml, domainXml);
        persister.registerAdditionalRootElement(new QName(Namespace.DOMAIN_1_0.getUriString(), "domain"), domainXml);
        extensionRegistry.setWriterRegistry(persister);
        return persister;
    }

    public static ExtensibleConfigurationPersister createCachedRemoteDomainXmlConfigurationPersister(final File configDir, ExecutorService executorService, ExtensionRegistry extensionRegistry) {
        DomainXml domainXml = new DomainXml(Module.getBootModuleLoader(), executorService, extensionRegistry);
        File file = new File(configDir, "domain.cached-remote.xml");
        ExtensibleConfigurationPersister persister = new XmlConfigurationPersister(file, new QName(Namespace.CURRENT.getUriString(), "domain"), domainXml, domainXml);
        extensionRegistry.setWriterRegistry(persister);
        return persister;
    }

    public static ExtensibleConfigurationPersister createTransientDomainXmlConfigurationPersister(ExecutorService executorService, ExtensionRegistry extensionRegistry) {
        DomainXml domainXml = new DomainXml(Module.getBootModuleLoader(), executorService, extensionRegistry);
        ExtensibleConfigurationPersister persister =  new NullConfigurationPersister(domainXml);
        extensionRegistry.setWriterRegistry(persister);
        return persister;
    }
}
