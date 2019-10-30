/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.deployment;

import java.util.List;
import java.util.Map;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.common.SharedTldsMetaDataBuilder;
import org.jboss.metadata.web.spec.TldMetaData;

/**
 * @author Remy Maucherat
 */
public class TldsMetaData {

    public static final AttachmentKey<TldsMetaData> ATTACHMENT_KEY = AttachmentKey.create(TldsMetaData.class);

    /**
     * Shared TLDs.
     */
    private SharedTldsMetaDataBuilder sharedTlds;

    /**
     * Webapp TLDs.
     */
    private Map<String, TldMetaData> tlds;

    public List<TldMetaData> getSharedTlds(DeploymentUnit deploymentUnit) {
        return sharedTlds.getSharedTlds(deploymentUnit);
    }

    public void setSharedTlds(SharedTldsMetaDataBuilder sharedTlds) {
        this.sharedTlds = sharedTlds;
    }

    public Map<String, TldMetaData> getTlds() {
        return tlds;
    }

    public void setTlds(Map<String, TldMetaData> tlds) {
        this.tlds = tlds;
    }

}
