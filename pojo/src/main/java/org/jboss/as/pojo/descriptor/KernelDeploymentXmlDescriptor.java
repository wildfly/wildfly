/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.pojo.descriptor;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;

import java.io.Serializable;
import java.util.List;

/**
 * The object representation of a legacy "jboss-beans.xml" descriptor file.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class KernelDeploymentXmlDescriptor implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final AttachmentKey<AttachmentList<KernelDeploymentXmlDescriptor>> ATTACHMENT_KEY = AttachmentKey.createList(KernelDeploymentXmlDescriptor.class);

    private List<BeanMetaDataConfig> beans;
    private ModeConfig mode;
    private int beanFactoriesCount;

    public List<BeanMetaDataConfig> getBeans() {
        return beans;
    }

    public void setBeans(List<BeanMetaDataConfig> beans) {
        this.beans = beans;
    }

    public ModeConfig getMode() {
        return mode;
    }

    public void setMode(ModeConfig mode) {
        this.mode = mode;
    }

    public int getBeanFactoriesCount() {
        return beanFactoriesCount;
    }

    public void incrementBeanFactoryCount() {
        beanFactoriesCount++;
    }
}