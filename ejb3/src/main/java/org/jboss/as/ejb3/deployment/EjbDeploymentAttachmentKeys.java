/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment;

import org.jboss.as.ejb3.deployment.processors.EjbInjectionSource;
import org.jboss.as.ejb3.remote.EJBClientContextService;
import org.jboss.as.ejb3.security.EjbJaccConfig;
import org.jboss.as.ejb3.subsystem.deployment.InstalledComponent;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.msc.service.ServiceName;

/**
 * {@link org.jboss.as.server.deployment.DeploymentUnitProcessor} attachment keys specific to EJB3 deployment
 * unit processors
 * <p/>
 * Author: Jaikiran Pai
 */
public class EjbDeploymentAttachmentKeys {

    /**
     * Attachment key to the {@link EjbJarMetaData} attachment representing the metadata created out of the ejb-jar.xml
     * deployment descriptor
     */
    public static final AttachmentKey<EjbJarMetaData> EJB_JAR_METADATA = AttachmentKey.create(EjbJarMetaData.class);

    public static final AttachmentKey<EjbJarDescription> EJB_JAR_DESCRIPTION = AttachmentKey.create(EjbJarDescription.class);
    public static final AttachmentKey<ApplicationExceptionDescriptions> APPLICATION_EXCEPTION_DESCRIPTIONS = AttachmentKey.create(ApplicationExceptionDescriptions.class);

    public static final AttachmentKey<ApplicationExceptions> APPLICATION_EXCEPTION_DETAILS = AttachmentKey.create(ApplicationExceptions.class);

    public static final AttachmentKey<AttachmentList<EjbInjectionSource>> EJB_INJECTIONS = AttachmentKey.createList(EjbInjectionSource.class);

    public static final AttachmentKey<EJBClientContextService> EJB_CLIENT_CONTEXT_SERVICE = AttachmentKey.create(EJBClientContextService.class);
    public static final AttachmentKey<ServiceName> EJB_CLIENT_CONTEXT_SERVICE_NAME = AttachmentKey.create(ServiceName.class);
    public static final AttachmentKey<ServiceName> EJB_REMOTING_PROFILE_SERVICE_NAME = AttachmentKey.create(ServiceName.class);

    /**
     * components that have been registered with the management API
     */
    public static final AttachmentKey<AttachmentList<InstalledComponent>> MANAGED_COMPONENTS = AttachmentKey.createList(InstalledComponent.class);

    public static final AttachmentKey<AttachmentList<EjbJaccConfig>> JACC_PERMISSIONS = AttachmentKey.createList(EjbJaccConfig.class);

}

