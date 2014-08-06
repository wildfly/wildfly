/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jsr77.managedobject;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.jboss.as.jsr77.logging.JSR77Logger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class J2EEDeployedObjectHandlers extends Handler {

    static final J2EEDeployedObjectHandlers INSTANCE = new J2EEDeployedObjectHandlers();

    static final String J2EE_TYPE_J2EE_APPLICATION = "J2EEApplication";
    static final String J2EE_TYPE_EJB_MODULE = "EjbModule";
    static final String J2EE_TYPE_WEB_MODULE = "WebModule";
    static final String J2EE_TYPE_RA_MODULE = "ResourceAdapterModule";
    static final String J2EE_TYPE_APP_CLIENT_MODULE = "AppClientModule";

    private J2EEDeployedObjectHandlers() {
    }

    @Override
    Set<ObjectName> queryObjectNames(final ModelReader reader, final ObjectName name, final QueryExp query) {
        //iterate deployment model to get all deployments
        final ModelNode deployments = reader.getDeploymentModel();
        if (!deployments.isDefined()) {
            return Collections.emptySet();
        }
        final Set<ObjectName> names = new HashSet<ObjectName>();
        for (ModelNode deployment : deployments.asList()) {
            //TODO This is weird - look into this again when my internet is working again
            //There seems to be an additional result + outcome level
            deployment = deployment.get(RESULT);

            final String deploymentName = deployment.get(NAME).asString();
            final String objName = createObjectName(
                    null,
                    deploymentName,
                    deployment.hasDefined(SUBSYSTEM) ? deployment.get(SUBSYSTEM).asString() : null);

            if (objName == null) {
                continue;
            }
            addMatchingObjectName(names, name, objName);

            if (deploymentName.endsWith(".ear")) {
                if (deployment.hasDefined(SUBDEPLOYMENT)) {
                    for (Property prop : deployment.get(SUBDEPLOYMENT).asPropertyList()) {
                        final ModelNode subDep = prop.getValue();
                        final String subObjName = createObjectName(
                                deploymentName,
                                prop.getName(),
                                subDep.hasDefined(SUBSYSTEM) ? subDep.get(SUBSYSTEM).asString() : null);

                        addMatchingObjectName(names, name, subObjName);
                    }
                }
            }
        }

        return names;
    }

    private void addMatchingObjectName(final Set<ObjectName> names, final ObjectName name, final String objName) {
        if (objName != null) {
            ObjectName objectName = ObjectNameBuilder.createObjectName(objName);
            if (objectName != null && (name == null || name.apply(objectName))) {
                names.add(objectName);
            }
        }
    }

    private String createObjectName(final String appName, final String deploymentName, final String subsystem) {
        String type = null;
        if (deploymentName.endsWith(".ear")) {
            type = J2EE_TYPE_J2EE_APPLICATION;
        } else if (deploymentName.endsWith(".war")) {
            type = J2EE_TYPE_WEB_MODULE;
        } else if (deploymentName.endsWith(".jar")) {
            if (deploymentName.endsWith("client.jar")) {
                //TODO - HACK to pick out client jars for the tck
                type = J2EE_TYPE_APP_CLIENT_MODULE;
            } else if (subsystem != null && subsystem.equals("ejb3")){
                type = J2EE_TYPE_EJB_MODULE;
            }
        } else if (deploymentName.endsWith(".rar")) {
            type = J2EE_TYPE_RA_MODULE;
        }

        if (type == null) {
            return null;
        }

        final ObjectNameBuilder builder = ObjectNameBuilder.createServerChild(type, deploymentName);
        if (appName != null) {
            builder.append(J2EE_TYPE_J2EE_APPLICATION, appName);
        }
        return builder.toString();
    }

    @Override
    Object getAttribute(final ModelReader reader, final ObjectName name, final String attribute) throws AttributeNotFoundException, InstanceNotFoundException {
       return findHandler(reader, name).getAttribute(reader, name, attribute);
    }

    @Override
    MBeanInfo getMBeanInfo(ModelReader reader, ObjectName name) throws InstanceNotFoundException {
        return findHandler(reader, name).getMBeanInfo(reader, name);
    }

    Handler findHandler(final ModelReader reader, final ObjectName name) throws InstanceNotFoundException {
        //TODO parse object name and iterate deployment model to get deployment
        final String j2eeType = name.getKeyProperty(Handler.J2EE_TYPE);
        final String namePart = name.getKeyProperty(Handler.NAME);
        final String appName = name.getKeyProperty(J2EE_TYPE_J2EE_APPLICATION);

        final String mainDeployment;
        final String subDeployment;
        if (appName == null) {
            mainDeployment = namePart;
            subDeployment = null;
        } else {
            mainDeployment = appName;
            subDeployment = namePart;
        }

        if (mainDeployment != null) {
            //Look for the main deployment
            ModelNode deploymentNode = null;
            ModelNode deployments = reader.getDeploymentModel();
            if (deployments.isDefined()) {
                for (ModelNode deployment : deployments.asList()) {
                    deployment = deployment.get(RESULT);


                    if (mainDeployment.equals(deployment.get(NAME).asString())) {
                        String actualDeployment = mainDeployment;
                        deploymentNode = deployment;
                        if (!deploymentNode.isDefined()) {
                            break;
                        }
                        if (subDeployment != null) {
                            actualDeployment = subDeployment;
                            deploymentNode = deploymentNode.get(SUBDEPLOYMENT, subDeployment);
                            if (!deploymentNode.isDefined()) {
                                break;
                            }
                        }

                        J2EEDeployedObjectHandler handler;
                        if (j2eeType.equals(J2EE_TYPE_J2EE_APPLICATION) && actualDeployment.endsWith(".ear")){
                            handler = J2EEApplicationHandler.INSTANCE;
                        } else if (j2eeType.equals(J2EE_TYPE_APP_CLIENT_MODULE) && actualDeployment.endsWith(".jar")) {
                            handler = AppClientModuleHandler.INSTANCE;
                        } else if (j2eeType.equals(J2EE_TYPE_EJB_MODULE) && actualDeployment.endsWith(".jar") &&
                                (deploymentNode.hasDefined(SUBSYSTEM) && deploymentNode.get(SUBSYSTEM).asString().equals("ejb3"))) {
                            handler = EJBModuleHandler.INSTANCE;
                        } else if (j2eeType.equals(J2EE_TYPE_WEB_MODULE) && actualDeployment.endsWith(".war")) {
                            handler = WebModuleHandler.INSTANCE;
                        } else {
                            break;
                        }
                       reader.setDeploymentModel(deploymentNode);
                       return handler;
                    }
                }
            }
        }
        throw JSR77Logger.ROOT_LOGGER.couldNotFindJ2eeType(j2eeType);
    }

    private static class J2EEApplicationHandler extends J2EEDeployedObjectHandler {
        private static final J2EEApplicationHandler INSTANCE = new J2EEApplicationHandler();
        static final String ATTR_MODULES = "modules";

        @Override
        Set<ObjectName> queryObjectNames(ModelReader reader, ObjectName name, QueryExp query) {
            return Collections.singleton(name);
        }

        @Override
        Object getAttribute(ModelReader reader, ObjectName name, String attribute) throws AttributeNotFoundException {
            if (attribute.equals(ATTR_MODULES)) {
                //TODO implement if TCK does not like this
                return new String[0];
            }
            return super.getAttribute(reader, name, attribute);
        }

        @Override
        Set<MBeanAttributeInfo> getAttributeInfos() {
            Set<MBeanAttributeInfo> attributes = super.getAttributeInfos();

            attributes.add(createRoMBeanAttributeInfo(ATTR_MODULES, String[].class.getName(), "The modules in this application"));

            return attributes;
        }

    }

    private abstract static class J2EEModuleHandler extends J2EEDeployedObjectHandler {
        static final String ATTR_JAVA_VMS = "javaVMs";

        @Override
        Set<ObjectName> queryObjectNames(ModelReader reader, ObjectName name, QueryExp query) {
            return Collections.singleton(name);
        }

        @Override
        Object getAttribute(ModelReader reader, ObjectName name, String attribute) throws AttributeNotFoundException {
            if (attribute.equals(ATTR_JAVA_VMS)) {
                return new String[] {JVMHandler.INSTANCE.getObjectName()};
            }
            return super.getAttribute(reader, name, attribute);
        }

        @Override
        Set<MBeanAttributeInfo> getAttributeInfos() {
            Set<MBeanAttributeInfo> attributes = super.getAttributeInfos();

            attributes.add(createRoMBeanAttributeInfo(ATTR_JAVA_VMS, String[].class.getName(), "The jvms"));

            return attributes;
        }

    }

    private static class AppClientModuleHandler extends J2EEModuleHandler {
        static final AppClientModuleHandler INSTANCE = new AppClientModuleHandler();

        @Override
        Object getAttribute(ModelReader reader, ObjectName name, String attribute) throws AttributeNotFoundException {
            return super.getAttribute(reader, name, attribute);
        }
    }

    private static class WebModuleHandler extends J2EEModuleHandler {
        static final WebModuleHandler INSTANCE = new WebModuleHandler();

        static final String ATTR_SERVLETS = "servlets";

        @Override
        Object getAttribute(ModelReader reader, ObjectName name, String attribute) throws AttributeNotFoundException {
            if (attribute.equals(ATTR_SERVLETS)) {
                return new String[0];
            }
            return super.getAttribute(reader, name, attribute);
        }

        @Override
        Set<MBeanAttributeInfo> getAttributeInfos() {
            Set<MBeanAttributeInfo> attributes = super.getAttributeInfos();

            attributes.add(createRoMBeanAttributeInfo(ATTR_SERVLETS, String[].class.getName(), "The servlets"));

            return attributes;
        }
    }


    private static class EJBModuleHandler extends J2EEModuleHandler {
        static final EJBModuleHandler INSTANCE = new EJBModuleHandler();

        static final String ATTR_EJBS = "ejbs";

        @Override
        Object getAttribute(ModelReader reader, ObjectName name, String attribute) throws AttributeNotFoundException {
            if (attribute.equals(ATTR_EJBS)) {
                return new String[0];
            }
            return super.getAttribute(reader, name, attribute);
        }

        @Override
        Set<MBeanAttributeInfo> getAttributeInfos() {
            Set<MBeanAttributeInfo> attributes = super.getAttributeInfos();

            attributes.add(createRoMBeanAttributeInfo(ATTR_EJBS, String[].class.getName(), "The ejbs"));

            return attributes;
        }

    }
}
