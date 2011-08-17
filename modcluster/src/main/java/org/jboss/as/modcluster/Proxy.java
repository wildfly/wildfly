package org.jboss.as.modcluster;

import java.util.Iterator;
import java.util.List;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

public class Proxy {
    String host;
    int port;

    public Proxy(ModelNode operation) throws OperationFailedException {
        List<Property> list = operation.asPropertyList();
        Iterator<Property> it= list.iterator();
        host = null;
        port = 0;
        while(it.hasNext()) {
            Property prop= it.next();
            if (prop.getName().equals("host")) {
                host = prop.getValue().toString();
            }
            if (prop.getName().equals("port")) {
                port = Integer.parseInt(ContextHost.RemoveQuotes(prop.getValue().toString()));
            }
        }
        if (host == null || port == 0)
            throw new OperationFailedException(new ModelNode().set("need host and port"));

        host = ContextHost.RemoveQuotes(host);
    }
}
