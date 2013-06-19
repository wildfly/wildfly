package org.wildfly.extension.cluster;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusterSubsystemHelper {

    public static final ServiceName CHANNEL_PARENT = ServiceName.of("jboss", "jgroups", "channel");
    public static final int CHANNEL_PREFIX_LENGTH = CHANNEL_PARENT.toString().length();

    public static final ServiceName CACHE_CONTAINER_PARENT = ServiceName.of("jboss", "infinispan");
    public static final int CACHE_CONTAINER_PREFIX_LENGTH = CACHE_CONTAINER_PARENT.toString().length();

    /*
     * Returns the set of all Channel ServiceNames at this address (service jboss.jgroups.channel.*)
     * which are in the UP state.
     */
    public static Set<ServiceName> getChannelServiceNames(ServiceRegistry registry) {
        if (registry == null) {
            return Collections.emptySet();
        }
        Set<ServiceName> channelServiceNames = new HashSet<ServiceName>();
        List<ServiceName> serviceNames = registry.getServiceNames();
        for (ServiceName serviceName : serviceNames) {
            if (CHANNEL_PARENT.isParentOf(serviceName)) {
                if (serviceIsUp(registry, serviceName)) {
                    channelServiceNames.add(serviceName);
                }
            }
        }
        return channelServiceNames;
    }

    /*
     * Returns the set of all Channel ServiceNames at this address (service jboss.jgroups.channel.*)
     * which are in any state.
     */
    public static Set<ServiceName> getAllChannelServiceNames(ServiceRegistry registry) {
        if (registry == null) {
            return Collections.emptySet();
        }
        Set<ServiceName> channelServiceNames = new HashSet<ServiceName>();
        List<ServiceName> serviceNames = registry.getServiceNames();
        for (ServiceName serviceName : serviceNames) {
            if (CHANNEL_PARENT.isParentOf(serviceName)) {
                    channelServiceNames.add(serviceName);
            }
        }
        return channelServiceNames;
    }

    /*
     * Returns the set of all Channel names at this address (channel=*)
     * which are in the UP state.
     */
    public static Set<String> getChannelNames(ServiceRegistry registry) {
        if (registry == null) {
            return Collections.emptySet();
        }
        Set<String> channelNames = new HashSet<String>();
        for (ServiceName serviceName : getChannelServiceNames(registry)) {
            if (serviceIsUp(registry, serviceName)) {
                channelNames.add(getChannelNameFromChannelServiceName(serviceName));
            }
        }
        return channelNames;
    }

    public static String getChannelNameFromChannelServiceName(ServiceName serviceName) {
        String serviceNameAsString = serviceName.toString();
        return serviceNameAsString.substring(CHANNEL_PREFIX_LENGTH + 1);
    }

    public static ServiceName getChannelServiceNameFromChannelName(String name) {
        return CHANNEL_PARENT.append(name);
    }


    /*
     * Returns the set of all Cache ServiceNames at this address (service jboss.infinispan.<channel>.*)
     * which are in the UP state.
     */
    public static Set<ServiceName> getDeploymentServiceNames(ServiceRegistry registry, String cluster) {
        if (registry == null) {
            return Collections.emptySet();
        }
        Set<ServiceName> cacheServiceNames = new HashSet<ServiceName>();

        List<ServiceName> serviceNames = registry.getServiceNames();
        for (ServiceName serviceName : serviceNames) {
            if (CACHE_CONTAINER_PARENT.append(cluster).isParentOf(serviceName)) {

                // don't include cache config services
                if (serviceName.getSimpleName().equals("config")) {
                    continue;
                }
                // don't include default cache service
                if (CACHE_CONTAINER_PARENT.append(cluster).equals(serviceName)) {
                    continue;
                }

                if (ClusterSubsystemHelper.serviceIsUp(registry, serviceName)) {
                    cacheServiceNames.add(serviceName);
                }
            }
        }
        return cacheServiceNames;
    }

    /*
     * Returns the set of all Cache names at this address (channel=*)
     * which are in the UP state.
     *
     * We have services of the form:
     * jboss.infinispan.<container-name>.<cache-name>
     * jboss.infinispan.<container-name>.<cache-name>.config
     * and the default cache
     * jboss.infinispan.<container-name>
     * jboss.infinispan.<container-name>.config
     *
     * Also, many of the cache names will correspond to caches defined in XML, not deployments.
     *
     * Do we want to list deployments only?
     *
     */
    public static Set<String> getDeploymentNames(ServiceRegistry registry, String cluster) {
        Set<String> deploymentNames = new HashSet<String>();
        for (ServiceName serviceName : getDeploymentServiceNames(registry, cluster)) {
            if (ClusterSubsystemHelper.serviceIsUp(registry, serviceName)) {
               deploymentNames.add(getDeploymentNameFromDeploymentServiceName(serviceName, cluster));
            }
        }
        return deploymentNames;
    }

    public static String getDeploymentNameFromDeploymentServiceName(ServiceName serviceName, String cluster) {

        String serviceNameAsString = serviceName.toString();
        int containerPrefixLength = CACHE_CONTAINER_PARENT.append(cluster).toString().length();
        return serviceNameAsString.substring(containerPrefixLength + 1);
    }

    public static ServiceName getDeploymentServiceNameFromDeploymentName(String cluster, String name) {
        return CACHE_CONTAINER_PARENT.append(cluster).append(name);
    }

    /*
     * We want to return the names only of channel controllers which are UP
     * This method may be called with service names which do not correspond to existing channel services!
     */
    public static boolean serviceIsUp(ServiceRegistry registry, ServiceName name) {
        if (registry == null) return false;
        ServiceController<?> controller = registry.getService(name);
        return (controller != null) ? ServiceController.State.UP.equals(controller.getState()) : false;
    }
}
