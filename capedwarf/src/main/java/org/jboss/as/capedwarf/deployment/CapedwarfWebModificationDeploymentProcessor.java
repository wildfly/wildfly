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

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.WebMetaData;

/**
 * CapeDwarf modifying web content processor.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 */
public abstract class CapedwarfWebModificationDeploymentProcessor extends CapedwarfWebDeploymentProcessor {

    protected static enum Type {
        SPEC,
        JBOSS,
        MERGED,
        SHARED
    }

    protected void doDeploy(DeploymentUnit unit) {
        WarMetaData warMetaData = unit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null)
            return;

        WebMetaData webMetaData = warMetaData.getWebMetaData();
        if (webMetaData != null)
            doDeploy(unit, webMetaData, Type.SPEC);

        webMetaData = warMetaData.getSharedWebMetaData();
        if (webMetaData != null)
            doDeploy(unit, webMetaData, Type.SHARED);

        JBossWebMetaData jBossWebMetaData = warMetaData.getJbossWebMetaData();
        if (webMetaData != null)
            doDeploy(unit, jBossWebMetaData, Type.JBOSS);

        jBossWebMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData != null)
            doDeploy(unit, jBossWebMetaData, Type.MERGED);
    }

    protected void doDeploy(DeploymentUnit unit, WebMetaData webMetaData, Type type) {
    }

    protected void doDeploy(DeploymentUnit unit, JBossWebMetaData webMetaData, Type type) {
    }
}
