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

package org.jboss.as.capedwarf.deployment;

import javax.transaction.TransactionManager;

import org.infinispan.Cache;
import org.jboss.as.capedwarf.services.MuxIdService;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.Channel;

/**
 * Install mux id gen service.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfMuxIdProcessor extends CapedwarfDeploymentUnitProcessor {
    protected void doDeploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        String appId = CapedwarfDeploymentMarker.getAppId(phaseContext.getDeploymentUnit());
        ServiceTarget target = phaseContext.getServiceTarget();
        ServiceName name = ServiceName.JBOSS.append(CAPEDWARF).append("mux-gen").append(appId);
        MuxIdService service = new MuxIdService(appId);
        ServiceBuilder<Void> builder = target.addService(name, service);
        ServiceName cacheName = CacheService.getServiceName(CAPEDWARF, "dist");
        builder.addDependency(cacheName, Cache.class, service.getCacheInjectedValue());
        ServiceName channelName = ChannelService.getServiceName(CAPEDWARF);
        builder.addDependency(channelName, Channel.class, service.getChannelInjectedValue());
        ServiceName tmName = TxnServices.JBOSS_TXN_TRANSACTION_MANAGER;
        builder.addDependency(tmName, TransactionManager.class, service.getTmInjectedValue());
        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }
}
