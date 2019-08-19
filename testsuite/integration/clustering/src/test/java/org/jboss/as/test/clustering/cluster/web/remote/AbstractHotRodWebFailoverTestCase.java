/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.web.remote;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.transaction.TransactionMode;
import org.jboss.as.test.clustering.NodeUtil;
import org.jboss.as.test.clustering.cluster.web.AbstractWebFailoverTestCase;

/**
 * Variation of {@link AbstractWebFailoverTestCase} using invalidation cache with HotRod-based store implementation referencing
 * {@literal remote-cache-container} configuration. Test runs against running genuine Infinispan Server instance.
 *
 * @author Radoslav Husar
 */
public abstract class AbstractHotRodWebFailoverTestCase extends AbstractWebFailoverTestCase {

    public AbstractHotRodWebFailoverTestCase(String deploymentName) {
        super(deploymentName, CacheMode.LOCAL, TransactionMode.NON_TRANSACTIONAL);
    }

    @Override
    public void beforeTestMethod() {
        // Also start the Infinispan Server instance
        NodeUtil.start(this.controller, INFINISPAN_SERVER_1);

        NodeUtil.start(this.controller, this.nodes);
        NodeUtil.deploy(this.deployer, this.deployments);
    }
}
