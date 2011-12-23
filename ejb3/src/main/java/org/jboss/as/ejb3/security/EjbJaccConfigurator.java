package org.jboss.as.ejb3.security;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.security.jacc.EJBMethodPermission;
import javax.security.jacc.EJBRoleRefPermission;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.ApplicableMethodInformation;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassIndex;
import org.jboss.as.server.deployment.reflect.DeploymentClassIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;

/**
 * Component configurator the calculates JACC roles
 *
 * @author Stuart Douglas
 */
public class EjbJaccConfigurator implements ComponentConfigurator {
    @Override
    public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        final DeploymentClassIndex classIndex = deploymentUnit.getAttachment(Attachments.CLASS_INDEX);

        final EJBComponentDescription component = EJBComponentDescription.class.cast(description);

        final EjbJaccConfig config = new EjbJaccConfig();
        context.getDeploymentUnit().addToAttachmentList(EjbDeploymentAttachmentKeys.JACC_PERMISSIONS, config);

        String ejbClassName = component.getEJBClassName();
        String ejbName = component.getEJBName();
        // Process the exclude-list and method-permission
        // check class level
        boolean denyOnAllViews = true;
        boolean permitOnAllViews = true;
        List<EJBMethodPermission> permissions = new ArrayList<EJBMethodPermission>();
        List<EJBMethodPermission> uncheckedPermissions = new ArrayList<EJBMethodPermission>();
        final ApplicableMethodInformation<EJBMethodSecurityAttribute> perms = component.getDescriptorMethodPermissions();
        for (ViewDescription view : component.getViews()) {

            String viewClassName = view.getViewClassName();
            final ClassIndex viewClass;
            try {
                viewClass = classIndex.classIndex(viewClassName);
            } catch (ClassNotFoundException e) {
                throw EjbMessages.MESSAGES.failToLoadEjbViewClass(e);
            }
            MethodIntf methodIntf = ((EJBViewDescription) view).getMethodIntf();
            MethodInterfaceType type = getMethodInterfaceType(methodIntf);
            EJBMethodSecurityAttribute classLevel = perms.getClassLevelAttribute(ejbClassName);
            if (classLevel != null && !classLevel.isDenyAll()) {
                denyOnAllViews = false;
            } else {
                EJBMethodPermission p = new EJBMethodPermission(ejbName, null, type.name(), null);
                permissions.add(p);
            }
            if (classLevel != null && !classLevel.isPermitAll()) {
                permitOnAllViews = false;
            } else {
                EJBMethodPermission p = new EJBMethodPermission(ejbName, null, type.name(), null);
                uncheckedPermissions.add(p);
            }
            if (classLevel != null) {
                for (String role : classLevel.getRolesAllowed()) {
                    config.addRole(role, new EJBMethodPermission(ejbName, null, null, null));
                }
            }

            for (Method method : viewClass.getClassMethods()) {
                final MethodIdentifier identifier = MethodIdentifier.getIdentifierForMethod(method);
                EJBMethodSecurityAttribute methodLevel = component.getDescriptorMethodPermissions().getAttribute(methodIntf, method.getDeclaringClass().getName(), method.getName(), identifier.getParameterTypes());
                // check method level
                if (methodLevel == null) {
                    methodLevel = component.getAnnotationMethodPermissions().getAttribute(methodIntf, method.getDeclaringClass().getName(), method.getName(), identifier.getParameterTypes());
                    if (methodLevel == null) {
                        continue;
                    }
                }
                EJBMethodPermission p = new EJBMethodPermission(ejbName, identifier.getName(), type.name(), identifier.getParameterTypes());

                if (methodLevel.isDenyAll()) {
                    config.addDeny(p);
                }
                if (methodLevel.isPermitAll()) {
                    config.addPermit(p);
                }
                for (String role : methodLevel.getRolesAllowed()) {
                    config.addRole(role, p);
                }
            }
        }
        // if deny is on all views, we add permission with null as the interface
        if (denyOnAllViews) {
            permissions = new ArrayList<EJBMethodPermission>();
            permissions.add(new EJBMethodPermission(ejbName, null, null, null));
        }

        // add exclude-list permissions
        for (EJBMethodPermission ejbMethodPermission : permissions) {
            config.addDeny(ejbMethodPermission);
        }

        // if permit is on all views, we add permission with null as the interface
        if (permitOnAllViews) {
            uncheckedPermissions = new ArrayList<EJBMethodPermission>();
            uncheckedPermissions.add(new EJBMethodPermission(ejbName, null, null, null));
        }

        // add method-permission permissions
        for (EJBMethodPermission ejbMethodPermission : uncheckedPermissions) {
            config.addPermit(ejbMethodPermission);
        }

        // Process the security-role-ref
        Map<String, Collection<String>> securityRoles = component.getSecurityRoleLinks();
        for (Map.Entry<String, Collection<String>> entry : securityRoles.entrySet()) {
            String roleName = entry.getKey();
            for (String roleLink : entry.getValue()) {
                EJBRoleRefPermission p = new EJBRoleRefPermission(ejbName, roleName);
                config.addRole(roleLink, p);
            }
        }

        /*
        * Special handling of stateful session bean getEJBObject due how the stateful session handles acquire the
        * proxy by sending an invocation to the ejb container.
        */
        if (component instanceof SessionBeanComponentDescription) {
            SessionBeanComponentDescription session = SessionBeanComponentDescription.class.cast(component);
            if (session.isStateful()) {
                EJBMethodPermission p = new EJBMethodPermission(ejbName, "getEJBObject", "Home", null);
                config.addPermit(p);
            }
        }
    }


    protected MethodInterfaceType getMethodInterfaceType(MethodIntf viewType) {
        switch (viewType) {
            case HOME:
                return MethodInterfaceType.Home;
            case LOCAL_HOME:
                return MethodInterfaceType.LocalHome;
            case SERVICE_ENDPOINT:
                return MethodInterfaceType.ServiceEndpoint;
            case LOCAL:
                return MethodInterfaceType.Local;
            case REMOTE:
                return MethodInterfaceType.Remote;
            case TIMER:
                return MethodInterfaceType.Timer;
            case MESSAGE_ENDPOINT:
                return MethodInterfaceType.MessageEndpoint;
            default:
                return null;
        }
    }
}
