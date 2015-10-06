/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.as.insights.extension;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.insights.api.InsightsScheduler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:jkinlaw@redhat.com">Josh Kinlaw</a>
 */
public class InsightsSubsystemDefinition extends PersistentResourceDefinition {

    static final String INSIGHTS_RUNTIME_CAPABILITY_NAME = "org.jboss.as.insights";
    static final RuntimeCapability<Void> INSIGHTS_RUNTIME_CAPABILITY = RuntimeCapability.Builder
            .of(INSIGHTS_RUNTIME_CAPABILITY_NAME, true, InsightsScheduler.class)
            .build();

    protected static final SimpleAttributeDefinition SCHEDULE_INTERVAL = new SimpleAttributeDefinitionBuilder(
            InsightsExtension.SCHEDULE_INTERVAL, ModelType.INT)
            .setAllowExpression(true)
            .setXmlName(InsightsExtension.SCHEDULE_INTERVAL)
            .setMeasurementUnit(MeasurementUnit.DAYS)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode(InsightsService.DEFAULT_SCHEDULE_INTERVAL))
            .setAllowNull(true).build();

    protected static final SimpleAttributeDefinition ENABLED = new SimpleAttributeDefinitionBuilder(
            InsightsExtension.ENABLED, ModelType.BOOLEAN)
            .setAllowExpression(true).setXmlName(InsightsExtension.ENABLED)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode(false)).setAllowNull(true).build();

    protected static final SimpleAttributeDefinition RHNUID = new SimpleAttributeDefinitionBuilder(
            InsightsExtension.RHNUID, ModelType.STRING)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setAllowNull(true).build();

    protected static final SimpleAttributeDefinition RHNPW = new SimpleAttributeDefinitionBuilder(
            InsightsExtension.RHNPW, ModelType.STRING).setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setAllowNull(true).build();

    protected static final SimpleAttributeDefinition PROXYUSER = new SimpleAttributeDefinitionBuilder(
            InsightsExtension.PROXY_USER, ModelType.STRING)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setAllowNull(true).build();

    protected static final SimpleAttributeDefinition PROXYPASSWORD = new SimpleAttributeDefinitionBuilder(
            InsightsExtension.PROXY_PASSWORD, ModelType.STRING)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setAllowNull(true).build();

    protected static final SimpleAttributeDefinition PROXYPORT = new SimpleAttributeDefinitionBuilder(
            InsightsExtension.PROXY_PORT, ModelType.INT)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode(-1)).setAllowNull(true).build();

    protected static final SimpleAttributeDefinition PROXYURL = new SimpleAttributeDefinitionBuilder(
            InsightsExtension.PROXY_URL, ModelType.STRING)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setAllowNull(true).build();

    protected static final SimpleAttributeDefinition INSIGHTSENDPOINT = new SimpleAttributeDefinitionBuilder(
            InsightsExtension.INSIGHTS_ENDPOINT, ModelType.STRING)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode(InsightsService.DEFAULT_INSIGHTS_ENDPOINT))
            .setAllowNull(true).build();

    protected static final SimpleAttributeDefinition SYSTEMENDPOINT = new SimpleAttributeDefinitionBuilder(
            InsightsExtension.SYSTEM_ENDPOINT, ModelType.STRING)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode(InsightsService.DEFAULT_SYSTEM_ENDPOINT))
            .setAllowNull(true).build();

    protected static final SimpleAttributeDefinition URL = new SimpleAttributeDefinitionBuilder(
            InsightsExtension.URL, ModelType.STRING).setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode(InsightsService.DEFAULT_BASE_URL))
            .setAllowNull(true).build();

    protected static final SimpleAttributeDefinition USERAGENT = new SimpleAttributeDefinitionBuilder(
            InsightsExtension.USER_AGENT, ModelType.STRING)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode(InsightsService.DEFAULT_USER_AGENT))
            .setAllowNull(true).build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] {SCHEDULE_INTERVAL, ENABLED, RHNUID, RHNPW,
        PROXYUSER, PROXYPASSWORD, PROXYPORT, PROXYURL, INSIGHTSENDPOINT, SYSTEMENDPOINT, URL, USERAGENT};

    public static final InsightsSubsystemDefinition INSTANCE = new InsightsSubsystemDefinition();

    private InsightsSubsystemDefinition() {
        super(InsightsExtension.SUBSYSTEM_PATH, InsightsExtension.getResourceDescriptionResolver(null),
                InsightsSubsystemAdd.INSTANCE, InsightsSubsystemRemove.INSTANCE);
    }

    /**
     * {@inheritDoc} Registers an add operation handler or a remove operation
     * handler if one was provided to the constructor.
     */
    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }
}
