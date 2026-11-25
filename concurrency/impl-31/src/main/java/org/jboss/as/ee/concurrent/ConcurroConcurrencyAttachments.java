/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent;

import org.glassfish.concurro.cdi.ConcurrencyManagedCDIBeans;
import org.jboss.as.server.deployment.AttachmentKey;

/**
 * @author emartins
 */
public class ConcurroConcurrencyAttachments {
    /*
     the key to obtain the ConcurrencyManagedCDIBeans instance for the deployment
     */
    public static final AttachmentKey<ConcurrencyManagedCDIBeans> CONCURRENCY_MANAGED_CDI_BEANS = AttachmentKey.create(ConcurrencyManagedCDIBeans.class);
}
