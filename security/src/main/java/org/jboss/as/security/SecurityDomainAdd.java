/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.security.CommonAttributes.MODULE_OPTIONS;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.service.JaasConfigurationService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.security.acl.config.ACLProviderEntry;
import org.jboss.security.audit.config.AuditProviderEntry;
import org.jboss.security.auth.container.config.AuthModuleEntry;
import org.jboss.security.auth.login.AuthenticationInfo;
import org.jboss.security.auth.login.JASPIAuthenticationInfo;
import org.jboss.security.auth.login.LoginModuleStackHolder;
import org.jboss.security.authorization.config.AuthorizationModuleEntry;
import org.jboss.security.config.ACLInfo;
import org.jboss.security.config.ApplicationPolicy;
import org.jboss.security.config.ApplicationPolicyRegistration;
import org.jboss.security.config.AuditInfo;
import org.jboss.security.config.AuthorizationInfo;
import org.jboss.security.config.ControlFlag;
import org.jboss.security.config.IdentityTrustInfo;
import org.jboss.security.config.MappingInfo;
import org.jboss.security.identitytrust.config.IdentityTrustModuleEntry;
import org.jboss.security.mapping.MappingType;
import org.jboss.security.mapping.config.MappingModuleEntry;

/**
 * Add a security domain configuration.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author Brian Stansberry
 */
class SecurityDomainAdd implements ModelAddOperationHandler {

    static final String OPERATION_NAME = ADD;

    static final ModelNode getRecreateOperation(ModelNode address, ModelNode securityDomain) {
        return Util.getOperation(OPERATION_NAME, address, securityDomain);
    }

    static final SecurityDomainAdd INSTANCE = new SecurityDomainAdd();

    /** Private to ensure a singleton. */
    private SecurityDomainAdd() {
    }

    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
        ModelNode opAddr = operation.require(OP_ADDR);
        PathAddress address = PathAddress.pathAddress(opAddr);
        String securityDomain = address.getLastElement().getValue();

        Util.copyParamsToModel(operation, context.getSubModel());

        final ApplicationPolicy applicationPolicy = createApplicationPolicy(securityDomain, operation);

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    // add parsed security domain to the Configuration
                    final ApplicationPolicyRegistration loginConfig = getConfiguration(context.getServiceRegistry());
                    loginConfig.addApplicationPolicy(applicationPolicy.getName(), applicationPolicy);
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        // Create the compensating operation
        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(opAddr);
        return new BasicOperationResult(compensatingOperation);
    }

    private ApplicationPolicy createApplicationPolicy(String securityDomain, ModelNode operation) {
        ApplicationPolicy applicationPolicy = new ApplicationPolicy(securityDomain);
        ModelNode node = null;
        List<ModelNode> modules;

        // authentication
        node = operation.get(Element.AUTHENTICATION.getLocalName());
        if (node.isDefined()) {
            AuthenticationInfo authenticationInfo = new AuthenticationInfo(securityDomain);
            modules = node.asList();
            for (ModelNode module : modules) {
                String codeName = module.require(Attribute.CODE.getLocalName()).asString();
                LoginModuleControlFlag controlFlag = getControlFlag(module.require(Attribute.FLAG.getLocalName()).asString());
                Map<String, Object> options = new HashMap<String, Object>();
                if (module.hasDefined(MODULE_OPTIONS)) {
                    for (Property prop : module.get(MODULE_OPTIONS).asPropertyList()) {
                        options.put(prop.getName(), prop.getValue().asString());
                    }
                }
                AppConfigurationEntry entry = new AppConfigurationEntry(codeName, controlFlag, options);
                authenticationInfo.addAppConfigurationEntry(entry);
            }
            applicationPolicy.setAuthenticationInfo(authenticationInfo);
        }

        // acl
        node = operation.get(Element.ACL.getLocalName());
        if (node.isDefined()) {
            ACLInfo aclInfo = new ACLInfo(securityDomain);
            modules = node.asList();
            for (ModelNode module : modules) {
                String codeName = module.require(Attribute.CODE.getLocalName()).asString();
                ControlFlag controlFlag = ControlFlag.valueOf(module.require(Attribute.FLAG.getLocalName()).asString());
                Map<String, Object> options = new HashMap<String, Object>();
                if (module.hasDefined(MODULE_OPTIONS)) {
                    for (Property prop : module.get(MODULE_OPTIONS).asPropertyList()) {
                        options.put(prop.getName(), prop.getValue().asString());
                    }
                }
                ACLProviderEntry entry = new ACLProviderEntry(codeName, options);
                entry.setControlFlag(controlFlag);
                aclInfo.add(entry);

            }
            applicationPolicy.setAclInfo(aclInfo);
        }

        // audit
        node = operation.get(Element.AUDIT.getLocalName());
        if (node.isDefined()) {
            AuditInfo auditInfo = new AuditInfo(securityDomain);
            modules = node.asList();
            for (ModelNode module : modules) {
                String codeName = module.require(Attribute.CODE.getLocalName()).asString();
                Map<String, Object> options = new HashMap<String, Object>();
                if (module.hasDefined(MODULE_OPTIONS)) {
                    for (Property prop : module.get(MODULE_OPTIONS).asPropertyList()) {
                        options.put(prop.getName(), prop.getValue().asString());
                    }
                }
                AuditProviderEntry entry = new AuditProviderEntry(codeName, options);
                auditInfo.add(entry);

            }
            applicationPolicy.setAuditInfo(auditInfo);
        }

        // authorization
        node = operation.get(Element.AUTHORIZATION.getLocalName());
        if (node.isDefined()) {
            AuthorizationInfo authorizationInfo = new AuthorizationInfo(securityDomain);
            modules = node.asList();
            for (ModelNode module : modules) {
                String codeName = module.require(Attribute.CODE.getLocalName()).asString();
                ControlFlag controlFlag = ControlFlag.valueOf(module.require(Attribute.FLAG.getLocalName()).asString());
                Map<String, Object> options = new HashMap<String, Object>();
                if (module.hasDefined(MODULE_OPTIONS)) {
                    for (Property prop : module.get(MODULE_OPTIONS).asPropertyList()) {
                        options.put(prop.getName(), prop.getValue().asString());
                    }
                }
                AuthorizationModuleEntry entry = new AuthorizationModuleEntry(codeName, options);
                entry.setControlFlag(controlFlag);
                authorizationInfo.add(entry);

            }
            applicationPolicy.setAuthorizationInfo(authorizationInfo);
        }

        // identity trust
        node = operation.get(Element.IDENTITY_TRUST.getLocalName());
        if (node.isDefined()) {
            IdentityTrustInfo identityTrustInfo = new IdentityTrustInfo(securityDomain);
            modules = node.asList();
            for (ModelNode module : modules) {
                String codeName = module.require(Attribute.CODE.getLocalName()).asString();
                ControlFlag controlFlag = ControlFlag.valueOf(module.require(Attribute.FLAG.getLocalName()).asString());
                Map<String, Object> options = new HashMap<String, Object>();
                if (module.hasDefined(MODULE_OPTIONS)) {
                    for (Property prop : module.get(MODULE_OPTIONS).asPropertyList()) {
                        options.put(prop.getName(), prop.getValue().asString());
                    }
                }
                IdentityTrustModuleEntry entry = new IdentityTrustModuleEntry(codeName, options);
                entry.setControlFlag(controlFlag);
                identityTrustInfo.add(entry);

            }
            applicationPolicy.setIdentityTrustInfo(identityTrustInfo);
        }

        // mapping
        node = operation.get(Element.MAPPING.getLocalName());
        if (node.isDefined()) {
            modules = node.asList();
            String mappingType = null;
            for (ModelNode module : modules) {
                MappingInfo mappingInfo = new MappingInfo(securityDomain);
                String codeName = module.require(Attribute.CODE.getLocalName()).asString();
                if (module.hasDefined(Attribute.TYPE.getLocalName()))
                    mappingType = module.get(Attribute.TYPE.getLocalName()).asString();
                else
                    mappingType = MappingType.ROLE.toString();
                Map<String, Object> options = new HashMap<String, Object>();
                if (module.hasDefined(MODULE_OPTIONS)) {
                    for (Property prop : module.get(MODULE_OPTIONS).asPropertyList()) {
                        options.put(prop.getName(), prop.getValue().asString());
                    }
                }
                MappingModuleEntry entry = new MappingModuleEntry(codeName, options, mappingType);
                mappingInfo.add(entry);
                applicationPolicy.setMappingInfo(mappingType, mappingInfo);
            }
        }

        // authentication-jaspi
        node = operation.get(Element.AUTHENTICATION_JASPI.getLocalName());
        if (node.isDefined()) {
            JASPIAuthenticationInfo authenticationInfo = new JASPIAuthenticationInfo(securityDomain);
            Map<String, LoginModuleStackHolder> holders = new HashMap<String, LoginModuleStackHolder>();
            ModelNode moduleStack = node.get(Element.LOGIN_MODULE_STACK.getLocalName());
            modules = moduleStack.asList();
            for (ModelNode loginModuleStack : modules) {
                List<ModelNode> nodes = loginModuleStack.asList();
                Iterator<ModelNode> iter = nodes.iterator();
                ModelNode nameNode = iter.next();
                String name = nameNode.get(Attribute.NAME.getLocalName()).asString();
                LoginModuleStackHolder holder = new LoginModuleStackHolder(name, null);
                holders.put(name, holder);
                authenticationInfo.add(holder);
                while (iter.hasNext()) {
                    ModelNode lmsNode = iter.next();
                    List<ModelNode> lms = lmsNode.asList();
                    for (ModelNode lmNode : lms) {
                        String code = lmNode.require(Attribute.CODE.getLocalName()).asString();
                        LoginModuleControlFlag controlFlag = getControlFlag(lmNode.require(Attribute.FLAG.getLocalName())
                                .asString());
                        Map<String, Object> options = new HashMap<String, Object>();
                        if (lmNode.hasDefined(MODULE_OPTIONS)) {
                            for (Property prop : lmNode.get(MODULE_OPTIONS).asPropertyList()) {
                                options.put(prop.getName(), prop.getValue().asString());
                            }
                        }
                        AppConfigurationEntry entry = new AppConfigurationEntry(code, controlFlag, options);
                        holder.addAppConfigurationEntry(entry);
                    }
                }
            }
            ModelNode authModuleNode = node.get(Element.AUTH_MODULE.getLocalName());
            List<ModelNode> authModules = authModuleNode.asList();
            for (ModelNode authModule : authModules) {
                String code = authModule.require(Attribute.CODE.getLocalName()).asString();
                String loginStackRef = null;
                if (authModule.hasDefined(Attribute.LOGIN_MODULE_STACK_REF.getLocalName()))
                    loginStackRef = authModule.get(Attribute.LOGIN_MODULE_STACK_REF.getLocalName()).asString();
                Map<String, Object> options = new HashMap<String, Object>();
                if (authModule.hasDefined(MODULE_OPTIONS)) {
                    for (Property prop : authModule.get(MODULE_OPTIONS).asPropertyList()) {
                        options.put(prop.getName(), prop.getValue().asString());
                    }
                }
                AuthModuleEntry entry = new AuthModuleEntry(code, options, loginStackRef);
                if (loginStackRef != null) {
                    if (!holders.containsKey(loginStackRef)) {
                        throw new IllegalArgumentException("auth-module references a login module stack that doesn't exist: "
                                + loginStackRef);
                    }
                    entry.setLoginModuleStackHolder(holders.get(loginStackRef));
                }
                authenticationInfo.add(entry);
            }
            applicationPolicy.setAuthenticationInfo(authenticationInfo);
        }

        return applicationPolicy;
    }

    private synchronized ApplicationPolicyRegistration getConfiguration(ServiceRegistry serviceRegistry) {
        ServiceController<?> controller = serviceRegistry.getRequiredService(JaasConfigurationService.SERVICE_NAME);
        return (ApplicationPolicyRegistration) controller.getValue();
    }

    private LoginModuleControlFlag getControlFlag(String flag) {
        if ("required".equalsIgnoreCase(flag))
            return LoginModuleControlFlag.REQUIRED;
        if ("sufficient".equalsIgnoreCase(flag))
            return LoginModuleControlFlag.SUFFICIENT;
        if ("optional".equalsIgnoreCase(flag))
            return LoginModuleControlFlag.OPTIONAL;
        if ("requisite".equalsIgnoreCase(flag))
            return LoginModuleControlFlag.REQUISITE;
        throw new RuntimeException(flag + " is not recognized");
    }
}
