package org.jboss.as.clustering.management.support.impl;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.CommandAwareRpcDispatcher;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
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
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jgroups.JChannel;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.stack.IpAddress;

/**
 * This class supports making management-related RPC calls on a cluster.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ManagementAPIClusterSupport implements GroupMembershipListener {

    public static final Class<?>[] GET_CLUSTER_STATE_TYPES = new Class[]{String.class};
    public static final Class<?>[] GET_CACHE_STATE_TYPES = new Class[]{String.class, String.class};

    // definition of RPC interface on remote cluster nodes
    public static class RpcTarget {
        private final ManagementAPIClusterSupport support;

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

    private final String serviceHAName;
    private final GroupRpcDispatcher rpcDispatcher;
    private final GroupMembershipNotifier membershipNotifier;
    private final List<ClusterNode> members = new CopyOnWriteArrayList<ClusterNode>();
    private ClusterNode me;

    private RpcTarget rpcTarget;

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
     * Cluster-wide call to collect a view of channel/cache manager state on all
     * nodes in a cluster.
     */
    public List<RemoteClusterResponse> getClusterState(String channelName) throws InterruptedException {

        String serviceName = getServiceHAName();
        String methodName = "getClusterStateRemote";
        boolean excludeSelf = false;                                 // make RPC on all nodes
        ResponseFilter filter = null;                                // wait for all responses
        long methodTimeout = rpcDispatcher.getMethodCallTimeout();    // how long to wait for responses
        boolean unordered = false;                                   // don't need to order concurrent RPCs sequentially

        List<RemoteClusterResponse> rsps = null;
        try {
            // make the RPC
            rsps = rpcDispatcher.callMethodOnCluster(serviceName, methodName, new Object[]{channelName}, GET_CLUSTER_STATE_TYPES, excludeSelf, filter, methodTimeout, unordered);
        } catch (InterruptedException ie) {
            // this blocking method is being cancelled - just rethrow to let our caller know
            throw ie ;
        }
        return rsps;
    }

    /*
     * Cluster-wide call to collect a view of cache state on all nodes in the cluster.
     */
    public List<RemoteCacheResponse> getCacheState(String containerName, String cacheName) throws InterruptedException {

        String serviceName = getServiceHAName();
        String methodName = "getCacheStateRemote";
        boolean excludeSelf = false;                                 // make RPC on all nodes
        ResponseFilter filter = null;                                // wait for all responses
        long methodTimeout = rpcDispatcher.getMethodCallTimeout();   // how long to wait for responses
        boolean unordered = false;                                   // don't need to order concurrent RPCs sequentially

        List<RemoteCacheResponse> rsps = null;
        try {
            // make the RPC
            rsps = rpcDispatcher.callMethodOnCluster(serviceName, methodName, new Object[]{containerName, cacheName}, GET_CACHE_STATE_TYPES, excludeSelf, filter, methodTimeout, unordered);
        } catch (InterruptedException ie) {
           // this blocking method is being cancelled - just rethrow to let our caller know
           throw ie ;
        }
        return rsps;
    }

    //
    // implementation of handler methods on remote nodes
    //

    /*
     * Get the remote Channel and EmbeddedCacheManager state
     */
    private RemoteClusterResponse getClusterStateRemote(String channelName) {

        final String SYNC_UNICASTS = "sync_unicasts" ;
        final String ASYNC_UNICASTS = "async_unicasts" ;
        final String SYNC_MULTICASTS = "sync_multicasts" ;
        final String ASYNC_MULTICASTS = "async_multicasts" ;
        final String SYNC_ANYCASTS = "sync_anycasts" ;
        final String ASYNC_ANYCASTS = "async_anycasts" ;

        RemoteClusterResponse result = new RemoteClusterResponse(getLocalClusterNode());

        // get the information we need from services directly without using management interface
        ServiceContainer registry = ServiceContainerHelper.getCurrentServiceContainer();
        ServiceName channelServiceName = ChannelService.getServiceName(channelName);
        ServiceName containerServiceName = EmbeddedCacheManagerService.getServiceName(channelName);

        try {
            ServiceController<?> channelController = ServiceContainerHelper.getService(registry, channelServiceName);

            // check that the service has been installed and started
            boolean started = channelController != null && channelController.getValue() != null;
            if (started) {
                JChannel channel = (JChannel) channelController.getValue();

                // get the view
                String view = channel.getViewAsString();
                result.setView(view);

                // get the view history
                GMS gms = (GMS) channel.getProtocolStack().findProtocol("GMS") ;
                result.setViewHistory(gms.printPreviousViews());
            }
        } catch (ServiceNotFoundException snf) {
            // handle service not found - we need to access the service, but it ain't there
            // result: we cannot return a value for the cluster state - throw?
        } catch (IllegalStateException ise) {
            // handle illegal state exception - we need to get the value of the service, and the service
            // is there, but it ain't available (UP)
            // result: we cannot return a value for the cluster state - throw?
        }

        try {
            ServiceController<?> containerController = ServiceContainerHelper.getService(registry, containerServiceName);

            // check that the service has been installed and started
            boolean started = containerController != null && containerController.getValue() != null;
            if (started) {
                EmbeddedCacheManager container = (EmbeddedCacheManager) containerController.getValue();
                CommandAwareRpcDispatcher dispatcher =  ((JGroupsTransport)container.getTransport()).getCommandAwareRpcDispatcher();

                // get the unicast, multicast, anycast stats
                result.setAsyncUnicasts(getAtomicIntField(dispatcher, ASYNC_UNICASTS));
                result.setSyncUnicasts(getAtomicIntField(dispatcher, SYNC_UNICASTS));

                result.setAsyncMulticasts(getAtomicIntField(dispatcher, ASYNC_MULTICASTS));
                result.setSyncMulticasts(getAtomicIntField(dispatcher, SYNC_MULTICASTS));

                result.setAsyncAnycasts(getAtomicIntField(dispatcher, ASYNC_ANYCASTS));
                result.setSyncAnycasts(getAtomicIntField(dispatcher, ASYNC_ANYCASTS));
            }
        } catch (ServiceNotFoundException snf) {
            // handle service not found - we need to access the service, but it ain't there
            // result: we cannot return a value for the cluster state - throw?
        } catch (IllegalStateException ise) {
            // handle illegal state exception - we need to get the value of the service, and the service
            // is there, but it ain't available (UP)
            // result: we cannot return a value for the cluster state - throw?
        }

        return result;
    }

    private int getAtomicIntField(Object o, String name) {

        Class<?> c = o.getClass();
        try {
            Field f = getField(c, name);
            f.setAccessible(true);
            return ((AtomicInteger)f.get(o)).get();
        } catch(NoSuchFieldException nsfe) {
            System.out.println("No such field: " + name);
        } catch(IllegalAccessException iae) {
            System.out.println("Illegal access for field: " + name);
        }
        return 0;
    }

    private static Field getField(Class clazz, String fieldName) throws NoSuchFieldException {
      try {
        return clazz.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        Class superClass = clazz.getSuperclass();
        if (superClass == null) {
          throw e;
        } else {
          return getField(superClass, fieldName);
        }
      }
    }

    /*
     * Execute a services-based operation on all nodes in the cluster.
     */
    private RemoteCacheResponse getCacheStateRemote(String containerName, String cacheName) {

        RemoteCacheResponse result = new RemoteCacheResponse(getLocalClusterNode());

        // get the information we need from services directly without using management interface
        ServiceContainer registry = ServiceContainerHelper.getCurrentServiceContainer();
        ServiceName cacheServiceName = CacheService.getServiceName(containerName, cacheName);

        try {
            ServiceController<?>  cacheController = ServiceContainerHelper.getService(registry, cacheServiceName);

            // check that the service has been installed and started
            boolean started = cacheController != null && cacheController.getValue() != null;
            if (started) {
                Cache<?, ?> cache = (Cache<?, ?>) cacheController.getValue();
                // get the view (in JGroupsAddress format)
                StateTransferManager stateTransferManager = cache.getAdvancedCache().getComponentRegistry().getStateTransferManager();
                List<Address> cacheMembers = stateTransferManager.getCacheTopology().getMembers();
                result.setView(cacheMembers.toString());
            }
        } catch (ServiceNotFoundException snf) {
            // handle service not found - we need to access the cache service, but it ain't there
            // this can occur if an app has been undeployed on this node only
            result.setView("null view");
        } catch (IllegalStateException ise) {
            // handle illegal state exception - we need to get the value of the service, and the service
            // is there, but it ain't available (UP)
            // result: we cannot return a value for the cluster state - throw?
        }
        return result;
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
                InetAddress ip = ((IpAddress) jgroupsAddress).getIpAddress();
                int port = ((IpAddress) jgroupsAddress).getPort();
                result = findByInetAddressAndPort(members, ip, port);
            } else {
                // match by name
                String name = address.toString();
                result = findByName(members, name);
            }
        }
        return result;
    }

    private ClusterNode findByInetAddressAndPort(List<ClusterNode> members, InetAddress ip, int port) {
        ClusterNode result = null;
        for (ClusterNode node : members) {
            if (node.getIpAddress().equals(ip) && (node.getPort() == port)) {
                result = node;
                break;
            }
        }
        return result;
    }

    private ClusterNode findByName(List<ClusterNode> members, String name) {
        ClusterNode result = null;
        for (ClusterNode node : members) {
            if (node.getName().equals(name)) {
                result = node;
                break;
            }
        }
        return result;
    }

}
