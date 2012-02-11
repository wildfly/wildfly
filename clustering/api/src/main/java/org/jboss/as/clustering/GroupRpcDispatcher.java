/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering;

import java.util.List;

/**
 * Provide Remote Procedure Call services to a set of nodes that share a common group communication infrastructure.
 *
 * @author Brian Stansberry
 *
 * @version $Revision: 104233 $
 */
public interface GroupRpcDispatcher extends GroupCommunicationService {
    /**
     * Gets the default period, in ms, that the various <code>callMethodOnXXX</code> methods that don't specify a
     * <code>methodTimeout</code> parameter will wait for a response.
     */
    long getMethodCallTimeout();

    /**
     * Register an object upon which RPCs associated with the given serviceName will be invoked. The partition receives RPC
     * calls from other nodes in the cluster and demultiplexes them, according to a service name, to a particular service.
     * Consequently, each service must first subscribe with a particular service name in the partition. The subscriber does not
     * need to implement any specific interface: the call is handled dynamically through reflection.
     *
     * @param serviceName Name of the subscribing service (demultiplexing key)
     * @param handler object to be called when receiving a RPC for its key.
     */
    void registerRPCHandler(String serviceName, Object handler);

    /**
     * Register an object upon which RPCs associated with the given serviceName will be invoked. The partition receives RPC
     * calls from other nodes in the cluster and demultiplexes them, according to a service name, to a particular service.
     * Consequently, each service must first subscribe with a particular service name in the partition. The subscriber does not
     * need to implement any specific interface: the call is handled dynamically through reflection. In cases where the client
     * is using a scoped classloader, the client will need to provide a reference to the classloader if the service's RPC calls
     * use custom parameter or response object types. The classloader will be used to deserialize the RPC and/or response.
     *
     * @param serviceName Name of the subscribing service (demultiplexing key)
     * @param handler object to be called when receiving a RPC for its key.
     * @param classloader ClassLoader to be used when marshalling and unmarshalling RPC requests and responses.
     */
    void registerRPCHandler(String serviceName, Object handler, ClassLoader classloader);

    /**
     * Unregister the service from the partition
     *
     * @param serviceName Name of the service key (on which the demultiplexing occurs)
     * @param subscriber The target object that unsubscribes
     */
    void unregisterRPCHandler(String serviceName, Object subscriber);

    /**
     * Invoke an RPC call on all nodes of the partition/cluster and return their response values as a list. This convenience
     * method is equivalent to
     * {@link #callMethodOnCluster(String, String, Object[], Class[], boolean, ResponseFilter, long, boolean)}
     * callAsynchMethodOnCluster(serviceName, methodName, args, types, Object.class, excludeSelf, null, methodTimeout, false)}
     * where <code>methodTimeout</code> is the value returned by {@link #getMethodCallTimeout()}.
     *
     * @param serviceName name of the target service name on which calls are invoked
     * @param methodName name of the Java method to be called on remote services
     * @param args array of Java Object representing the set of parameters to be given to the remote method
     * @param types types of the parameters
     * @param excludeSelf <code>false</code> if the RPC must also be made on the current node of the partition,
     *        <code>true</code> if only on remote nodes
     *
     * @return an array of responses from nodes that invoked the RPC
     */
    <T> List<T> callMethodOnCluster(String serviceName, String methodName, Object[] args, Class<?>[] types, boolean excludeSelf) throws InterruptedException;

    /**
     * Invoke a synchronous RPC call on all nodes of the partition/cluster and return their response values as a list. This
     * convenience method is equivalent to
     * {@link #callMethodOnCluster(String, String, Object[], Class[], boolean, ResponseFilter, long, boolean)}
     * callAsynchMethodOnCluster(serviceName, methodName, args, types, Object.class, excludeSelf, filter, methodTimeout, false)}
     * where <code>methodTimeout</code> is the value returned by {@link #getMethodCallTimeout()}.
     *
     * @param serviceName name of the target service name on which calls are invoked
     * @param methodName name of the Java method to be called on remote services
     * @param args array of Java Object representing the set of parameters to be given to the remote method
     * @param types types of the parameters
     * @param excludeSelf <code>false</code> if the RPC must also be made on the current node of the partition,
     *        <code>true</code> if only on remote nodes
     * @param filter response filter instance which allows for early termination of the synchronous RPC call. Can be
     *        <code>null</code>.
     *
     * @return an array of responses from remote nodes
     */
    <T> List<T> callMethodOnCluster(String serviceName, String methodName, Object[] args, Class<?>[] types, boolean excludeSelf, ResponseFilter filter) throws InterruptedException;

    /**
     * Invoke an RPC call on all nodes of the partition/cluster and return their response values as a list.
     *
     * @param <T> the expected type of the return values
     * @param serviceName name of the target service name on which calls are invoked
     * @param methodName name of the Java method to be called on remote services
     * @param args array of Java Object representing the set of parameters to be given to the remote method
     * @param types types of the parameters
     * @param excludeSelf <code>false</code> if the RPC must also be made on the current node of the partition,
     *        <code>true</code> if only on remote nodes
     * @param filter response filter instance which allows for early termination of the RPC call once acceptable responses are
     *        received. Can be <code>null</code>, in which the call will not return until all nodes have responded.
     * @param methodTimeout max number of ms to wait for response to arrive before returning
     * @param unordered <code>true</code> if the HAPartition isn't required to ensure that this RPC is invoked on all nodes in a
     *        consistent order with respect to other RPCs originated by the same node
     * @return a list of responses from remote nodes
     */
    <T> List<T> callMethodOnCluster(String serviceName, String methodName, Object[] args, Class<?>[] types, boolean excludeSelf, ResponseFilter filter, long methodTimeout, boolean unordered) throws InterruptedException;

    /**
     * Invoke an RPC call on all nodes of the partition/cluster without waiting for any responses. The call will return
     * immediately after sending a message to the cluster telling nodes to invoke the RPC and will not wait for the nodes to
     * answer. Thus no return values are available. This convenience method is equivalent to
     * {@link #callAsynchMethodOnCluster(String, String, Object[], Class[], boolean, boolean)
     * callAsynchMethodOnCluster(serviceName, methodName, args, types, excludeSelf, false)}.
     *
     * @param serviceName name of the target service name on which calls are invoked
     * @param methodName name of the Java method to be called on remote services
     * @param args array of Java Object representing the set of parameters to be given to the remote method
     * @param types types of the parameters
     * @param excludeSelf <code>false</code> if the RPC must also be made on the current node of the partition,
     *        <code>true</code> if only on remote nodes
     */
    void callAsynchMethodOnCluster(String serviceName, String methodName, Object[] args, Class<?>[] types, boolean excludeSelf) throws InterruptedException;

    /**
     * Invoke an RPC call on all nodes of the partition/cluster without waiting for any responses. The call will return
     * immediately after sending a message to the cluster telling nodes to invoke the RPC and will not wait for the nodes to
     * answer. Thus no return values are available.
     *
     * @param serviceName name of the target service name on which calls are invoked
     * @param methodName name of the Java method to be called on remote services
     * @param args array of Java Object representing the set of parameters to be given to the remote method
     * @param types types of the parameters
     * @param excludeSelf <code>false</code> if the RPC must also be made on the current node of the partition,
     *        <code>true</code> if only on remote nodes
     * @param unordered <code>true</code> if the HAPartition isn't required to ensure that this RPC is invoked on all nodes in a
     *        consistent order with respect to other RPCs originated by the same node
     */
    void callAsynchMethodOnCluster(String serviceName, String methodName, Object[] args, Class<?>[] types, boolean excludeSelf, boolean unordered) throws InterruptedException;

    /**
     * Calls method on Cluster coordinator node only. The cluster coordinator node is the first node in the current cluster
     * view. This convenience method is equivalent to
     * {@link #callMethodOnCoordinatorNode(String, String, Object[], Class[], boolean, long, boolean)}
     * callMethodOnCoordinatorNode(serviceName, methodName, args, types, Object.class, excludeSelf, methodTimeout, false)} where
     * <code>methodTimeout</code> is the value returned by {@link #getMethodCallTimeout()}.
     *
     * @param serviceName name of the target service name on which calls are invoked
     * @param methodName name of the Java method to be called on remote services
     * @param args array of Java Object representing the set of parameters to be given to the remote method
     * @param types types of the parameters
     * @param excludeSelf <code>true</code> if the RPC should not be made on the current node even if the current node is the
     *        coordinator
     *
     * @return the value returned by the target method
     */
    <T> T callMethodOnCoordinatorNode(String serviceName, String methodName, Object[] args, Class<?>[] types, boolean excludeSelf) throws Exception;

    /**
     * Calls method on Cluster coordinator node only. The cluster coordinator node is the first node in the current cluster
     * view.
     *
     * @param <T> the expected type of the return value
     * @param serviceName name of the target service name on which calls are invoked
     * @param methodName name of the Java method to be called on remote services
     * @param args array of Java Object representing the set of parameters to be given to the remote method
     * @param types types of the parameters
     * @param excludeSelf <code>true</code> if the RPC should not be made on the current node even if the current node is the
     *        coordinator
     * @param methodTimeout max number of ms to wait for response to arrive before returning
     * @param unordered <code>true</code> if the HAPartition isn't required to ensure that this RPC is invoked on all nodes in a
     *        consistent order with respect to other RPCs originated by the same node
     * @return the value returned by the target method
     */
    <T> T callMethodOnCoordinatorNode(String serviceName, String methodName, Object[] args, Class<?>[] types, boolean excludeSelf, long methodTimeout, boolean unordered) throws Exception;

    /**
     * Calls method on target node only. This convenience method is equivalent to
     * {@link #callMethodOnNode(String, String, Object[], Class[], long, ClusterNode, boolean)}
     * callMethodOnNode(serviceName, methodName, args, types, Object.class, methodTimeout, targetNode, false)} where
     * <code>methodTimeout</code> is the value returned by {@link #getMethodCallTimeout()}.
     *
     * @param serviceName name of the target service name on which calls are invoked
     * @param methodName name of the Java method to be called on remote services
     * @param args array of Java Object representing the set of parameters to be given to the remote method
     * @param types types of the parameters
     * @param targetNode is the target of the call
     *
     * @return the value returned by the target method
     */
    <T> T callMethodOnNode(String serviceName, String methodName, Object[] args, Class<?>[] types, ClusterNode targetNode) throws Exception;

    /**
     * Calls method on target node only. This convenience method is equivalent to
     * {@link #callMethodOnNode(String, String, Object[], Class[], long, ClusterNode, boolean)}
     * callMethodOnNode(serviceName, methodName, args, types, Object.class, methodTimeout, targetNode, false)}.
     *
     * @param serviceName name of the target service name on which calls are invoked
     * @param methodName name of the Java method to be called on remote services
     * @param args array of Java Object representing the set of parameters to be given to the remote method
     * @param types types of the parameters
     * @param methodTimeout max number of ms to wait for response to arrive before returning
     * @param targetNode is the target of the call
     *
     * @return the value returned by the target method
     */
    <T> T callMethodOnNode(String serviceName, String methodName, Object[] args, Class<?>[] types, long methodTimeout, ClusterNode targetNode) throws Exception;

    /**
     * Calls method synchronously on target node only.
     *
     * @param <T> the expected type of the return value
     * @param serviceName name of the target service name on which calls are invoked
     * @param methodName name of the Java method to be called on remote services
     * @param args array of Java Object representing the set of parameters to be given to the remote method
     * @param types types of the parameters
     * @param methodTimeout max number of ms to wait for response to arrive before returning
     * @param targetNode is the target of the call
     * @param unordered <code>true</code> if the HAPartition isn't required to ensure that this RPC is invoked on all nodes in a
     *        consistent order with respect to other RPCs originated by the same node
     * @return the value returned by the target method
     */
    <T> T callMethodOnNode(String serviceName, String methodName, Object[] args, Class<?>[] types, long methodTimeout, ClusterNode targetNode, boolean unordered) throws Exception;

    /**
     * Calls method on target node only. The call will return immediately and will not wait for the node to answer. Thus no
     * answer is available. This convenience method is equivalent to
     * {@link #callAsyncMethodOnNode(String, String, Object[], Class[], ClusterNode, boolean)}
     * callAsynchMethodOnCluster(serviceName, methodName, args, types, methodTimeout, targetNode, false)}.
     *
     * @param serviceName name of the target service name on which calls are invoked
     * @param methodName name of the Java method to be called on remote services
     * @param args array of Java Object representing the set of parameters to be given to the remote method
     * @param types types of the parameters
     * @param targetNode is the target of the call
     */
    void callAsyncMethodOnNode(String serviceName, String methodName, Object[] args, Class<?>[] types, ClusterNode targetNode) throws Exception;

    /**
     * Calls method on target node only. The call will return immediately and will not wait for the node to answer. Thus no
     * answer is available.
     *
     * @param serviceName name of the target service name on which calls are invoked
     * @param methodName name of the Java method to be called on remote services
     * @param args array of Java Object representing the set of parameters to be given to the remote method
     * @param types types of the parameters
     * @param targetNode is the target of the call
     * @param unordered <code>true</code> if the HAPartition isn't required to ensure that this RPC is invoked on all nodes in a
     *        consistent order with respect to other RPCs originated by the same node
     */
    void callAsyncMethodOnNode(String serviceName, String methodName, Object[] args, Class<?>[] types, ClusterNode targetNode, boolean unordered) throws Exception;

    /**
     * Calls method on Cluster coordinator node only. The cluster coordinator node is the first node in the current cluster
     * view. The call will return immediately and will not wait for the node to answer. Thus no answer is available. This
     * convenience method is equivalent to
     * {@link #callAsyncMethodOnCoordinatorNode(String, String, Object[], Class[], boolean, boolean)
     * callMethodOnCoordinatorNode(serviceName, methodName, args, types, excludeSelf, false)} .
     *
     * @param serviceName name of the target service name on which calls are invoked
     * @param methodName name of the Java method to be called on remote services
     * @param args array of Java Object representing the set of parameters to be given to the remote method
     * @param types types of the parameters
     * @param excludeSelf <code>true</code> if the RPC should not be made on the current node even if the current node is the
     *        coordinator
     */
    void callAsyncMethodOnCoordinatorNode(String serviceName, String methodName, Object[] args, Class<?>[] types, boolean excludeSelf) throws Exception;

    /**
     * Calls method on Cluster coordinator node only. The cluster coordinator node is the first node in the current cluster
     * view. The call will return immediately and will not wait for the node to answer. Thus no answer is available.
     *
     * @param serviceName name of the target service name on which calls are invoked
     * @param methodName name of the Java method to be called on remote services
     * @param args array of Java Object representing the set of parameters to be given to the remote method
     * @param types types of the parameters
     * @param excludeSelf <code>true</code> if the RPC should not be made on the current node even if the current node is the
     *        coordinator
     * @param unordered <code>true</code> if the HAPartition isn't required to ensure that this RPC is invoked on all nodes in a
     *        consistent order with respect to other RPCs originated by the same node
     * @return the value returned by the target method
     */
    void callAsyncMethodOnCoordinatorNode(String serviceName, String methodName, Object[] args, Class<?>[] types, boolean excludeSelf, boolean unordered) throws Exception;
}