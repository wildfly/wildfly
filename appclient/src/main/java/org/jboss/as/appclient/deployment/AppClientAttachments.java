/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.appclient.deployment;

import org.jboss.as.appclient.component.ApplicationClientComponentDescription;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.metadata.appclient.spec.ApplicationClientMetaData;

/**
 * @author Stuart Douglas
 */
public class AppClientAttachments {

    public static final AttachmentKey<Class<?>> MAIN_CLASS = AttachmentKey.create(Class.class);

    public static final AttachmentKey<Boolean> START_APP_CLIENT = AttachmentKey.create(Boolean.class);

    public static final AttachmentKey<ApplicationClientMetaData> APPLICATION_CLIENT_META_DATA = AttachmentKey.create(ApplicationClientMetaData.class);

    public static final AttachmentKey<ApplicationClientComponentDescription> APPLICATION_CLIENT_COMPONENT = AttachmentKey.create(ApplicationClientComponentDescription.class);

    private AppClientAttachments() {
    }
}
