package org.wildfly.extension.cluster.support;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.infinispan.statetransfer.StateTransferManager;
import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.GroupMembershipListener;
import org.jboss.as.clustering.GroupMembershipNotifier;
import org.jboss.as.clustering.GroupRpcDispatcher;
import org.jboss.as.clustering.ResponseFilter;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jgroups.JChannel;
import org.jgroups.stack.IpAddress;

/**
 * This class supports making management-related RPC calls on a cluster.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ManagementAPIClusterSupport implements GroupMembershipListener {

    public static final Class<?>[] EXECUTE_MANAGEMENT_OPERATION_TYPES = new Class[] {ModelNode.class};
    public static final Class<?>[] GET_CLUSTER_STATE_TYPES = new Class[] {String.class};
    public static final Class<?>[] GET_CACHE_STATE_TYPES = new Class[] {String.class, String.class};

    // definition of RPC interface on remote cluster nodes
    public static class RpcTarget {
        private final ManagementAPIClusterSupport support ;

        public RpcTarget(ManagementAPIClusterSupport support) {
            this.support = support;
        }

        public RemoteClusterResponse getClusterStateRemote(String channel) {
            return support.getClusterStateRemote(channel);
        }

        public RemoteCacheResponse getCacheStateRemote(String container, String cache) {
            return support.getCacheStateRemote(container, cache);
        }
    }

    private final String serviceHAName ;
    private final GroupRpcDispatcher rpcDispatcher ;
    private final GroupMembershipNotifier membershipNotifier ;
    private final List<ClusterNode> members = new CopyOnWriteArrayList<ClusterNode>();
    private ClusterNode me ;

    private RpcTarget rpcTarget ;

    public ManagementAPIClusterSupport(ManagementAPIClusterSupportConfiguration config) {
        this.serviceHAName = config.getCluster();
        this.rpcDispatcher = (GroupRpcDispatcher) config.getCoreGroupCommunicationService();
        this.membershipNotifier = (GroupMembershipNotifier) config.getCoreGroupCommunicationService();
    }

    public String getServiceHAName() {
        return serviceHAName;
    }

    public GroupRpcDispatcher getRpcDispatcher() {
        return rpcDispatcher;
    }

    public GroupMembershipNotifier getMembershipNotifier() {
        return membershipNotifier;
    }

    public RpcTarget getRpcTarget() {
        return rpcTarget;
    }

    public ClusterNode getLocalClusterNode() {
        return this.me;
    }

    public String getGroupName() {
        return rpcDispatcher.getGroupName();
    }

    public void start() throws Exception {

        this.me = this.rpcDispatcher.getClusterNode();

        this.rpcTarget = new RpcTarget(this);
        this.rpcDispatcher.registerRPCHandler(this.serviceHAName, this.rpcTarget);
        this.membershipNotifier.registerGroupMembershipListener(this);

        // update view of membership
        List<ClusterNode> allMembers = new ArrayList<ClusterNode>();
        for (ClusterNode node : this.rpcDispatcher.getClusterNodes()) {
            allMembers.add(node);
        }
        membershipChanged(new ArrayList<ClusterNode>(), allMembers, allMembers);
    }

    public void stop() throws Exception {

        if (this.rpcTarget != null) {
            this.rpcDispatcher.unregisterRPCHandler(this.serviceHAName, this.rpcTarget);
            this.rpcTarget = null;
            this.membershipNotifier.unregisterGroupMembershipListener(this);

            // update view of membership
            List<ClusterNode> dead = new ArrayList<ClusterNode>(members);
            List<ClusterNode> empty = new ArrayList<ClusterNode>();
            membershipChanged(dead, empty, empty);

            this.me = null;
        }
    }


    @Override
    public synchronized void membershipChanged(List<ClusterNode> deadMembers, List<ClusterNode> newMembers, List<ClusterNode> allMembers) {
        // fill me in
    }

    public synchronized void membershipChangedDuringMerge(List<ClusterNode> deadMembers, List<ClusterNode> newMembers, List<ClusterNode> allMembers, List<List<ClusterNode>> originatingGroups) {
        // fill me in
    }

    //
    // implementation of cluster-wide method calls
    //

    /*
     * Cluster-wide call to execute a services-based operation on all nodes in the cluster.
     */
    public List<RemoteClusterResponse> getClusterState(String channelName) {

        String serviceName = getServiceHAName();
        String methodName = "getClusterStateRemote";
        boolean excludeSelf = false ;                                 // make RPC on all nodes
        ResponseFilter filter = null ;                                // wait for all responses
        long methodTimeout = rpcDispatcher.getMethodCallTimeout();    // how long to wait for responses
        boolean unordered = false ;                                   // don't need to order concurrent RPCs sequentially

        List<RemoteClusterResponse> rsps = null ;
        try {
            // make the RPC
            rsps = rpcDispatcher.callMethodOnCluster(serviceName, methodName, new Object[] {channelName}, GET_CLUSTER_STATE_TYPES, excludeSelf, filter, methodTimeout, unordered);
        } catch(InterruptedException ie) {
            // handle this exception
            System.out.println("InterruptedException: " + ie.toString());
        }
        return rsps ;
    }

    /*
     * Cluster-wide call to execute a services-based operation on all nodes in the cluster.
     */
    public List<RemoteCacheResponse> getCacheState(String containerName, String cacheName) {

        String serviceName = getServiceHAName();
        String methodName = "getCacheStateRemote";
        boolean excludeSelf = false ;                                 // make RPC on all nodes
        ResponseFilter filter = null ;                                // wait for all responses
        long methodTimeout = rpcDispatcher.getMethodCallTimeout();    // how long to wait for responses
        boolean unordered = false ;                                   // don't need to order concurrent RPCs sequentially

        List<RemoteCacheResponse> rsps = null ;
        try {
            // make the RPC
            rsps = rpcDispatcher.callMethodOnCluster(serviceName, methodName, new Object[] {containerName, cacheName}, GET_CACHE_STATE_TYPES, excludeSelf, filter, methodTimeout, unordered);
        } catch(InterruptedException ie) {
            // handle this exception
            System.out.println("InterruptedException: " + ie.toString());
        }
        return rsps ;
    }

    //
    // implementation of handler methods on remote nodes
    //

    /*
     * Execute a services-based operation on all nodes in the cluster.
     */
     private RemoteClusterResponse getClusterStateRemote(String channelName) {

         RemoteClusterResponse result = new RemoteClusterResponse(getLocalClusterNode()) ;

         // get the information we need from services directly without using management interface
         ServiceContainer registry = ServiceContainerHelper.getCurrentServiceContainer();
         ServiceName channelServiceName = ChannelService.getServiceName(channelName);
         ServiceName containerServiceName = EmbeddedCacheManagerService.getServiceName(channelName);
         ServiceController<?> channelController = channelController = ServiceContainerHelper.getService(registry, channelServiceName);

         // check that the service has been installed and started
         boolean started = channelController != null && channelController.getValue() != null;
         if (started) {
             try {
             JChannel channel = (JChannel) channelController.getValue();
             // get the view
             String view = channel.getViewAsString();
             result.setView(view);
             }
             catch(Exception e) {
                 System.out.println("Exception occurred: " + e.toString());
             }
         }
        return result ;
    }

    /*
     * Execute a services-based operation on all nodes in the cluster.
     */
     private RemoteCacheResponse getCacheStateRemote(String containerName, String cacheName) {

         RemoteCacheResponse result = new RemoteCacheResponse(getLocalClusterNode()) ;

         // get the information we need from services directly without using management interface
         ServiceContainer registry = ServiceContainerHelper.getCurrentServiceContainer();
         ServiceName cacheServiceName = CacheService.getServiceName(containerName, cacheName);
         ServiceController<?> cacheController = cacheController = ServiceContainerHelper.getService(registry, cacheServiceName);

         // check that the service has been installed and started
         boolean started = cacheController != null && cacheController.getValue() != null;
         if (started) {
             try {
             Cache<?,?> cache = (Cache<?,?>) cacheController.getValue();
             // get the view (in JGroupsAddress format)
             StateTransferManager stateTransferManager = cache.getAdvancedCache().getComponentRegistry().getStateTransferManager();
             List<Address> cacheMembers = stateTransferManager.getCacheTopology().getMembers() ;
             result.setView(cacheMembers.toString());
             }
             catch(Exception e) {
                 System.out.println("Exception occurred: " + e.toString());
             }
         }
        return result ;
    }

    private ClusterNode cacheAddressToClusterNode(Address address) {
        List<ClusterNode> members = this.rpcDispatcher.getClusterNodes();
        ClusterNode result = null;

        // look through the list of members for the matching address
        // this is not easy as there are som many address types around
        if (address instanceof JGroupsAddress) {
            org.jgroups.Address jgroupsAddress = ((JGroupsAddress) address).getJGroupsAddress();
            if (jgroupsAddress instanceof IpAddress) {
                // match by InetAddress and port
                InetAddress ip = ((IpAddress)jgroupsAddress).getIpAddress();
                int port = ((IpAddress)jgroupsAddress).getPort();
                result = findByInetAddressAndPort(members, ip, port) ;
            } else {
                // match by name
                String name = address.toString();
                result = findByName(members, name);
            }
        }
        return result ;
    }

    private ClusterNode findByInetAddressAndPort(List<ClusterNode> members, InetAddress ip, int port) {
        ClusterNode result = null;
        for (ClusterNode node : members) {
            if (node.getIpAddress().equals(ip) && (node.getPort() == port)) {
                result = node ;
                break;
            }
        }
        return result ;
    }

    private ClusterNode findByName(List<ClusterNode> members, String name) {
        ClusterNode result = null;
        for (ClusterNode node : members) {
            if (node.getName().equals(name)) {
                result = node ;
                break;
            }
        }
        return result ;
    }

}
