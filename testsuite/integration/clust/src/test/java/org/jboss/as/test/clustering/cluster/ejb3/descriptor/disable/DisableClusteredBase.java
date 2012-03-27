/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb3.descriptor.disable;

import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.logging.Logger;

/**
 * @author Ondrej Chaloupka
 * @author Brian Stansberry
 */
public class DisableClusteredBase implements java.io.Serializable, DisableClusteredRemote {
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    private Logger log = Logger.getLogger(getClass());

    public String getNodeState() {
        String nodeName = NodeNameGetter.getNodeName();
        log.info("getNodeState, " + nodeName);
        return nodeName;
    }

    public void setUpFailover(String failover) {
        // To setup the failover property
        log.info("Setting up failover property: " + failover);
        System.setProperty("JBossCluster-DoFail", failover);
    }

}
