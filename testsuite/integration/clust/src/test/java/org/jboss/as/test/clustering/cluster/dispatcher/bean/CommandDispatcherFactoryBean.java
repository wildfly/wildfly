package org.jboss.as.test.clustering.cluster.dispatcher.bean;

import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.Node;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.dispatcher.MembershipListener;

@Singleton
@Startup
@Local(CommandDispatcherFactory.class)
public class CommandDispatcherFactoryBean implements CommandDispatcherFactory {

    @Resource(lookup = "java:jboss/clustering/dispatcher/ejb")
    private CommandDispatcherFactory factory;

    @Override
    public <C> CommandDispatcher<C> createCommandDispatcher(ServiceName service, C context) {
        return this.factory.createCommandDispatcher(service, context);
    }

    @Override
    public <C> CommandDispatcher<C> createCommandDispatcher(ServiceName service, C context, MembershipListener listener) {
        return this.factory.createCommandDispatcher(service, context, listener);
    }

    @Override
    public boolean isCoordinator() {
        return this.factory.isCoordinator();
    }

    @Override
    public Node getLocalNode() {
        return this.factory.getLocalNode();
    }

    @Override
    public Node getCoordinatorNode() {
        return this.factory.getCoordinatorNode();
    }

    @Override
    public List<Node> getNodes() {
        return this.factory.getNodes();
    }
}
