/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsf.deployment;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.weld.WeldCapability;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.metadata.web.spec.ServletsMetaData;
import org.jboss.metadata.web.spec.WebCommonMetaData;
import org.jboss.metadata.web.spec.WebFragmentMetaData;
import org.jboss.vfs.VirtualFile;

/**
 * Determines the Jakarta Server Faces version that will be used by a deployment, and if Jakarta Server Faces should be used at all
 *
 * @author Stuart Douglas
 * @author Stan Silvert
 */
public class JSFVersionProcessor implements DeploymentUnitProcessor {

    public static final String JSF_CONFIG_NAME_PARAM = "org.jboss.jbossfaces.JSF_CONFIG_NAME";
    public static final String WAR_BUNDLES_JSF_IMPL_PARAM = "org.jboss.jbossfaces.WAR_BUNDLES_JSF_IMPL";

    private static final DotName[] JSF_ANNOTATIONS = {
            DotName.createSimple("jakarta.faces.view.facelets.FaceletsResourceResolver"),
            DotName.createSimple("jakarta.faces.component.behavior.FacesBehavior"),
            DotName.createSimple("jakarta.faces.render.FacesBehaviorRenderer"),
            DotName.createSimple("jakarta.faces.component.FacesComponent"),
            DotName.createSimple("jakarta.faces.convert.FacesConverter"),
            DotName.createSimple("jakarta.faces.annotation.FacesConfig"), // Should actually be check for enabled bean, but difficult to guarantee, see SERVLET_SPEC-79
            DotName.createSimple("jakarta.faces.validator.FacesValidator"),
            DotName.createSimple("jakarta.faces.event.ListenerFor"),
            DotName.createSimple("jakarta.faces.event.ListenersFor"),
            DotName.createSimple("jakarta.faces.bean.ManagedBean"),
            DotName.createSimple("jakarta.faces.event.NamedEvent"),
            DotName.createSimple("jakarta.faces.application.ResourceDependencies"),
            DotName.createSimple("jakarta.faces.application.ResourceDependency"),
            DotName.createSimple("jakarta.faces.annotation.ManagedProperty")};


    private static final DotName[] JSF_INTERFACES = {
            DotName.createSimple("jakarta.faces.convert.Converter"),
            DotName.createSimple("jakarta.faces.event.PhaseListener"),
            DotName.createSimple("jakarta.faces.render.Renderer"),
            DotName.createSimple("jakarta.faces.component.UIComponent"),
            DotName.createSimple("jakarta.faces.validator.Validator")};

    private static final String META_INF_FACES = "META-INF/faces-config.xml";
    private static final String WEB_INF_FACES = "WEB-INF/faces-config.xml";
    private static final String JAVAX_FACES_WEBAPP_FACES_SERVLET = "jakarta.faces.webapp.FacesServlet";
    private static final String CONFIG_FILES = "jakarta.faces.application.CONFIG_FILES";


    /**
     * Create the Jakarta Server Faces VersionProcessor and set the default Jakarta Server Faces implementation slot.
     *
     * @param jsfSlot The model for the Jakarta Server Faces subsystem.
     */
    public JSFVersionProcessor(String jsfSlot) {
        JSFModuleIdFactory.getInstance().setDefaultSlot(jsfSlot);
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit topLevelDeployment = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final WarMetaData metaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);

        if (!shouldJsfActivate(deploymentUnit, metaData)) {
            JsfVersionMarker.setVersion(deploymentUnit, JsfVersionMarker.NONE);
            return;
        }

        try {
            // As per section 5.6 of the spec, mark deployment as requiring CDI.
            deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT)
                    .getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class)
                    .markAsWeldDeployment(deploymentUnit);
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw new DeploymentUnitProcessingException(e);
        }

        if (metaData == null) {
            return;
        }

        List<ParamValueMetaData> contextParams = new ArrayList<ParamValueMetaData>();

        if ((metaData.getWebMetaData() != null) && (metaData.getWebMetaData().getContextParams() != null)) {
            contextParams.addAll(metaData.getWebMetaData().getContextParams());
        }

        if (metaData.getWebFragmentsMetaData() != null) {
            for (WebFragmentMetaData fragmentMetaData : metaData.getWebFragmentsMetaData().values()) {
                if (fragmentMetaData.getContextParams() != null) {
                    contextParams.addAll(fragmentMetaData.getContextParams());
                }
            }
        }

        //we need to set the Jakarta Server Faces version for the whole deployment
        //as otherwise linkage errors can occur
        //if the user does have an ear with two wars with two different
        //Jakarta Server Faces versions they are going to need to use deployment descriptors
        //to manually sort out the dependencies
        for (final ParamValueMetaData param : contextParams) {
            if ((param.getParamName().equals(WAR_BUNDLES_JSF_IMPL_PARAM) &&
                    (param.getParamValue() != null) &&
                    (param.getParamValue().toLowerCase(Locale.ENGLISH).equals("true")))) {
                JsfVersionMarker.setVersion(topLevelDeployment, JsfVersionMarker.WAR_BUNDLES_JSF_IMPL);
                break; // WAR_BUNDLES_JSF_IMPL always wins
            }

            if (param.getParamName().equals(JSF_CONFIG_NAME_PARAM)) {
                JsfVersionMarker.setVersion(topLevelDeployment, param.getParamValue());
                break;
            }
        }
    }


    private boolean shouldJsfActivate(final DeploymentUnit deploymentUnit, WarMetaData warMetaData) {
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return false;
        }
        if (warMetaData != null) {
            WebCommonMetaData jBossWebMetaData = warMetaData.getWebMetaData();
            if (isJsfDeclarationsPresent(jBossWebMetaData)) {
                return true;
            }
            if (warMetaData.getWebFragmentsMetaData() != null) {
                for (WebFragmentMetaData fragmentMetaData : warMetaData.getWebFragmentsMetaData().values()) {
                    if(isJsfDeclarationsPresent(fragmentMetaData)) {
                        return true;
                    }
                }
            }
        }
        Set<ResourceRoot> roots = new HashSet<>();
        roots.add(deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT));
        roots.addAll(deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS));
        for (ResourceRoot root : roots) {
            VirtualFile c = root.getRoot().getChild(META_INF_FACES);
            if (c.exists()) {
                return true;
            }
            c = root.getRoot().getChild(WEB_INF_FACES);
            if (c.exists()) {
                return true;
            }
        }

        CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        for (DotName annotation : JSF_ANNOTATIONS) {
            List<AnnotationInstance> annotations = index.getAnnotations(annotation);
            if (!annotations.isEmpty()) {
                return true;
            }
        }
        for (DotName annotation : JSF_INTERFACES) {
            Set<ClassInfo> implementors = index.getAllKnownImplementors(annotation);
            if (!implementors.isEmpty()) {
                return true;
            }
        }
        return false;

    }

    private boolean isJsfDeclarationsPresent(WebCommonMetaData jBossWebMetaData) {
        if (jBossWebMetaData != null) {
            ServletsMetaData servlets = jBossWebMetaData.getServlets();
            if (servlets != null) {
                for (ServletMetaData servlet : servlets) {
                    if (JAVAX_FACES_WEBAPP_FACES_SERVLET.equals(servlet.getServletClass())) {
                        return true;
                    }
                }
            }
            List<ParamValueMetaData> sc = jBossWebMetaData.getContextParams();
            if (sc != null) {
                for (ParamValueMetaData p : sc) {
                    if (CONFIG_FILES.equals(p.getParamName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
