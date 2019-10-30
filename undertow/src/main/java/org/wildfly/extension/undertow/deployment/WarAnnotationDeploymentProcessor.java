/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import org.jboss.annotation.javaee.Descriptions;
import org.jboss.annotation.javaee.DisplayNames;
import org.jboss.annotation.javaee.Icons;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.AnnotationIndexUtils;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.DescriptionImpl;
import org.jboss.metadata.javaee.spec.DescriptionsImpl;
import org.jboss.metadata.javaee.spec.DisplayNameImpl;
import org.jboss.metadata.javaee.spec.DisplayNamesImpl;
import org.jboss.metadata.javaee.spec.IconImpl;
import org.jboss.metadata.javaee.spec.IconsImpl;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.javaee.spec.RunAsMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.metadata.web.spec.AnnotationMetaData;
import org.jboss.metadata.web.spec.AnnotationsMetaData;
import org.jboss.metadata.web.spec.DispatcherType;
import org.jboss.metadata.web.spec.EmptyRoleSemanticType;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.HttpMethodConstraintMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.MultipartConfigMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.metadata.web.spec.ServletSecurityMetaData;
import org.jboss.metadata.web.spec.ServletsMetaData;
import org.jboss.metadata.web.spec.TransportGuaranteeType;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.modules.ModuleIdentifier;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * Web annotation deployment processor.
 *
 * @author Emanuel Muckenhuber
 * @author Remy Maucherat
 */
public class WarAnnotationDeploymentProcessor implements DeploymentUnitProcessor {

    private static final DotName webFilter = DotName.createSimple(WebFilter.class.getName());
    private static final DotName webListener = DotName.createSimple(WebListener.class.getName());
    private static final DotName webServlet = DotName.createSimple(WebServlet.class.getName());
    private static final DotName runAs = DotName.createSimple(RunAs.class.getName());
    private static final DotName declareRoles = DotName.createSimple(DeclareRoles.class.getName());
    private static final DotName multipartConfig = DotName.createSimple(MultipartConfig.class.getName());
    private static final DotName servletSecurity = DotName.createSimple(ServletSecurity.class.getName());

    /**
     * Process web annotations.
     */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return; // Skip non web deployments
        }

        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        assert warMetaData != null;
        Map<String, WebMetaData> annotationsMetaData = warMetaData.getAnnotationsMetaData();
        if (annotationsMetaData == null) {
            annotationsMetaData = new HashMap<String, WebMetaData>();
            warMetaData.setAnnotationsMetaData(annotationsMetaData);
        }
        Map<ResourceRoot, Index> indexes = AnnotationIndexUtils.getAnnotationIndexes(deploymentUnit);

        // Process lib/*.jar
        for (final Entry<ResourceRoot, Index> entry : indexes.entrySet()) {
            final Index jarIndex = entry.getValue();
            annotationsMetaData.put(entry.getKey().getRootName(), processAnnotations(jarIndex));
        }

        Map<ModuleIdentifier, CompositeIndex> additionalModelAnnotations = deploymentUnit.getAttachment(Attachments.ADDITIONAL_ANNOTATION_INDEXES_BY_MODULE);
        if (additionalModelAnnotations != null) {
            final List<WebMetaData> additional = new ArrayList<WebMetaData>();
            for (Entry<ModuleIdentifier, CompositeIndex> entry : additionalModelAnnotations.entrySet()) {
                for(Index index : entry.getValue().getIndexes()) {
                    additional.add(processAnnotations(index));
                }
            }
            warMetaData.setAdditionalModuleAnnotationsMetadata(additional);
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }

    /**
     * Process a single index.
     *
     * @param index the annotation index
     *
     * @throws DeploymentUnitProcessingException
     */
    protected WebMetaData processAnnotations(Index index)
    throws DeploymentUnitProcessingException {
        WebMetaData metaData = new WebMetaData();
        // @WebServlet
        final List<AnnotationInstance> webServletAnnotations = index.getAnnotations(webServlet);
        if (webServletAnnotations != null && webServletAnnotations.size() > 0) {
            ServletsMetaData servlets = new ServletsMetaData();
            List<ServletMappingMetaData> servletMappings = new ArrayList<ServletMappingMetaData>();
            for (final AnnotationInstance annotation : webServletAnnotations) {
                ServletMetaData servlet = new ServletMetaData();
                AnnotationTarget target = annotation.target();
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.invalidWebServletAnnotation(target));
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                servlet.setServletClass(classInfo.toString());
                AnnotationValue nameValue = annotation.value("name");
                if (nameValue == null || nameValue.asString().isEmpty()) {
                    servlet.setName(classInfo.toString());
                } else {
                    servlet.setName(nameValue.asString());
                }
                AnnotationValue loadOnStartup = annotation.value("loadOnStartup");
                if (loadOnStartup != null && loadOnStartup.asInt() >= 0) {
                    servlet.setLoadOnStartupInt(loadOnStartup.asInt());
                }
                AnnotationValue asyncSupported = annotation.value("asyncSupported");
                if (asyncSupported != null) {
                    servlet.setAsyncSupported(asyncSupported.asBoolean());
                }
                AnnotationValue initParamsValue = annotation.value("initParams");
                if (initParamsValue != null) {
                    AnnotationInstance[] initParamsAnnotations = initParamsValue.asNestedArray();
                    if (initParamsAnnotations != null && initParamsAnnotations.length > 0) {
                        List<ParamValueMetaData> initParams = new ArrayList<ParamValueMetaData>();
                        for (AnnotationInstance initParamsAnnotation : initParamsAnnotations) {
                            ParamValueMetaData initParam = new ParamValueMetaData();
                            AnnotationValue initParamName = initParamsAnnotation.value("name");
                            AnnotationValue initParamValue = initParamsAnnotation.value();
                            if (initParamName == null || initParamValue == null) {
                                throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.invalidWebInitParamAnnotation(target));
                            }
                            AnnotationValue initParamDescription = initParamsAnnotation.value("description");
                            initParam.setParamName(initParamName.asString());
                            initParam.setParamValue(initParamValue.asString());
                            if (initParamDescription != null) {
                                Descriptions descriptions = getDescription(initParamDescription.asString());
                                if (descriptions != null) {
                                    initParam.setDescriptions(descriptions);
                                }
                            }
                            initParams.add(initParam);
                        }
                        servlet.setInitParam(initParams);
                    }
                }
                AnnotationValue descriptionValue = annotation.value("description");
                AnnotationValue displayNameValue = annotation.value("displayName");
                AnnotationValue smallIconValue = annotation.value("smallIcon");
                AnnotationValue largeIconValue = annotation.value("largeIcon");
                DescriptionGroupMetaData descriptionGroup =
                    getDescriptionGroup((descriptionValue == null) ? "" : descriptionValue.asString(),
                            (displayNameValue == null) ? "" : displayNameValue.asString(),
                            (smallIconValue == null) ? "" : smallIconValue.asString(),
                            (largeIconValue == null) ? "" : largeIconValue.asString());
                if (descriptionGroup != null) {
                    servlet.setDescriptionGroup(descriptionGroup);
                }
                ServletMappingMetaData servletMapping = new ServletMappingMetaData();
                servletMapping.setServletName(servlet.getName());
                List<String> urlPatterns = new ArrayList<String>();
                AnnotationValue urlPatternsValue = annotation.value("urlPatterns");
                if (urlPatternsValue != null) {
                    for (String urlPattern : urlPatternsValue.asStringArray()) {
                        urlPatterns.add(urlPattern);
                    }
                }
                urlPatternsValue = annotation.value();
                if (urlPatternsValue != null) {
                    for (String urlPattern : urlPatternsValue.asStringArray()) {
                        urlPatterns.add(urlPattern);
                    }
                }
                if (urlPatterns.size() > 0) {
                    servletMapping.setUrlPatterns(urlPatterns);
                    servletMappings.add(servletMapping);
                }
                servlets.add(servlet);
            }
            metaData.setServlets(servlets);
            metaData.setServletMappings(servletMappings);
        }
        // @WebFilter
        final List<AnnotationInstance> webFilterAnnotations = index.getAnnotations(webFilter);
        if (webFilterAnnotations != null && webFilterAnnotations.size() > 0) {
            FiltersMetaData filters = new FiltersMetaData();
            List<FilterMappingMetaData> filterMappings = new ArrayList<FilterMappingMetaData>();
            for (final AnnotationInstance annotation : webFilterAnnotations) {
                FilterMetaData filter = new FilterMetaData();
                AnnotationTarget target = annotation.target();
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.invalidWebFilterAnnotation(target));
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                filter.setFilterClass(classInfo.toString());
                AnnotationValue nameValue = annotation.value("filterName");
                if (nameValue == null || nameValue.asString().isEmpty()) {
                    filter.setName(classInfo.toString());
                } else {
                    filter.setName(nameValue.asString());
                }
                AnnotationValue asyncSupported = annotation.value("asyncSupported");
                if (asyncSupported != null) {
                    filter.setAsyncSupported(asyncSupported.asBoolean());
                }
                AnnotationValue initParamsValue = annotation.value("initParams");
                if (initParamsValue != null) {
                    AnnotationInstance[] initParamsAnnotations = initParamsValue.asNestedArray();
                    if (initParamsAnnotations != null && initParamsAnnotations.length > 0) {
                        List<ParamValueMetaData> initParams = new ArrayList<ParamValueMetaData>();
                        for (AnnotationInstance initParamsAnnotation : initParamsAnnotations) {
                            ParamValueMetaData initParam = new ParamValueMetaData();
                            AnnotationValue initParamName = initParamsAnnotation.value("name");
                            AnnotationValue initParamValue = initParamsAnnotation.value();
                            if (initParamName == null || initParamValue == null) {
                                throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.invalidWebInitParamAnnotation(target));
                            }
                            AnnotationValue initParamDescription = initParamsAnnotation.value("description");
                            initParam.setParamName(initParamName.asString());
                            initParam.setParamValue(initParamValue.asString());
                            if (initParamDescription != null) {
                                Descriptions descriptions = getDescription(initParamDescription.asString());
                                if (descriptions != null) {
                                    initParam.setDescriptions(descriptions);
                                }
                            }
                            initParams.add(initParam);
                        }
                        filter.setInitParam(initParams);
                    }
                }
                AnnotationValue descriptionValue = annotation.value("description");
                AnnotationValue displayNameValue = annotation.value("displayName");
                AnnotationValue smallIconValue = annotation.value("smallIcon");
                AnnotationValue largeIconValue = annotation.value("largeIcon");
                DescriptionGroupMetaData descriptionGroup =
                    getDescriptionGroup((descriptionValue == null) ? "" : descriptionValue.asString(),
                            (displayNameValue == null) ? "" : displayNameValue.asString(),
                            (smallIconValue == null) ? "" : smallIconValue.asString(),
                            (largeIconValue == null) ? "" : largeIconValue.asString());
                if (descriptionGroup != null) {
                    filter.setDescriptionGroup(descriptionGroup);
                }
                filters.add(filter);
                FilterMappingMetaData filterMapping = new FilterMappingMetaData();
                filterMapping.setFilterName(filter.getName());
                List<String> urlPatterns = new ArrayList<String>();
                List<String> servletNames = new ArrayList<String>();
                List<DispatcherType> dispatchers = new ArrayList<DispatcherType>();
                AnnotationValue urlPatternsValue = annotation.value("urlPatterns");
                if (urlPatternsValue != null) {
                    for (String urlPattern : urlPatternsValue.asStringArray()) {
                        urlPatterns.add(urlPattern);
                    }
                }
                urlPatternsValue = annotation.value();
                if (urlPatternsValue != null) {
                    for (String urlPattern : urlPatternsValue.asStringArray()) {
                        urlPatterns.add(urlPattern);
                    }
                }
                if (urlPatterns.size() > 0) {
                    filterMapping.setUrlPatterns(urlPatterns);
                }
                AnnotationValue servletNamesValue = annotation.value("servletNames");
                if (servletNamesValue != null) {
                    for (String servletName : servletNamesValue.asStringArray()) {
                        servletNames.add(servletName);
                    }
                }
                if (servletNames.size() > 0) {
                    filterMapping.setServletNames(servletNames);
                }
                AnnotationValue dispatcherTypesValue = annotation.value("dispatcherTypes");
                if (dispatcherTypesValue != null) {
                    for (String dispatcherValue : dispatcherTypesValue.asEnumArray()) {
                        dispatchers.add(DispatcherType.valueOf(dispatcherValue));
                    }
                }
                if (dispatchers.size() > 0) {
                    filterMapping.setDispatchers(dispatchers);
                }
                if (urlPatterns.size() > 0 || servletNames.size() > 0) {
                    filterMappings.add(filterMapping);
                }
            }
            metaData.setFilters(filters);
            metaData.setFilterMappings(filterMappings);
        }
        // @WebListener
        final List<AnnotationInstance> webListenerAnnotations = index.getAnnotations(webListener);
        if (webListenerAnnotations != null && webListenerAnnotations.size() > 0) {
            List<ListenerMetaData> listeners = new ArrayList<ListenerMetaData>();
            for (final AnnotationInstance annotation : webListenerAnnotations) {
                ListenerMetaData listener = new ListenerMetaData();
                AnnotationTarget target = annotation.target();
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.invalidWebListenerAnnotation(target));
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                listener.setListenerClass(classInfo.toString());
                AnnotationValue descriptionValue = annotation.value();
                if (descriptionValue != null) {
                    DescriptionGroupMetaData descriptionGroup = getDescriptionGroup(descriptionValue.asString());
                    if (descriptionGroup != null) {
                        listener.setDescriptionGroup(descriptionGroup);
                    }
                }
                listeners.add(listener);
            }
            metaData.setListeners(listeners);
        }
        // @RunAs
        final List<AnnotationInstance> runAsAnnotations = index.getAnnotations(runAs);
        if (runAsAnnotations != null && runAsAnnotations.size() > 0) {
            AnnotationsMetaData annotations = metaData.getAnnotations();
            if (annotations == null) {
               annotations = new AnnotationsMetaData();
               metaData.setAnnotations(annotations);
            }
            for (final AnnotationInstance annotation : runAsAnnotations) {
                AnnotationTarget target = annotation.target();
                if (!(target instanceof ClassInfo)) {
                    continue;
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                AnnotationMetaData annotationMD = annotations.get(classInfo.toString());
                if (annotationMD == null) {
                    annotationMD = new AnnotationMetaData();
                    annotationMD.setClassName(classInfo.toString());
                    annotations.add(annotationMD);
                }
                if (annotation.value() == null) {
                    throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.invalidRunAsAnnotation(target));
                }
                RunAsMetaData runAs = new RunAsMetaData();
                runAs.setRoleName(annotation.value().asString());
                annotationMD.setRunAs(runAs);
            }
        }
        // @DeclareRoles
        final List<AnnotationInstance> declareRolesAnnotations = index.getAnnotations(declareRoles);
        if (declareRolesAnnotations != null && declareRolesAnnotations.size() > 0) {
            SecurityRolesMetaData securityRoles = metaData.getSecurityRoles();
            if (securityRoles == null) {
               securityRoles = new SecurityRolesMetaData();
               metaData.setSecurityRoles(securityRoles);
            }
            for (final AnnotationInstance annotation : declareRolesAnnotations) {
                if (annotation.value() == null) {
                    throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.invalidDeclareRolesAnnotation(annotation.target()));
                }
                for (String role : annotation.value().asStringArray()) {
                    SecurityRoleMetaData sr = new SecurityRoleMetaData();
                    sr.setRoleName(role);
                    securityRoles.add(sr);
                }
            }
        }
        // @MultipartConfig
        final List<AnnotationInstance> multipartConfigAnnotations = index.getAnnotations(multipartConfig);
        if (multipartConfigAnnotations != null && multipartConfigAnnotations.size() > 0) {
            AnnotationsMetaData annotations = metaData.getAnnotations();
            if (annotations == null) {
               annotations = new AnnotationsMetaData();
               metaData.setAnnotations(annotations);
            }
            for (final AnnotationInstance annotation : multipartConfigAnnotations) {
                AnnotationTarget target = annotation.target();
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.invalidMultipartConfigAnnotation(target));
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                AnnotationMetaData annotationMD = annotations.get(classInfo.toString());
                if (annotationMD == null) {
                    annotationMD = new AnnotationMetaData();
                    annotationMD.setClassName(classInfo.toString());
                    annotations.add(annotationMD);
                }
                MultipartConfigMetaData multipartConfig = new MultipartConfigMetaData();
                AnnotationValue locationValue = annotation.value("location");
                if (locationValue != null && locationValue.asString().length() > 0) {
                    multipartConfig.setLocation(locationValue.asString());
                }
                AnnotationValue maxFileSizeValue = annotation.value("maxFileSize");
                if (maxFileSizeValue != null && maxFileSizeValue.asLong() != -1L) {
                    multipartConfig.setMaxFileSize(maxFileSizeValue.asLong());
                }
                AnnotationValue maxRequestSizeValue = annotation.value("maxRequestSize");
                if (maxRequestSizeValue != null && maxRequestSizeValue.asLong() != -1L) {
                    multipartConfig.setMaxRequestSize(maxRequestSizeValue.asLong());
                }
                AnnotationValue fileSizeThresholdValue = annotation.value("fileSizeThreshold");
                if (fileSizeThresholdValue != null && fileSizeThresholdValue.asInt() != 0) {
                    multipartConfig.setFileSizeThreshold(fileSizeThresholdValue.asInt());
                }
                annotationMD.setMultipartConfig(multipartConfig);
            }
        }
        // @ServletSecurity
        final List<AnnotationInstance> servletSecurityAnnotations = index.getAnnotations(servletSecurity);
        if (servletSecurityAnnotations != null && servletSecurityAnnotations.size() > 0) {
            AnnotationsMetaData annotations = metaData.getAnnotations();
            if (annotations == null) {
               annotations = new AnnotationsMetaData();
               metaData.setAnnotations(annotations);
            }
            for (final AnnotationInstance annotation : servletSecurityAnnotations) {
                AnnotationTarget target = annotation.target();
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.invalidServletSecurityAnnotation(target));
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                AnnotationMetaData annotationMD = annotations.get(classInfo.toString());
                if (annotationMD == null) {
                    annotationMD = new AnnotationMetaData();
                    annotationMD.setClassName(classInfo.toString());
                    annotations.add(annotationMD);
                }
                ServletSecurityMetaData servletSecurity = new ServletSecurityMetaData();
                AnnotationValue httpConstraintValue = annotation.value();
                List<String> rolesAllowed = new ArrayList<String>();
                if (httpConstraintValue != null) {
                    AnnotationInstance httpConstraint = httpConstraintValue.asNested();
                    AnnotationValue httpConstraintERSValue = httpConstraint.value();
                    if (httpConstraintERSValue != null) {
                        servletSecurity.setEmptyRoleSemantic(EmptyRoleSemanticType.valueOf(httpConstraintERSValue.asEnum()));
                    }
                    AnnotationValue httpConstraintTGValue = httpConstraint.value("transportGuarantee");
                    if (httpConstraintTGValue != null) {
                        servletSecurity.setTransportGuarantee(TransportGuaranteeType.valueOf(httpConstraintTGValue.asEnum()));
                    }
                    AnnotationValue rolesAllowedValue = httpConstraint.value("rolesAllowed");
                    if (rolesAllowedValue != null) {
                        for (String role : rolesAllowedValue.asStringArray()) {
                            rolesAllowed.add(role);
                        }
                    }
                }
                servletSecurity.setRolesAllowed(rolesAllowed);
                AnnotationValue httpMethodConstraintsValue = annotation.value("httpMethodConstraints");
                if (httpMethodConstraintsValue != null) {
                    AnnotationInstance[] httpMethodConstraints = httpMethodConstraintsValue.asNestedArray();
                    if (httpMethodConstraints.length > 0) {
                        List<HttpMethodConstraintMetaData> methodConstraints = new ArrayList<HttpMethodConstraintMetaData>();
                        for (AnnotationInstance httpMethodConstraint : httpMethodConstraints) {
                            HttpMethodConstraintMetaData methodConstraint = new HttpMethodConstraintMetaData();
                            AnnotationValue httpMethodConstraintValue = httpMethodConstraint.value();
                            if (httpMethodConstraintValue != null) {
                                methodConstraint.setMethod(httpMethodConstraintValue.asString());
                            }
                            AnnotationValue httpMethodConstraintERSValue = httpMethodConstraint.value("emptyRoleSemantic");
                            if (httpMethodConstraintERSValue != null) {
                                methodConstraint.setEmptyRoleSemantic(EmptyRoleSemanticType.valueOf(httpMethodConstraintERSValue.asEnum()));
                            }
                            AnnotationValue httpMethodConstraintTGValue = httpMethodConstraint.value("transportGuarantee");
                            if (httpMethodConstraintTGValue != null) {
                                methodConstraint.setTransportGuarantee(TransportGuaranteeType.valueOf(httpMethodConstraintTGValue.asEnum()));
                            }
                            AnnotationValue rolesAllowedValue = httpMethodConstraint.value("rolesAllowed");
                            rolesAllowed = new ArrayList<String>();
                            if (rolesAllowedValue != null) {
                                for (String role : rolesAllowedValue.asStringArray()) {
                                    rolesAllowed.add(role);
                                }
                            }
                            methodConstraint.setRolesAllowed(rolesAllowed);
                            methodConstraints.add(methodConstraint);
                        }
                        servletSecurity.setHttpMethodConstraints(methodConstraints);
                    }
                }
                annotationMD.setServletSecurity(servletSecurity);
            }
        }
        return metaData;
    }

    protected Descriptions getDescription(String description) {
        DescriptionsImpl descriptions = null;
        if (description.length() > 0) {
            DescriptionImpl di = new DescriptionImpl();
            di.setDescription(description);
            descriptions = new DescriptionsImpl();
            descriptions.add(di);
        }
        return descriptions;
    }

    protected DisplayNames getDisplayName(String displayName) {
        DisplayNamesImpl displayNames = null;
        if (displayName.length() > 0) {
            DisplayNameImpl dn = new DisplayNameImpl();
            dn.setDisplayName(displayName);
            displayNames = new DisplayNamesImpl();
            displayNames.add(dn);
        }
        return displayNames;
    }

    protected Icons getIcons(String smallIcon, String largeIcon) {
        IconsImpl icons = null;
        if (smallIcon.length() > 0 || largeIcon.length() > 0) {
            IconImpl i = new IconImpl();
            i.setSmallIcon(smallIcon);
            i.setLargeIcon(largeIcon);
            icons = new IconsImpl();
            icons.add(i);
        }
        return icons;
    }

    protected DescriptionGroupMetaData getDescriptionGroup(String description) {
        DescriptionGroupMetaData dg = null;
        if (description.length() > 0) {
            dg = new DescriptionGroupMetaData();
            Descriptions descriptions = getDescription(description);
            dg.setDescriptions(descriptions);
        }
        return dg;
    }

    protected DescriptionGroupMetaData getDescriptionGroup(String description, String displayName, String smallIcon,
            String largeIcon) {
        DescriptionGroupMetaData dg = null;
        if (description.length() > 0 || displayName.length() > 0 || smallIcon.length() > 0 || largeIcon.length() > 0) {
            dg = new DescriptionGroupMetaData();
            Descriptions descriptions = getDescription(description);
            if (descriptions != null)
                dg.setDescriptions(descriptions);
            DisplayNames displayNames = getDisplayName(displayName);
            if (displayNames != null)
                dg.setDisplayNames(displayNames);
            Icons icons = getIcons(smallIcon, largeIcon);
            if (icons != null)
                dg.setIcons(icons);
        }
        return dg;
    }

}
