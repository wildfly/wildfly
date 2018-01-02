package org.jboss.as.test.clustering.cluster.dispatcher.bean;

import java.util.Map;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.dispatcher.CommandResponse;
import org.wildfly.clustering.group.Node;

@Singleton
@Startup
@Local(CommandDispatcher.class)
public class CommandDispatcherBean implements CommandDispatcher<Node> {
    @EJB
    private CommandDispatcherFactory factory;
    private CommandDispatcher<Node> dispatcher;

    @PostConstruct
    public void init() {
        this.dispatcher = this.factory.createCommandDispatcher("CommandDispatcherTestCase", this.getContext());
    }

    @PreDestroy
    public void destroy() {
        this.close();
    }

    @Override
    public <R> CommandResponse<R> executeOnNode(Command<R, ? super Node> command, Node node) throws CommandDispatcherException {
        return this.dispatcher.executeOnNode(command, node);
    }

    @Override
    public <R> Map<Node, CommandResponse<R>> executeOnCluster(Command<R, ? super Node> command, Node... excludedNodes) throws CommandDispatcherException  {
        return this.dispatcher.executeOnCluster(command, excludedNodes);
    }

    @Override
    public <R> Future<R> submitOnNode(Command<R, ? super Node> command, Node node) throws CommandDispatcherException  {
        return this.dispatcher.submitOnNode(command, node);
    }

    @Override
    public <R> Map<Node, Future<R>> submitOnCluster(Command<R, ? super Node> command, Node... excludedNodes) throws CommandDispatcherException  {
        return this.dispatcher.submitOnCluster(command, excludedNodes);
    }

    @Override
    public void close() {
        this.dispatcher.close();
    }

    @Override
    public Node getContext() {
        return this.factory.getGroup().getLocalMember();
    }
}
