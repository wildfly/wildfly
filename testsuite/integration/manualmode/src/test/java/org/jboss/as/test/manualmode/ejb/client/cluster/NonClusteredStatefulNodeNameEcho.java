/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.test.manualmode.ejb.client.cluster;

import javax.ejb.Remote;
import javax.ejb.Stateful;
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
