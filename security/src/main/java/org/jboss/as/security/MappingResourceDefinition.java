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


import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author Jason T. Greene
 */
public class MappingResourceDefinition extends SimpleResourceDefinition {

    public static final MappingResourceDefinition INSTANCE = new MappingResourceDefinition();

    //public static final ListAttributeDefinition MAPPING_MODULES = new MappingModulesAttributeDefinition();
    /*
    "attributes" => {"mapping-modules" => {
                                "type" => LIST,
                                "description" => "List of modules that map principal, role, and credential information",
                                "expressions-allowed" => false,
                                "nillable" => false,
                                "value-type" => {
                                    "code" => {
                                        "description" => "Class name of the module to be instantiated.",
                                        "type" => STRING,
                                        "nillable" => false,
                                        "min-length" => 1
                                    },
                                    "type" => {
                                        "description" => "Type of mapping this module performs. Allowed values are principal, role, attribute or credential..",
                                        "type" => STRING,
                                        "nillable" => false
                                    },
                                    "module-options" => {
                                        "description" => "List of module options containing a name/value pair.",
                                        "type" => OBJECT,
                                        "value-type" => STRING,
                                        "nillable" => true
                                    }
                                }
                            }},
                             <mapping-module code="SimpleRoles" type="role">
                 <module-option name="d" value="e"/>
              </mapping-module>
     */




    /*private static final ObjectTypeAttributeDefinition ATTRIBUTES = new ObjectTypeAttributeDefinition.Builder("attributes", CODE, TYPE, MODULE_OPTIONS).build();

    public static final ObjectListAttributeDefinition MAPPING_MODULES = new ObjectListAttributeDefinition.Builder(Constants.MAPPING_MODULES, ATTRIBUTES)
            .build();*/

    private MappingResourceDefinition() {
        super(PathElement.pathElement(Constants.MAPPING, Constants.CLASSIC),
                SecurityExtension.getResourceDescriptionResolver(Constants.MAPPING),
                LoginModuleStackResourceDefinitionAdd.INSTANCE, new SecurityDomainReloadRemoveHandler());
    }

   /* public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(MAPPING_MODULES, null, new SecurityDomainReloadWriteHandler(MAPPING_MODULES));
    }*/

    static class LoginModuleStackResourceDefinitionAdd extends SecurityDomainReloadAddHandler {
        static final LoginModuleStackResourceDefinitionAdd INSTANCE = new LoginModuleStackResourceDefinitionAdd();

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            //MAPPING_MODULES.validateAndSet(operation, model);
        }

    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new MappingModuleDefinition(Constants.MAPPING_MODULE));
    }

    public static void marshallAsElement() {

    }
}
