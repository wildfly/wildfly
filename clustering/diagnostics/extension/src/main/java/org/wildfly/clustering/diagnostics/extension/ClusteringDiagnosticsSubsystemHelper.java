package org.wildfly.clustering.diagnostics.extension;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.clustering.diagnostics.extension.deployment.ClusteredDeploymentRepository;
import org.wildfly.clustering.diagnostics.extension.deployment.ClusteredEjbDeploymentInformation;
import org.wildfly.clustering.diagnostics.extension.deployment.ClusteredModuleDeployment;
import org.wildfly.clustering.diagnostics.extension.deployment.ClusteredWarDeploymentInformation;

/**
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusteringDiagnosticsSubsystemHelper {

    public static final ServiceName CHANNEL_PARENT = ServiceName.of("jboss", "jgroups", "channel");
    public static final int CHANNEL_PREFIX_LENGTH = CHANNEL_PARENT.toString().length();

    /*
     * Returns the set of all Channel ServiceNames at this address (service jboss.jgroups.channel.*)
     * which are in the UP state.
     *
     * NOTE: We need to eliminate the service names which we create for our own use; namely,
     * X.management             - the ChannelManagementServiceName for channel X
     * X.management.controller  - the ChannelManagementServiceNameController for channel X
     *
     */
    public static Set<ServiceName> getChannelServiceNames(ServiceRegistry registry) {
        if (registry == null) {
            return Collections.emptySet();
        }
        Set<ServiceName> channelServiceNames = new HashSet<ServiceName>();
        List<ServiceName> serviceNames = registry.getServiceNames();
        for (ServiceName serviceName : serviceNames) {
            if (isValidChannelServiceName(serviceName)) {
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
            if (isValidChannelServiceName(serviceName)) {
                channelServiceNames.add(serviceName);
            }
        }
        return channelServiceNames;
    }

    /*
     * Need to differentiate between real channel names and the services which are used to support them
     */
    public static boolean isValidChannelServiceName(ServiceName serviceName) {
        boolean hasParentPrefix = CHANNEL_PARENT.isParentOf(serviceName);
        boolean hasLengthFour = serviceName.length() == 4;

        return hasParentPrefix && hasLengthFour;
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
     * This method returns all web module deployments which:
     * - contain the distributable directive
     * - which use a particular cluster for their web session cache.
     *
     * The information is obtained from the ClusteredDeploymentRepository.
     */
    public static Set<String> getWebDeploymentNames(ServiceRegistry registry, String cluster) {
        Set<String> deploymentNames = new HashSet<String>();

        Map<String, ClusteredModuleDeployment> deployments = getClusteredDeployments(registry);

        for (Map.Entry<String, ClusteredModuleDeployment> entry : deployments.entrySet()) {
            String id = entry.getKey();
            ClusteredModuleDeployment deployment = entry.getValue();

            // war deplooyment
            if (deployment.getWar() != null) {
                ClusteredWarDeploymentInformation warInfo = deployment.getWar();
                String container = warInfo.getSessionCacheContainer();
                String cache = warInfo.getSessionCache();

                // only return deployments using the same cluster
                if (cluster.equals(container)) {
                    deploymentNames.add(id);
                }
            }
        }
        return deploymentNames;
    }

    /*
     * THis method returns cache information for a distributable web application:
     * - in a given module deployment
     * - on a given cluster
     *
     * The cache information is obtained from the ClusteredDeploymentRepository.
     */
    public static DeploymentCacheInfo getWebDeploymentCacheInfo(ServiceRegistry registry, String cluster, String deploymentName) {
        Set<String> deploymentNames = new HashSet<String>();

        Map<String, ClusteredModuleDeployment> deployments = getClusteredDeployments(registry);

        DeploymentCacheInfo info = null;
        for (Map.Entry<String, ClusteredModuleDeployment> entry : deployments.entrySet()) {
            String id = entry.getKey();

            if (id.equals(deploymentName)) {
                ClusteredModuleDeployment deployment = entry.getValue();
                // war deployment
                if (deployment.getWar() != null) {
                    ClusteredWarDeploymentInformation warInfo = deployment.getWar();
                    String container = warInfo.getSessionCacheContainer();
                    String cache = warInfo.getSessionCache();

                    // only return deployments using the same cluster
                    if (cluster.equals(container)) {
                        return new DeploymentCacheInfo(container, cache);
                    }
                }
            }
        }
        return null;
    }

    /*
     * This method returns all EJB module deployments which:
     * - contain @Clustered SFSB beans
     * - which use a particular cluster for their session cache.
     *
     * The information is obtained from the ClusteredDeploymentRepository.
     */
    public static Set<String> getBeanDeploymentNames(ServiceRegistry registry, String cluster) {
        Set<String> deploymentNames = new HashSet<String>();

        Map<String, ClusteredModuleDeployment> deployments = getClusteredDeployments(registry);

        for (Map.Entry<String, ClusteredModuleDeployment> entry : deployments.entrySet()) {
            String id = entry.getKey();
            ClusteredModuleDeployment deployment = entry.getValue();

            // ejb deployment
            Map<String, ClusteredEjbDeploymentInformation> ejbs = deployment.getEjbs();
            boolean first = true;
            if (ejbs != null) {
                for (Map.Entry<String, ClusteredEjbDeploymentInformation> ejbEntry : ejbs.entrySet()) {
                    String ejbId = ejbEntry.getKey();
                    ClusteredEjbDeploymentInformation ejbInfo = ejbEntry.getValue();
                    String container = ejbInfo.getSessionCacheContainer();
                    String cache = ejbInfo.getSessionCache();

                    // only return bean deployments using the same channel
                    if (cluster.equals(container) && first) {
                        deploymentNames.add(id);
                        first = false;
                    }
                }
            }
        }
        return deploymentNames;
    }

    /*
     * THis method returns cache information for a @Clustered SFSB bean:
     * - in a given module deployment
     * - on a given cluster
     *
     * The cache information is obtained from the ClusteredDeploymentRepository.
     */
    public static DeploymentCacheInfo getBeanDeploymentCacheInfo(ServiceRegistry registry, String cluster, String deploymentName, String beanDeployment) {
        Set<String> deploymentNames = new HashSet<String>();

        Map<String, ClusteredModuleDeployment> deployments = getClusteredDeployments(registry);

        DeploymentCacheInfo info = null;
        for (Map.Entry<String, ClusteredModuleDeployment> entry : deployments.entrySet()) {
            String id = entry.getKey();

            if (id.equals(deploymentName)) {
                ClusteredModuleDeployment deployment = entry.getValue();
                // ejb deployment

                Map<String, ClusteredEjbDeploymentInformation> ejbs = deployment.getEjbs();
                if (ejbs != null) {
                    for (Map.Entry<String, ClusteredEjbDeploymentInformation> ejbEntry : ejbs.entrySet()) {
                        String ejbId = ejbEntry.getKey();
                        ClusteredEjbDeploymentInformation ejbInfo = ejbEntry.getValue();
                        String beanName = ejbInfo.getBeanName();
                        String container = ejbInfo.getSessionCacheContainer();
                        String cache = ejbInfo.getSessionCache();

                        // only return bean deployments using the same channel
                        if (cluster.equals(container) && beanName.equals(beanDeployment)) {
                            return new DeploymentCacheInfo(container, cache);
                        }
                    }
                }
            }
        }
        return null;
    }

    /*
     * THis method returns the DeploymentModuleIdentifier for a deployment.
     */
    public static DeploymentModuleIdentifier getDeploymentModuleIdentifier(ServiceRegistry registry, String deploymentName) {

        Map<String, ClusteredModuleDeployment> deployments = getClusteredDeployments(registry);

        for (Map.Entry<String, ClusteredModuleDeployment> entry : deployments.entrySet()) {
            String id = entry.getKey();

            if (id.equals(deploymentName)) {
                ClusteredModuleDeployment deployment = entry.getValue();
                return deployment.getIdentifier();
            }
        }
        return null;
    }

    /*
     * Get all deployment names, web and bean, which pertain to a given cluster
     */
    public static Set<String> getDeploymentNames(ServiceRegistry registry, String cluster) {
        Set<String> deploymentNames = new HashSet<String>();
        deploymentNames = getWebDeploymentNames(registry, cluster);
        deploymentNames.addAll(getBeanDeploymentNames(registry, cluster));
        return deploymentNames;
    }

    /*
     * Get all bean names for a given cluster and deployment.
     */
    public static Set<String> getBeanNamesForDeployment(ServiceRegistry registry, String cluster, String deploymentName) {
        Set<String> beanNames = new HashSet<String>();

        Map<String, ClusteredModuleDeployment> deployments = getClusteredDeployments(registry);

        for (Map.Entry<String, ClusteredModuleDeployment> entry : deployments.entrySet()) {
            String id = entry.getKey();

            if (id.equals(deploymentName)) {
                ClusteredModuleDeployment deployment = entry.getValue();
                // ejb deployment

                Map<String, ClusteredEjbDeploymentInformation> ejbs = deployment.getEjbs();
                if (ejbs != null) {
                    for (Map.Entry<String, ClusteredEjbDeploymentInformation> ejbEntry : ejbs.entrySet()) {
                        String ejbId = ejbEntry.getKey();
                        ClusteredEjbDeploymentInformation ejbInfo = ejbEntry.getValue();
                        String beanName = ejbInfo.getBeanName();
                        beanNames.add(beanName);
                    }
                }
            }
        }
        return beanNames;
    }

    /*
     * Get all clustered deployments from the ClusteredDeploymentRepository.
     */
    public static Map<String, ClusteredModuleDeployment> getClusteredDeployments(ServiceRegistry registry) {
        ServiceController<?> controller = registry.getService(ClusteredDeploymentRepository.SERVICE_NAME);
        ClusteredDeploymentRepository repository = (ClusteredDeploymentRepository) controller.getValue();
        return repository.getModules();
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

    /*
     * We want to return the names only of channel controllers which are UP
     * This method may be called with service names which do not correspond to existing channel services!
     */
    public static void startService(ServiceController<?> controller) throws InterruptedException {
        try {
            // set the mode to active to start the service
            ServiceController.Mode mode = controller.getMode();
            controller.setMode(ServiceController.Mode.ACTIVE);
            // wait until the service comes up
            controller.awaitValue();
        } catch (InterruptedException e) {
            throw e;
        }
    }

}
