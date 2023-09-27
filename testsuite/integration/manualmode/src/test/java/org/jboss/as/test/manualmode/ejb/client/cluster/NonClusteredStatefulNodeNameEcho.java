/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.client.cluster;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

/**
 * @author Jaikiran Pai
 */
@Stateful
@Remote(NodeNameEcho.class)
public class NonClusteredStatefulNodeNameEcho implements NodeNameEcho {

    private NodeNameEcho clusteredDelegate;

    @Override
    public String getNodeName(final boolean preferOtherNode) {
        final String me = System.getProperty("jboss.node.name");
        if (this.clusteredDelegate == null) {
            this.lookupClusteredBean();
        }
        String nodeName = this.clusteredDelegate.getNodeName(false);
        if (preferOtherNode && me.equals(nodeName)) {
            for (int i = 0; i < 10; i++) {
                // do another lookup to try creating a SFSB instance on a different node
                this.lookupClusteredBean();
                nodeName = this.clusteredDelegate.getNodeName(false);
                if (!me.equals(nodeName)) {
                    return nodeName;
                }
            }
            throw new RuntimeException("Invocation on clustered bean always (10 times) ended up picking current node " + me);
        }
        return nodeName;
    }

    private void lookupClusteredBean() {
        final Properties props = new Properties();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        try {
            final Context context = new InitialContext(props);
            try {
                this.clusteredDelegate = (NodeNameEcho) context.lookup("ejb:/server-to-server-clustered-ejb-invocation//ClusteredStatefulNodeNameEcho!org.jboss.as.test.manualmode.ejb.client.cluster.NodeNameEcho?stateful");
            } finally {
                context.close();
            }
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }
    }
}
