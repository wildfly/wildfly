/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jpa.processor;

import org.jboss.as.jpa.beanmanager.BeanManagerAfterDeploymentValidation;
import org.jboss.as.jpa.beanmanager.PersistenceCdiExtension;
import org.jboss.as.jpa.config.JPADeploymentSettings;
import org.jboss.as.jpa.config.PersistenceProviderDeploymentHolder;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.msc.service.ServiceName;

import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * @author Stuart Douglas
 * @author thomas.diesler@jboss.com
 */
public final class JpaAttachments {

    public static final AttachmentKey<String> ADAPTOR_CLASS_NAME = AttachmentKey.create(String.class);

    public static final AttachmentKey<JPADeploymentSettings> DEPLOYMENT_SETTINGS_KEY = AttachmentKey.create(JPADeploymentSettings.class);

    public static final AttachmentKey<ServiceName> PERSISTENCE_UNIT_SERVICE_KEY = AttachmentKey.create(ServiceName.class);

    public static final AttachmentKey<Void> LOCAL_TRANSACTION_PROVIDER = AttachmentKey.create(Void.class);
    public static final AttachmentKey<TransactionSynchronizationRegistry> TRANSACTION_SYNCHRONIZATION_REGISTRY= AttachmentKey.create(TransactionSynchronizationRegistry.class);

    /**
     * List<PersistenceUnitMetadataImpl> that represents the Jakarta Persistence persistent units
     */
    public static final AttachmentKey<PersistenceProviderDeploymentHolder> DEPLOYED_PERSISTENCE_PROVIDER = AttachmentKey.create(PersistenceProviderDeploymentHolder.class);

    public static final AttachmentKey<BeanManagerAfterDeploymentValidation> BEAN_MANAGER_AFTER_DEPLOYMENT_VALIDATION_ATTACHMENT_KEY = AttachmentKey.create(BeanManagerAfterDeploymentValidation.class);

    public static final AttachmentKey<PersistenceCdiExtension> AFTER_BEAN_DISCOVERY_ATTACHMENT_KEY = AttachmentKey.create(PersistenceCdiExtension.class);

    public static final AttachmentKey<AttachmentList<String>> INTEGRATOR_ADAPTOR_MODULE_NAMES = AttachmentKey.createList(String.class);

    private JpaAttachments() {
    }
}
