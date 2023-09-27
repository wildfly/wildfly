/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.weld.discovery.AnnotationType;

/**
 * {@link AttachmentKey}s for weld attachments
 *
 * @author Stuart Douglas
 *
 */
public class WeldAttachments {

    /**
     * The {@link BeanDeploymentModule} for a deployment
     */
    public static final AttachmentKey<BeanDeploymentModule> BEAN_DEPLOYMENT_MODULE = AttachmentKey.create(BeanDeploymentModule.class);

    /**
     * top level list of all additional bean deployment modules
     */
    public static final AttachmentKey<AttachmentList<BeanDeploymentArchiveImpl>> ADDITIONAL_BEAN_DEPLOYMENT_MODULES = AttachmentKey.createList(BeanDeploymentArchiveImpl.class);

    /**
     * The {@link BeanDeploymentArchiveImpl} that corresponds to the main resource root of a deployment or sub deployment. For
     * consistency, the bean manager that corresponds to this bda is always bound to the java:comp namespace for web modules.
     */
    public static final AttachmentKey<BeanDeploymentArchiveImpl> DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE = AttachmentKey.create(BeanDeploymentArchiveImpl.class);

    /**
     * A set of bean defining annotations, as defined by the CDI specification.
     * @see CdiAnnotationProcessor
     */
    public static final AttachmentKey<AttachmentList<AnnotationType>> BEAN_DEFINING_ANNOTATIONS = AttachmentKey.createList(AnnotationType.class);

    /**
     * The {@link ResourceRoot} for WEB-INF/classes of a web archive.
     */
    public static final AttachmentKey<ResourceRoot> CLASSES_RESOURCE_ROOT = AttachmentKey.create(ResourceRoot.class);

}
