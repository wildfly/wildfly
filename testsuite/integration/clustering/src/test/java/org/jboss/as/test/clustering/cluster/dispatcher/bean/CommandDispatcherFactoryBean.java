package org.jboss.as.test.clustering.cluster.dispatcher.bean;


import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;

@Singleton
@Startup
@Local(CommandDispatcherFactory.class)
public class CommandDispatcherFactoryBean implements CommandDispatcherFactory {

    @Resource(lookup = "java:jboss/clustering/dispatcher/default")
    private CommandDispatcherFactory factory;

    @Override
    public <C> CommandDispatcher<C> createCommandDispatcher(Object service, C context) {
        return this.factory.createCommandDispatcher(service, context);
    }

    @Override
    public Group getGroup() {
        return this.factory.getGroup();
    }
}
