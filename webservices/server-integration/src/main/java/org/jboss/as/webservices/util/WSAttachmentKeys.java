/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.util;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.webservices.deployers.WSEndpointConfigMapping;
import org.jboss.as.webservices.injection.WSEndpointHandlersMapping;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.as.webservices.webserviceref.WSRefRegistry;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.invocation.RejectionRule;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointsMetaData;
import org.jboss.wsf.spi.metadata.webservices.JBossWebservicesMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * Collection of attachment keys
 *
 * @author alessio.soldano@jboss.com
 * @since 11-Nov-2010
 */
public final class WSAttachmentKeys {

    public static final AttachmentKey<Deployment> DEPLOYMENT_KEY = AttachmentKey.create(Deployment.class);
    public static final AttachmentKey<JBossAppMetaData> JBOSS_APP_METADATA_KEY = AttachmentKey.create(JBossAppMetaData.class);
    public static final AttachmentKey<JMSEndpointsMetaData> JMS_ENDPOINT_METADATA_KEY = AttachmentKey.create(JMSEndpointsMetaData.class);
    public static final AttachmentKey<JAXWSDeployment> JAXWS_ENDPOINTS_KEY = AttachmentKey.create(JAXWSDeployment.class);
    public static final AttachmentKey<WebservicesMetaData> WEBSERVICES_METADATA_KEY = AttachmentKey.create(WebservicesMetaData.class);
    public static final AttachmentKey<JBossWebservicesMetaData> JBOSS_WEBSERVICES_METADATA_KEY = AttachmentKey.create(JBossWebservicesMetaData.class);
    public static final AttachmentKey<JBossWebMetaData> JBOSSWEB_METADATA_KEY = AttachmentKey.create(JBossWebMetaData.class);
    public static final AttachmentKey<ClassLoader> CLASSLOADER_KEY = AttachmentKey.create(ClassLoader.class);
    public static final AttachmentKey<WSRefRegistry> WS_REFREGISTRY = AttachmentKey.create(WSRefRegistry.class);
    public static final AttachmentKey<WSEndpointHandlersMapping> WS_ENDPOINT_HANDLERS_MAPPING_KEY = AttachmentKey.create(WSEndpointHandlersMapping.class);
    public static final AttachmentKey<WSEndpointConfigMapping> WS_ENDPOINT_CONFIG_MAPPING_KEY = AttachmentKey.create(WSEndpointConfigMapping.class);
    public static final AttachmentKey<ServerConfig> SERVER_CONFIG_KEY = AttachmentKey.create(ServerConfig.class);
    public static final AttachmentKey<RejectionRule> REJECTION_RULE_KEY = AttachmentKey.create(RejectionRule.class);

    private WSAttachmentKeys() {
        // forbidden inheritance
    }

}
