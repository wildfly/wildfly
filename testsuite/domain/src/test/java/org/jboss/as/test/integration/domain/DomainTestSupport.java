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

import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.JBossAsManagedConfiguration;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Assert;

import java.io.Closeable;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

/**
 * Utilities for running tests of domain mode.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainTestSupport {

    private static final Logger log = Logger.getLogger("org.jboss.as.test.integration.domain");

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

    public static ModelNode createOperationNode(String address, String operation) {
        ModelNode op = new ModelNode();

        // set address
        ModelNode list = op.get("address").setEmptyList();
        if (address != null) {
            String [] pathSegments = address.split("/");
            for (String segment : pathSegments) {
                String[] elements = segment.split("=");
                list.add(elements[0], elements[1]);
            }
        }
        op.get("operation").set(operation);
        return op;
    }

    public static ModelNode validateResponse(ModelNode response) {

        if(! SUCCESS.equals(response.get(OUTCOME).asString())) {
            System.out.println("Failed response:");
            System.out.println(response);
            Assert.fail(response.get(FAILURE_DESCRIPTION).toString());
        }

        Assert.assertTrue("result exists", response.has(RESULT));
        return response.get(RESULT);
    }

    public static ModelNode validateFailedResponse(ModelNode response) {

        if(! FAILED.equals(response.get(OUTCOME).asString())) {
            System.out.println("Response succeeded:");
            System.out.println(response);
            Assert.fail(response.get(OUTCOME).toString());
        }

        Assert.assertTrue("failure description exists", response.has(FAILURE_DESCRIPTION));
        return response.get(FAILURE_DESCRIPTION);
    }

    public static void cleanFile(File file) {
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                for (File child : file.listFiles()) {
                    cleanFile(child);
                }
            }
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    public static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable t) {
            log.errorf(t, "Failed to close resource %s", closeable);
        }
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

    private final String domainConfig;
    private final String masterConfig;
    private final String slaveConfig;
    private final DomainLifecycleUtil domainMasterLifecycleUtil;
    private final DomainLifecycleUtil domainSlaveLifecycleUtil;


    public DomainTestSupport(final String testClass, final Configuration configuration) throws Exception {
        this(testClass, configuration.getDomainConfig(), configuration.getMasterConfig(), configuration.getSlaveConfigs());
    }

    public DomainTestSupport(final String testClass, final String domainConfig, final String masterConfig, final String slaveConfig) throws Exception {
        this.domainConfig = domainConfig;
        this.masterConfig = masterConfig;
        this.slaveConfig = slaveConfig;

        final JBossAsManagedConfiguration master = getMasterConfiguration(domainConfig, masterConfig, testClass);
        domainMasterLifecycleUtil = new DomainLifecycleUtil(master);

        if (slaveConfig != null) {
            final JBossAsManagedConfiguration slave = getSlaveConfiguration(slaveConfig, testClass);
            domainSlaveLifecycleUtil = new DomainLifecycleUtil(slave);
        } else {
            domainSlaveLifecycleUtil = null;
        }
    }

    public DomainLifecycleUtil getDomainMasterLifecycleUtil() {
        return domainMasterLifecycleUtil;
    }

    public DomainLifecycleUtil getDomainSlaveLifecycleUtil() {
        return domainSlaveLifecycleUtil;
    }

    public void start() {
        domainMasterLifecycleUtil.start();
        if (domainSlaveLifecycleUtil != null) {
            domainSlaveLifecycleUtil.start();
        }
        try {
            //Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (domainSlaveLifecycleUtil != null) {
                domainSlaveLifecycleUtil.stop();
            }

        }   finally {
            domainMasterLifecycleUtil.stop();
        }
    }

    public static class Configuration {

        private String domainConfig;
        private String masterConfig;
        private String slaveConfig;

        protected Configuration(final String domainConfig, final String masterConfig, final String slaveConfig) {
            this.domainConfig = domainConfig;
            this.masterConfig = masterConfig;
            this.slaveConfig = slaveConfig;
        }

        public String getDomainConfig() {
            return domainConfig;
        }

        public String getMasterConfig() {
            return masterConfig;
        }

        public String getSlaveConfigs() {
            return slaveConfig;
        }

        public static Configuration create(final String domainConfig, final String masterConfig, final String slaveConfig) {
            return new Configuration(domainConfig, masterConfig, slaveConfig);
        }

    }


}
