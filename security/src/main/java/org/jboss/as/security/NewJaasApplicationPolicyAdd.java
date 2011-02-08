/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.security.CommonAttributes.MODULE_OPTIONS;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.service.JaasConfigurationService;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.security.auth.login.AuthenticationInfo;
import org.jboss.security.config.ApplicationPolicy;
import org.jboss.security.config.ApplicationPolicyRegistration;

/**
 * Add JAAS Application Policy Operation.
 *
 * @author Brian Stansberry
 */
class NewJaasApplicationPolicyAdd implements ModelAddOperationHandler, RuntimeOperationHandler, DescriptionProvider {

    static final String OPERATION_NAME = ADD;

    static final ModelNode getRecreateOperation(ModelNode address, ModelNode appPolicy) {
        return Util.getOperation(OPERATION_NAME, address, appPolicy);
    }

    static final NewJaasApplicationPolicyAdd INSTANCE = new NewJaasApplicationPolicyAdd();

    /** Private to ensure a singleton. */
    private NewJaasApplicationPolicyAdd() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return SecuritySubsystemDescriptions.getJaasApplicationPolicyAdd(locale);
    }

    @Override
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        ModelNode opAddr = operation.require(OP_ADDR);
        PathAddress address = PathAddress.pathAddress(opAddr);
        String policyName = address.getLastElement().getValue();

        // Create the compensating operation
        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(opAddr);

        Util.copyParamsToModel(operation, context.getSubModel());

        ApplicationPolicy applicationPolicy = createApplicationPolicy(policyName, operation);

        if (context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext updateContext = (NewRuntimeOperationContext) context;
            // add parsed security domain to the Configuration
            final ApplicationPolicyRegistration loginConfig = getConfiguration(updateContext);
            loginConfig.addApplicationPolicy(applicationPolicy.getName(), applicationPolicy);
        }

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

    // FIXME -- this is a very limited hack to get a minimal policy working
    private ApplicationPolicy createApplicationPolicy(String policyName, ModelNode operation) {
        Set<String> keys = new HashSet<String>(operation.keys());
        keys.remove(OP);
        keys.remove(OP_ADDR);
        keys.remove(NAME);
        keys.remove(Element.AUTHENTICATION.getLocalName());
        if (!keys.isEmpty()) {
            throw new UnsupportedOperationException("NYI: full handling of JAAS application policies");
        }
        ApplicationPolicy applicationPolicy = new ApplicationPolicy(policyName);
        ModelNode authenticationNode = operation.get(Element.AUTHENTICATION.getLocalName());
        AuthenticationInfo authInfo = new AuthenticationInfo();
        List<ModelNode> modules = authenticationNode.asList();
        for (ModelNode module : modules) {
            String codeName = module.require(Attribute.CODE.getLocalName()).asString();
            LoginModuleControlFlag controlFlag = getControlFlag(module.require(Attribute.FLAG.getLocalName()).asString());
            Map<String, String> options = new HashMap<String,String>();
            if (module.hasDefined(MODULE_OPTIONS)) {
                for (Property prop : module.get(MODULE_OPTIONS).asPropertyList()) {
                    options.put(prop.getName(), prop.getValue().asString());
                }
            }
            authInfo.addAppConfigurationEntry(new AppConfigurationEntry(codeName, controlFlag, options));
        }
        applicationPolicy.setAuthenticationInfo(authInfo);
        return applicationPolicy;
    }

    private synchronized ApplicationPolicyRegistration getConfiguration(NewRuntimeOperationContext updateContext) {
        ServiceController<?> controller = updateContext.getServiceRegistry().getRequiredService(JaasConfigurationService.SERVICE_NAME);
        return  (ApplicationPolicyRegistration) controller.getValue();
    }

    private LoginModuleControlFlag getControlFlag(String flag) {
       if("required".equalsIgnoreCase(flag))
          return LoginModuleControlFlag.REQUIRED;
       if("sufficient".equalsIgnoreCase(flag))
          return LoginModuleControlFlag.SUFFICIENT;
       if("optional".equalsIgnoreCase(flag))
          return LoginModuleControlFlag.OPTIONAL;
       if("requisite".equalsIgnoreCase(flag))
          return LoginModuleControlFlag.REQUISITE;
       throw new RuntimeException(flag + " is not recognized");
    }
}
