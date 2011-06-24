/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain;

import org.jboss.as.arquillian.container.domain.managed.DomainLifecycleUtil;
import org.jboss.as.arquillian.container.domain.managed.JBossAsManagedConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;

/**
 * Test validating the configuration
 *
 * @author Emanuel Muckenhuber
 */
public class BuildConfigurationTestCase {

    static final String masterAddress = System.getProperty("jboss.test.host.master.address", "127.0.0.1");
    static final File BUILD_DIR = new File("../../build/src/main/resources/domain/configuration/");

    @Test
    public void test() throws Exception {
        final JBossAsManagedConfiguration config = createConfiguration("domain.xml", "host.xml", "test");
        final DomainLifecycleUtil utils = new DomainLifecycleUtil(config);
        utils.start(); // Start
        try {
            URLConnection connection = new URL("http://localhost:8080").openConnection();
            connection.connect();
        } finally {
            utils.stop(); // Stop
        }
    }


    static JBossAsManagedConfiguration createConfiguration(final String domainXmlName, final String hostXmlName, final String testConfiguration) {
        final File output = new File("target" + File.separator + "domains" + File.separator + testConfiguration);
        final JBossAsManagedConfiguration configuration = new JBossAsManagedConfiguration();

        configuration.setHostControllerManagementAddress(masterAddress);
        configuration.setHostCommandLineProperties("-Djboss.test.host.master.address=" + masterAddress);
        configuration.setDomainConfigFile(new File(BUILD_DIR, domainXmlName).getAbsolutePath());
        configuration.setHostConfigFile(new File(BUILD_DIR, hostXmlName).getAbsolutePath());

        configuration.setHostName("local"); // TODO this shouldn't be needed

        new File(output, "configuration").mkdirs(); // TODO this should not be necessary
        configuration.setDomainDirectory(output.getAbsolutePath());

        System.out.println(configuration.getDomainConfigFile());
        System.out.println(configuration.getHostConfigFile());
        return configuration;

    }

}
