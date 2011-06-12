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

import javax.xml.namespace.QName;

import org.jboss.as.controller.parsing.DomainXml;
import org.jboss.as.controller.parsing.HostXml;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.persistence.BackupXmlConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationFile;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.persistence.XmlConfigurationPersister;
import org.jboss.modules.Module;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ConfigurationPersisterFactory {

    public static ExtensibleConfigurationPersister createHostXmlConfigurationPersister(final File configDir, final ConfigurationFile file) {
        HostXml hostXml = new HostXml(Module.getBootModuleLoader());
        return new BackupXmlConfigurationPersister(file, new QName(Namespace.CURRENT.getUriString(), "host"), hostXml, hostXml);
    }

    public static ExtensibleConfigurationPersister createDomainXmlConfigurationPersister(final File configDir, final ConfigurationFile file) {
        DomainXml domainXml = new DomainXml(Module.getBootModuleLoader());
        return new BackupXmlConfigurationPersister(file, new QName(Namespace.CURRENT.getUriString(), "domain"), domainXml, domainXml);
    }

    public static ExtensibleConfigurationPersister createCachedRemoteDomainXmlConfigurationPersister(final File configDir) {
        DomainXml domainXml = new DomainXml(Module.getBootModuleLoader());
        File file = new File(configDir, "domain.cached-remote.xml");
        return new XmlConfigurationPersister(file, new QName(Namespace.CURRENT.getUriString(), "domain"), domainXml, domainXml);
    }

    public static ExtensibleConfigurationPersister createTransientDomainXmlConfigurationPersister() {
        DomainXml domainXml = new DomainXml(Module.getBootModuleLoader());
        return new NullConfigurationPersister(domainXml);
    }
}
