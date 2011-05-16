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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Utilities for running tests of domain mode.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainTestUtil {

    public static final String masterAddress = System.getProperty("jboss.test.host.master.address", "127.0.0.1");
    public static final String slaveAddress = System.getProperty("jboss.test.host.slave.address", "127.0.0.1");
    public static final long domainBootTimeout = Long.valueOf(System.getProperty("jboss.test.domain.boot.timeout", "60000"));
    public static final long domainShutdownTimeout = Long.valueOf(System.getProperty("jboss.test.domain.shutdown.timeout", "20000"));

    public static JBossAsManagedConfiguration getMasterConfiguration(String domainConfigPath, String hostConfigPath, String testName) throws URISyntaxException {

        File domains = new File("target" + File.separator + "domains" + File.separator + testName);
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        final JBossAsManagedConfiguration masterConfig = new JBossAsManagedConfiguration();
        masterConfig.setHostControllerManagementAddress(masterAddress);
        masterConfig.setHostCommandLineProperties("-Djboss.test.host.master.address=" + masterAddress);
        URL url = tccl.getResource(domainConfigPath);
        masterConfig.setDomainConfigFile(new File(url.toURI()).getAbsolutePath());
        System.out.println(masterConfig.getDomainConfigFile());
        url = tccl.getResource(hostConfigPath);
        System.out.println(masterConfig.getHostConfigFile());
        masterConfig.setHostConfigFile(new File(url.toURI()).getAbsolutePath());
        File masterDir = new File(domains, "master");
        // TODO this should not be necessary
        new File(masterDir, "configuration").mkdirs();
        masterConfig.setDomainDirectory(masterDir.getAbsolutePath());

        return masterConfig;
    }

    public static JBossAsManagedConfiguration getSlaveConfiguration(String hostConfigPath, String testName) throws URISyntaxException {

        File domains = new File("target" + File.separator + "domains" + File.separator + testName);
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        final JBossAsManagedConfiguration slaveConfig = new JBossAsManagedConfiguration();
        slaveConfig.setHostName("slave");
        slaveConfig.setHostControllerManagementAddress(slaveAddress);
        slaveConfig.setHostControllerManagementPort(19999);
        slaveConfig.setHostCommandLineProperties("-Djboss.test.host.master.address=" + masterAddress +
                " -Djboss.test.host.slave.address=" + slaveAddress);
        URL url = tccl.getResource(hostConfigPath);
        slaveConfig.setHostConfigFile(new File(url.toURI()).getAbsolutePath());
        System.out.println(slaveConfig.getHostConfigFile());
        File slaveDir = new File(domains, "slave");
        // TODO this should not be necessary
        new File(slaveDir, "configuration").mkdirs();
        slaveConfig.setDomainDirectory(slaveDir.getAbsolutePath());
        System.out.println(slaveConfig.getDomainDirectory());

        return slaveConfig;
    }

    public static void startHosts(long timeout, DomainLifecycleUtil... hosts) throws Exception {
        Future<?>[] futures = new Future<?>[hosts.length];
        for (int i = 0; i < hosts.length; i++) {
            futures[i] = hosts[i].startAsync();
        }

        processFutures(futures, timeout);
    }

    public static void stopHosts(long timeout, DomainLifecycleUtil... hosts) throws Exception {
        Future<?>[] futures = new Future<?>[hosts.length];
        for (int i = 0; i < hosts.length; i++) {
            futures[i] = hosts[i].stopAsync();
        }

        processFutures(futures, timeout);
    }

    private static void processFutures(Future<?>[] futures, long timeout) throws Exception {

        try {
            for (int i = 0; i < futures.length; i++) {
                try {
                    futures[i].get(timeout, TimeUnit.MILLISECONDS);
                }  catch (ExecutionException e){
                    throw e.getCause();
                }
            }
        } catch (Exception e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /** Prevent instantiation */
    private DomainTestUtil() {}
}
