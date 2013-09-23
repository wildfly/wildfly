/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.AccessConstraintUtilizationRegistry;
import org.jboss.as.controller.access.management.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.as.domain.management.access.AccessAuthorizationResourceDefinition;
import org.jboss.as.domain.management.audit.AccessAuditResourceDefinition;
import org.jboss.as.domain.management.audit.EnvironmentNameReader;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition;
import org.jboss.as.domain.management.security.SecurityRealmResourceDefinition;

/**
 * A {@link org.jboss.as.controller.ResourceDefinition} for the the core management resource.
 *
 * The content of this resource is dependent on the process it is being used within i.e. standalone server, host controller or
 * domain server.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class CoreManagementResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH_ELEMENT = PathElement.pathElement(CORE_SERVICE, MANAGEMENT);

    public static void registerDomainResource(Resource parent, AccessConstraintUtilizationRegistry registry) {
        Resource coreManagement = Resource.Factory.create();
        coreManagement.registerChild(AccessAuthorizationResourceDefinition.PATH_ELEMENT,
                AccessAuthorizationResourceDefinition.createResource(registry));
        parent.registerChild(PATH_ELEMENT, coreManagement);
    }

    private final Environment environment;
    private final List<ResourceDefinition> interfaces;
    private final DelegatingConfigurableAuthorizer authorizer;
    private final ManagedAuditLogger auditLogger;
    private final PathManagerService pathManager;
    private final EnvironmentNameReader environmentReader;

    private CoreManagementResourceDefinition(final Environment environment, final DelegatingConfigurableAuthorizer authorizer,
            final ManagedAuditLogger auditLogger, final PathManagerService pathManager, final EnvironmentNameReader environmentReader,
            final List<ResourceDefinition> interfaces) {
        super(PATH_ELEMENT, DomainManagementResolver.getResolver(CORE, MANAGEMENT));
        this.environment = environment;
        this.authorizer = authorizer;
        this.interfaces = interfaces;
        this.auditLogger = auditLogger;
        this.pathManager = pathManager;
        this.environmentReader = environmentReader;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        // TODO Currently these trickle through from the host controller definition - may need to consider how this merges with the domain.xml
        resourceRegistration.registerSubModel(SecurityRealmResourceDefinition.INSTANCE);
        resourceRegistration.registerSubModel(LdapConnectionResourceDefinition.INSTANCE);

        for (ResourceDefinition current : interfaces) {
            resourceRegistration.registerSubModel(current);
        }

        boolean registerAuditLog = true;
        switch (environment) {
            case DOMAIN:
                resourceRegistration.registerSubModel(AccessAuthorizationResourceDefinition.forDomain(authorizer));
                registerAuditLog = false;
                break;
            case DOMAIN_SERVER:
                resourceRegistration.registerSubModel(AccessAuthorizationResourceDefinition.forDomainServer(authorizer));
                break;
            case HOST_CONTROLLER:
                resourceRegistration.registerSubModel(AccessAuthorizationResourceDefinition.forHost(authorizer));
                break;
            case STANDALONE_SERVER:
                resourceRegistration.registerSubModel(AccessAuthorizationResourceDefinition.forStandaloneServer(authorizer));
        }
        //resourceRegistration.registerSubModel(AccessAuditResourceDefinition.INSTANCE);
        if (registerAuditLog) {
            resourceRegistration.registerSubModel(new AccessAuditResourceDefinition(auditLogger, pathManager, environmentReader));
        }
    }

    public static SimpleResourceDefinition forDomain(final DelegatingConfigurableAuthorizer authorizer) {
        List<ResourceDefinition> interfaces = Collections.emptyList();
        return new CoreManagementResourceDefinition(Environment.DOMAIN, authorizer, null, null, null, interfaces);
    }

    public static SimpleResourceDefinition forDomainServer(final DelegatingConfigurableAuthorizer authorizer,
            final ManagedAuditLogger auditLogger, final PathManagerService pathManager, final EnvironmentNameReader environmentReader) {
        List<ResourceDefinition> interfaces = Collections.emptyList();
        return new CoreManagementResourceDefinition(Environment.DOMAIN_SERVER, authorizer, auditLogger, pathManager, environmentReader, interfaces);
    }

    public static SimpleResourceDefinition forHost(final DelegatingConfigurableAuthorizer authorizer,
            final ManagedAuditLogger auditLogger, final PathManagerService pathManager, final EnvironmentNameReader environmentReader,
            final ResourceDefinition... interfaces) {
        return new CoreManagementResourceDefinition(Environment.HOST_CONTROLLER, authorizer, auditLogger, pathManager, environmentReader, Arrays.asList(interfaces));
    }

    public static SimpleResourceDefinition forStandaloneServer(final DelegatingConfigurableAuthorizer authorizer,
            final ManagedAuditLogger auditLogger, final PathManagerService pathManager, final EnvironmentNameReader environmentReader,
            final ResourceDefinition... interfaces) {
        return new CoreManagementResourceDefinition(Environment.STANDALONE_SERVER, authorizer, auditLogger, pathManager, environmentReader, Arrays.asList(interfaces));
    }

}
