/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009 Red Hat, Inc. and individual contributors
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

package org.jboss.as.web.session.mocks;

import org.jboss.as.clustering.web.ClusteringNotSupportedException;
import org.jboss.as.clustering.web.DistributedCacheManager;
import org.jboss.as.clustering.web.DistributedCacheManagerFactory;
import org.jboss.as.clustering.web.LocalDistributableSessionManager;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;

/**
 * 
 * 
 * @author Brian Stansberry
 * 
 * @version $Revision: $
 */
public class MockDistributedCacheManagerFactory implements DistributedCacheManagerFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T extends OutgoingDistributableSessionData> DistributedCacheManager<T> getDistributedCacheManager(LocalDistributableSessionManager localManager) throws ClusteringNotSupportedException {
        return (DistributedCacheManager<T>) MockDistributedCacheManager.INSTANCE;
    }

    @Override
    public boolean addDependencies(ServiceTarget target, ServiceBuilder<?> builder, JBossWebMetaData metaData) {
        return true;
    }
}
