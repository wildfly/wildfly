/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.domain.mixed.util;

import java.io.File;

import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.JBossAsManagedConfigurationParameters;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MixedDomainTestSupport extends DomainTestSupport {

    public MixedDomainTestSupport(String testClass, String domainConfig, String masterConfig, String slaveConfig, JBossAsManagedConfigurationParameters slaveParams)
            throws Exception {
        super(testClass, domainConfig, masterConfig, slaveConfig, JBossAsManagedConfigurationParameters.STANDARD, slaveParams);
    }


    public static MixedDomainTestSupport create(String testClass, String version) throws Exception {
        OldVersionCopier oldVersionCopier = OldVersionCopier.expandOldVersions();
        File dir = oldVersionCopier.getVersionDir(version);

        JBossAsManagedConfigurationParameters slaveParams = new OldSlaveJBossAsManagedConfigurationParameters(dir.getAbsolutePath());
        MixedDomainTestSupport support = new MixedDomainTestSupport(testClass, "master-config/domain.xml", "master-config/host.xml", "slave-configs/" + dir.getName() + "/domain/configuration/host-slave.xml", slaveParams);
//
//        //Override the jboss home for the slave
//        JBossAsManagedConfiguration slaveConfig = support.getDomainSlaveConfiguration();
//        slaveConfig.setJbossHome(dir.getAbsolutePath());

        return support;
    }
}
