/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import java.util.Set;

import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessorRegistry;
import org.jboss.as.ee.component.deployers.MessageDestinationInjectionSource;
import org.jboss.as.ee.component.deployers.StartupCountdown;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.msc.service.ServiceName;

/**
 * @author John Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class Attachments {

    public static final AttachmentKey<EEApplicationDescription> EE_APPLICATION_DESCRIPTION = AttachmentKey.create(EEApplicationDescription.class);
    public static final AttachmentKey<EEApplicationClasses> EE_APPLICATION_CLASSES_DESCRIPTION = AttachmentKey.create(EEApplicationClasses.class);


    public static final AttachmentKey<EEModuleDescription> EE_MODULE_DESCRIPTION = AttachmentKey.create(EEModuleDescription.class);
    public static final AttachmentKey<EEModuleConfiguration> EE_MODULE_CONFIGURATION = AttachmentKey.create(EEModuleConfiguration.class);

    public static final AttachmentKey<DeploymentDescriptorEnvironment> MODULE_DEPLOYMENT_DESCRIPTOR_ENVIRONMENT = AttachmentKey.create(DeploymentDescriptorEnvironment.class);

    /**
     * Components that failed during install. This will allow some optional components to be ignored.
     */
    public static final AttachmentKey<Set<ServiceName>> FAILED_COMPONENTS = AttachmentKey.create(Set.class);

    /**
     * A list of actions that should be performed for every web invocation
     */
    public static final AttachmentKey<AttachmentList<SetupAction>> WEB_SETUP_ACTIONS = AttachmentKey.createList(SetupAction.class);

    /**
     * A list of actions that should be performed for other non-web EE threads. At the moment this is ejb timer, remote, async invocations, and the app client.
     */
    public static final AttachmentKey<AttachmentList<SetupAction>> OTHER_EE_SETUP_ACTIONS = AttachmentKey.createList(SetupAction.class);

    /**
     * Additional (remote) components that can be resolved but are not installed.
     */
    public static final AttachmentKey<AttachmentList<ComponentDescription>> ADDITIONAL_RESOLVABLE_COMPONENTS = AttachmentKey.createList(ComponentDescription.class);

    /**
     * Unlike the EE spec which says application name is the name of the top level deployment (even if it is just
     * a jar and not an ear), the Jakarta Enterprise Beans spec semantics (for JNDI) expect that the application name is the
     * .ear name (or any configured value in application.xml). Absence of the .ear is expected to mean
     * there's no application name. This attachement key, provides the application name which is follows the
     * Jakarta Enterprise Beans spec semantics.
     */
    public static final AttachmentKey<String> EAR_APPLICATION_NAME = AttachmentKey.create(String.class);


    /**
     * Any message destinations that need to be resolved.
     */
    public static final AttachmentKey<AttachmentList<MessageDestinationInjectionSource>> MESSAGE_DESTINATIONS = AttachmentKey.createList(MessageDestinationInjectionSource.class);


    public static final AttachmentKey<EEResourceReferenceProcessorRegistry> RESOURCE_REFERENCE_PROCESSOR_REGISTRY = AttachmentKey.create(EEResourceReferenceProcessorRegistry.class);

    public static final AttachmentKey<StartupCountdown> STARTUP_COUNTDOWN = AttachmentKey.create(StartupCountdown.class);
    public static final AttachmentKey<ComponentRegistry> COMPONENT_REGISTRY = AttachmentKey.create(ComponentRegistry.class);
}
