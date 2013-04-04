package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jgroups.Channel;
import org.jgroups.stack.Protocol;

/**
 * @author Richard Achmatowicz (c) 2012 Red Hat Inc.
 */
public class ChannelInstanceCustomResource implements Resource {

    public static final ServiceName CHANNEL_PARENT = ServiceName.of("jboss", "jgroups", "channel");
    public static final int CHANNEL_PREFIX_LENGTH = CHANNEL_PARENT.toString().length();

    private final ServiceName serviceName;
    private final ServiceController serviceController ;

    public ChannelInstanceCustomResource(ServiceName name, ServiceController controller) {
        this.serviceName = name;
        this.serviceController = controller;
    }

    public ServiceName getServiceName() {
        return serviceName;
    }

    public ServiceController getServiceController() {
        return serviceController;
    }

    // this resource holds no persistent state, so "turn off" the model
    @Override
    public ModelNode getModel() {
        return new ModelNode();
    }

    @Override
    public void writeModel(ModelNode newModel) {
        throw MESSAGES.immutableResource();
    }

    @Override
    public boolean isModelDefined() {
        return false;
    }

    // this resource does have children, so activate te creation of children
    // the children are protocol resources associated with the channel
    @Override
    public boolean hasChild(PathElement element) {
        if (ModelKeys.PROTOCOL.equals(element.getKey())) {
            return hasProtocol(element);
        }
        return false;
    }

    @Override
    public Resource requireChild(PathElement element) {
        return null;
    }

    @Override
    public Resource getChild(PathElement element) {
        return hasProtocol(element) ? PlaceholderResource.INSTANCE : null;
    }

    @Override
    public Resource removeChild(PathElement address) {
        throw MESSAGES.immutableResource();
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        throw MESSAGES.immutableResource();
    }

    @Override
    public boolean hasChildren(String childType) {
        if (ModelKeys.PROTOCOL.equals(childType)) {
            return getChildrenNames(ModelKeys.PROTOCOL).size() > 0;
        } else {
            return false;
        }
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (ModelKeys.PROTOCOL.equals(childType)) {
            Set<ResourceEntry> result = new HashSet<ResourceEntry>();
            for (String name : getProtocolNames()) {
                result.add(new PlaceholderResource.PlaceholderResourceEntry(ModelKeys.PROTOCOL, name));
            }
            return result;
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (ModelKeys.PROTOCOL.equals(childType)) {
            return getProtocolNames();
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Set<String> getChildTypes() {
        Set<String> result = new HashSet<String>();
        result.add(ModelKeys.PROTOCOL);
        return result;
    }

    @Override
    public Resource navigate(PathAddress address) {
        // TODO: check this
        return Resource.Tools.navigate(this, address);
    }

    @Override
    public Resource clone() {
        // don't clone the pointer to the unique controller for this service
        ChannelInstanceCustomResource clone = new ChannelInstanceCustomResource(getServiceName(), getServiceController());
        return clone;
    }

    @Override
    public boolean isRuntime() {
        return true;
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    private boolean hasProtocol(PathElement element) {
        String protocolName = element.getValue();
        return getProtocolNames().contains(protocolName);
    }

    private Set<String> getProtocolNames() {
        Channel channel ;
        if (serviceController == null || ((channel = (Channel) serviceController.getValue()) == null)) {
            return Collections.emptySet();
        }

        Set<String> names = new HashSet<String>();
        List<Protocol> protocols = channel.getProtocolStack().getProtocols();
        for (Protocol protocol : protocols) {
            String name = protocol.getName();
            names.add(name);
        }
        return names ;
    }

    /*
     * ResourceEntry extends the resource and additionally provides information on its path
     */
    public static class ChannelInstanceCustomResourceEntry extends ChannelInstanceCustomResource implements ResourceEntry {

        final PathElement path;

        public ChannelInstanceCustomResourceEntry(final ServiceName serviceName, final ServiceController controller, final PathElement path) {
            super(serviceName, controller);
            this.path = path;
        }

        public ChannelInstanceCustomResourceEntry(final ServiceName serviceName, final ServiceController controller, final String type, final String name) {
            super(serviceName, controller);
            this.path = PathElement.pathElement(type, name);
        }

        @Override
        public String getName() {
            return path.getValue();
        }

        @Override
        public PathElement getPathElement() {
            return path;
        }

        @Override
        public ChannelInstanceCustomResourceEntry clone() {
            return new ChannelInstanceCustomResourceEntry(getServiceName(), getServiceController(), getPathElement());
        }
    }

}
