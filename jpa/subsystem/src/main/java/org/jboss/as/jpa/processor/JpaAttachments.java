/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.jpa.processor;

import org.jboss.as.jpa.beanmanager.BeanManagerAfterDeploymentValidation;
import org.jboss.as.jpa.config.JPADeploymentSettings;
import org.jboss.as.jpa.config.PersistenceProviderDeploymentHolder;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.msc.service.ServiceName;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * @author Stuart Douglas
 * @author thomas.diesler@jboss.com
 */
public final class JpaAttachments {

    public static final AttachmentKey<String> ADAPTOR_CLASS_NAME = AttachmentKey.create(String.class);

    public static final AttachmentKey<JPADeploymentSettings> DEPLOYMENT_SETTINGS_KEY = AttachmentKey.create(JPADeploymentSettings.class);

    public static final AttachmentKey<ServiceName> PERSISTENCE_UNIT_SERVICE_KEY = AttachmentKey.create(ServiceName.class);

    public static final AttachmentKey<TransactionManager> TRANSACTION_MANAGER = AttachmentKey.create(TransactionManager.class);
    public static final AttachmentKey<TransactionSynchronizationRegistry> TRANSACTION_SYNCHRONIZATION_REGISTRY= AttachmentKey.create(TransactionSynchronizationRegistry.class);

    /**
     * List<PersistenceUnitMetadataImpl> that represents the JPA persistent units
     */
    public static final AttachmentKey<PersistenceProviderDeploymentHolder> DEPLOYED_PERSISTENCE_PROVIDER = AttachmentKey.create(PersistenceProviderDeploymentHolder.class);

    public static final AttachmentKey<BeanManagerAfterDeploymentValidation> BEAN_MANAGER_AFTER_DEPLOYMENT_VALIDATION_ATTACHMENT_KEY = AttachmentKey.create(BeanManagerAfterDeploymentValidation.class);

    private JpaAttachments() {
    }
}
