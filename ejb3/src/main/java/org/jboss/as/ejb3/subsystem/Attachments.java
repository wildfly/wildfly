/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;


import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.ejb.client.EJBClientInterceptor;

import java.util.List;

/**
 * EJB3 related attachments.
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 *
 */
public final class Attachments {

    public static final AttachmentKey<List<EJBClientInterceptor>> STATIC_EJB_CLIENT_INTERCEPTORS = AttachmentKey.create(List.class);


    private Attachments() {
    }
}