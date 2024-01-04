/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.metadata.property;

import java.util.Properties;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * @author John Bailey
 */
public class Attachments {
    public static final AttachmentKey<Properties> DEPLOYMENT_PROPERTIES = AttachmentKey.create(Properties.class);

    public static final AttachmentKey<PropertyReplacer> FINAL_PROPERTY_REPLACER = AttachmentKey.create(PropertyReplacer.class);
}
