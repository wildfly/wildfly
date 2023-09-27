/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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