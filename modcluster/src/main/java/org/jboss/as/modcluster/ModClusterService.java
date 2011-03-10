/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.modcluster;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.JMException;
import javax.management.MBeanServer;

import org.apache.tomcat.util.modeler.Registry;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.modcluster.catalina.CatalinaEventHandlerAdapter;
import org.jboss.modcluster.catalina.ModClusterListener;
import org.jboss.modcluster.config.ModClusterConfig;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProvider;
import org.jboss.modcluster.load.impl.SimpleLoadBalanceFactorProvider;
import org.jboss.modcluster.load.metric.LoadContext;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.impl.ActiveSessionsLoadMetric;
import org.jboss.modcluster.load.metric.impl.BusyConnectorsLoadMetric;
import org.jboss.modcluster.load.metric.impl.ReceiveTrafficLoadMetric;
import org.jboss.modcluster.load.metric.impl.RequestCountLoadMetric;
import org.jboss.modcluster.load.metric.impl.SendTrafficLoadMetric;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service configuring and starting the web container.
 *
 * @author Emanuel Muckenhuber
 */
class ModClusterService implements Service<Void> {

    private static final Logger log = Logger.getLogger("org.jboss.as.modcluster");

    static final ServiceName NAME = ServiceName.JBOSS.append("mod-cluster");

    private ModelNode modelconf;

    private ModClusterListener listener;
    private CatalinaEventHandlerAdapter adapter;
    private LoadBalanceFactorProvider load;

    /* Depending on configuration we use one of the other */
    private org.jboss.modcluster.ModClusterService service;
    private ModClusterConfig config;
    // TODO HA brings too many dependencies :-(
    // private HAModClusterService haservice;
    // private HAModClusterConfig haconfig;

    private Class<? extends LoadMetric> loadMetricClass = BusyConnectorsLoadMetric.class;

    public ModClusterService(ModelNode modelconf) {
        this.modelconf = modelconf;
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {
        log.debugf("Starting Mod_cluster Extension");
        final MBeanServer mbeanServer = getMBeanServer();

        config = new ModClusterConfig();
        // Set the configuration.
        final ModelNode proxyconf = modelconf.get(CommonAttributes.PROXY_CONF);
        final ModelNode httpdconf = proxyconf.get(CommonAttributes.HTTPD_CONF);
        final ModelNode nodeconf = proxyconf.get(CommonAttributes.NODES_CONF);
        if (httpdconf.has(CommonAttributes.ADVERTISE_SOCKET)) {
            // TODO: That should be a socket-binding....
            config.setAdvertisePort(23364);
            config.setAdvertiseGroupAddress("224.0.1.105");
        }
        if (httpdconf.has(CommonAttributes.SSL)) {
            // TODO: Add SSL logic.
        }
        if (httpdconf.has(CommonAttributes.ADVERTISE))
            config.setAdvertise(httpdconf.get(CommonAttributes.ADVERTISE).asBoolean());
        if (httpdconf.has(CommonAttributes.PROXY_LIST)) {
            config.setProxyList(httpdconf.get(CommonAttributes.PROXY_LIST).asString());
        }
        if (httpdconf.has(CommonAttributes.PROXY_URL))
            config.setProxyList(httpdconf.get(CommonAttributes.PROXY_URL).asString());
        if (httpdconf.has(CommonAttributes.ADVERTISE_SECURITY_KEY))
            config.setProxyList(httpdconf.get(CommonAttributes.ADVERTISE_SECURITY_KEY).asString());

        if (nodeconf.has(CommonAttributes.EXCLUDED_CONTEXTS))
            config.setExcludedContexts(nodeconf.get(CommonAttributes.EXCLUDED_CONTEXTS).asString());
        if (nodeconf.has(CommonAttributes.AUTO_ENABLE_CONTEXTS))
            config.setAutoEnableContexts(nodeconf.get(CommonAttributes.AUTO_ENABLE_CONTEXTS).asBoolean());
        if (nodeconf.has(CommonAttributes.STOP_CONTEXT_TIMEOUT)) {
            config.setStopContextTimeout(nodeconf.get(CommonAttributes.SOCKET_TIMEOUT).asInt());
            config.setStopContextTimeoutUnit(TimeUnit.SECONDS);
        }
        if (nodeconf.has(CommonAttributes.SOCKET_TIMEOUT))
            config.setSocketTimeout(nodeconf.get(CommonAttributes.SOCKET_TIMEOUT).asInt());

        // Read the metrics configuration.
        final ModelNode loadmetric = modelconf.get(CommonAttributes.LOAD_METRIC);

        if (loadmetric.has(CommonAttributes.LOAD_METRIC_SIMPLE)) {
            // TODO it seems we don't support that stuff.
            // LoadBalanceFactorProvider implementation, org.jboss.modcluster.load.impl.SimpleLoadBalanceFactorProvider.
            final ModelNode node = loadmetric.get(CommonAttributes.LOAD_METRIC_SIMPLE);
            SimpleLoadBalanceFactorProvider myload = new SimpleLoadBalanceFactorProvider();
            myload.setLoadBalanceFactor(node.get(CommonAttributes.FACTOR).asInt(1));
            load = myload;
        }

        if (loadmetric.has(CommonAttributes.LOAD_METRIC_SERVER_SIDE)) {
            // TODO it seems we don't support that stuff.
            final ModelNode node = loadmetric.get(CommonAttributes.LOAD_METRIC_SERVER_SIDE);
        }

        if (loadmetric.has(CommonAttributes.LOAD_METRIC_WEB_CONTAINER_SIDE)) {
            final ModelNode node = loadmetric.get(CommonAttributes.LOAD_METRIC_WEB_CONTAINER_SIDE);
            String name = node.get(CommonAttributes.NAME).asString();
            int capacity = node.get(CommonAttributes.CAPACITY).asInt(512);
            Set<LoadMetric<LoadContext>> metrics = null;
            LoadMetric<LoadContext> metric = null;
            Class<? extends LoadMetric> loadMetricClass = null;
            if (name.equals("ActiveSessionsLoadMetric"))
                loadMetricClass = ActiveSessionsLoadMetric.class;
            if (name.equals("BusyConnectorsLoadMetric"))
                loadMetricClass = BusyConnectorsLoadMetric.class;
            if (name.equals("ReceiveTrafficLoadMetric"))
                loadMetricClass = ReceiveTrafficLoadMetric.class;
            if (name.equals("SendTrafficLoadMetric"))
                loadMetricClass = SendTrafficLoadMetric.class;
            if (name.equals("RequestCountLoadMetric"))
                loadMetricClass = RequestCountLoadMetric.class;
            if (loadMetricClass != null) {
                try {
                    metric = loadMetricClass.newInstance();
                    metric.setCapacity(capacity);
                    metrics.add(metric);
                } catch (InstantiationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (metrics!= null) {
                load = new DynamicLoadBalanceFactorProvider(metrics);
            }
        }

        if (load == null) {
            // Use a default one...
            log.info("Mod_cluster uses default load balancer provider");
            SimpleLoadBalanceFactorProvider myload = new SimpleLoadBalanceFactorProvider();
            myload.setLoadBalanceFactor(1);
            load = myload;
        }
        service = new org.jboss.modcluster.ModClusterService(config, load);
        adapter = new CatalinaEventHandlerAdapter(service, mbeanServer);
        try {
            adapter.start();
        } catch (JMException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        // TODO need something...
        if (adapter != null)
            try {
                adapter.stop();
            } catch (JMException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        adapter = null;
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    MBeanServer getMBeanServer() {
        return Registry.getRegistry(null, null).getMBeanServer();
    }
}
