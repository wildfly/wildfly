package org.jboss.as.test.clustering.cluster.dispatcher.bean;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.group.Node;

public class TestCommand implements Command<String, Node> {
    private static final long serialVersionUID = -3405593925871250676L;

    @Override
    public String execute(Node node) {
        return node.getName();
    }
}
