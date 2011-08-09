package org.jboss.as.modcluster;

import java.util.Iterator;
import java.util.List;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

public class ContextHost {
    String webcontext = null;
    String webhost = null;
    int waittime = 10;

    public ContextHost(ModelNode operation) throws OperationFailedException {
        List<Property> list = operation.asPropertyList();
        Iterator<Property> it= list.iterator();
        webcontext = null;
        webhost = null;
        while(it.hasNext()) {
            Property prop= it.next();
            if (prop.getName().equals("context")) {
                webcontext = prop.getValue().toString();
            }
            if (prop.getName().equals("virtualhost")) {
                webhost = prop.getValue().toString();
            }
            if (prop.getName().equals("waittime")) {
                waittime = Integer.parseInt(RemoveQuotes(prop.getValue().toString()));
            }
        }
        if (webcontext == null || webhost == null)
            throw new OperationFailedException(new ModelNode().set("need context and host"));

        webcontext = RemoveQuotes(webcontext);
        webhost = RemoveQuotes(webhost);
        if (webcontext.equals("/"))
            webcontext = "";
    }

    public static String RemoveQuotes(String string) {
        if (string.endsWith("\"") && string.startsWith("\""))
            return string.substring(1, string.length() -1);
        return null;
    }
}
