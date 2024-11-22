/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
public class JaxrsServerConfig {

    private ModelNode jaxrs20RequestMatching;
    private ModelNode resteasyAddCharset;
    private ModelNode resteasyBufferExceptionEntity;
    private ModelNode resteasyDisableHtmlSanitizer;
    private ModelNode resteasyDisableProviders;
    private ModelNode resteasyDocumentExpandEntityReferences;
    private ModelNode resteasyDocumentSecureProcessingFeature;
    private ModelNode resteasyGzipMaxInput;
    private ModelNode resteasyJndiResources;
    private ModelNode resteasyLanguageMappings;
    private ModelNode resteasyMediaTypeMappings;
    private ModelNode resteasyMediaTypeParamMapping;
    private ModelNode resteasyPatchFilterDisabled;
    private ModelNode resteasyPreferJacksonOverJsonB;
    private ModelNode resteasyProviders;
    private ModelNode resteasyRFC7232Preconditions;
    private ModelNode resteasyRoleBasedSecurity;
    private ModelNode resteasySecureDisableDTDs;
    private ModelNode resteasySecureRandomMaxUse;
    private ModelNode resteasyUseBuiltinProviders;
    private ModelNode resteasyUseContainerFormParams;
    private ModelNode resteasyWiderRequestMatching;

    private final Map<String, String> contextParameters;

    public JaxrsServerConfig() {
        contextParameters = new LinkedHashMap<>();
    }

    /**
     * Adds the value to the context parameters.
     *
     * @param name  the name of the context parameter
     * @param value the value for the context parameter
     *
     * @return this configuration
     */
    protected JaxrsServerConfig putContextParameter(final String name, final String value) {
        contextParameters.put(name, value);
        return this;
    }

    /**
     * Returns a copy of the context parameters.
     *
     * @return the context parameters
     */
    public Map<String, String> getContextParameters() {
        return Map.copyOf(contextParameters);
    }

    public ModelNode isJaxrs20RequestMatching() {
        return jaxrs20RequestMatching;
    }
    public void setJaxrs20RequestMatching(ModelNode jaxrs20RequestMatching) {
        this.jaxrs20RequestMatching = jaxrs20RequestMatching;
    }
    public ModelNode isResteasyAddCharset() {
        return resteasyAddCharset;
    }
    public void setResteasyAddCharset(ModelNode resteasyAddCharset) {
        this.resteasyAddCharset = resteasyAddCharset;
    }
    public ModelNode isResteasyBufferExceptionEntity() {
        return resteasyBufferExceptionEntity;
    }
    public void setResteasyBufferExceptionEntity(ModelNode resteasyBufferExceptionEntity) {
        this.resteasyBufferExceptionEntity = resteasyBufferExceptionEntity;
    }
    public ModelNode isResteasyDisableHtmlSanitizer() {
        return resteasyDisableHtmlSanitizer;
    }
    public void setResteasyDisableHtmlSanitizer(ModelNode resteasyDisableHtmlSanitizer) {
        this.resteasyDisableHtmlSanitizer = resteasyDisableHtmlSanitizer;
    }
    public ModelNode getResteasyDisableProviders() {
        return resteasyDisableProviders;
    }
    public void setResteasyDisableProviders(ModelNode resteasyDisableProviders) {
        this.resteasyDisableProviders = resteasyDisableProviders;
    }
    @Deprecated
    public ModelNode isResteasyDocumentExpandEntityReferences() {
        return resteasyDocumentExpandEntityReferences;
    }
    @Deprecated
    public void setResteasyDocumentExpandEntityReferences(ModelNode resteasyDocumentExpandEntityReferences) {
        this.resteasyDocumentExpandEntityReferences = resteasyDocumentExpandEntityReferences;
    }
    public ModelNode isResteasyDocumentSecureProcessingFeature() {
        return resteasyDocumentSecureProcessingFeature;
    }
    public void setResteasyDocumentSecureProcessingFeature(ModelNode resteasyDocumentSecureProcessingFeature) {
        this.resteasyDocumentSecureProcessingFeature = resteasyDocumentSecureProcessingFeature;
    }
    public ModelNode getResteasyGzipMaxInput() {
        return resteasyGzipMaxInput;
    }
    public void setResteasyGzipMaxInput(ModelNode resteasyGzipMaxInput) {
        this.resteasyGzipMaxInput = resteasyGzipMaxInput;
    }
    public ModelNode getResteasyJndiResources() {
        return resteasyJndiResources;
    }
    public void setResteasyJndiResources(ModelNode resteasyJndiResources) {
        this.resteasyJndiResources = resteasyJndiResources;
    }
    public ModelNode getResteasyLanguageMappings() {
        return resteasyLanguageMappings;
    }
    public void setResteasyLanguageMappings(ModelNode resteasyLanguageMappings) {
        this.resteasyLanguageMappings = resteasyLanguageMappings;
    }
    public ModelNode getResteasyMediaTypeMappings() {
        return resteasyMediaTypeMappings;
    }
    public void setResteasyMediaTypeMappings(ModelNode resteasyMediaTypeMappings) {
        this.resteasyMediaTypeMappings = resteasyMediaTypeMappings;
    }
    public ModelNode getResteasyMediaTypeParamMapping() {
        return resteasyMediaTypeParamMapping;
    }
    public void setResteasyMediaTypeParamMapping(ModelNode resteasyMediaTypeParamMapping) {
        this.resteasyMediaTypeParamMapping = resteasyMediaTypeParamMapping;
    }

    public ModelNode isResteasyPatchFilterDisabled() {
        return resteasyPatchFilterDisabled;
    }
    public void setResteasyPatchFilterDisabled(ModelNode resteasyPatchFilterDisabled) {
        this.resteasyPatchFilterDisabled = resteasyPatchFilterDisabled;
    }
    public ModelNode isResteasyPreferJacksonOverJsonB() {
        return resteasyPreferJacksonOverJsonB;
    }
    public void setResteasyPreferJacksonOverJsonB(ModelNode resteasyPreferJacksonOverJsonB) {
        this.resteasyPreferJacksonOverJsonB = resteasyPreferJacksonOverJsonB;
    }
    public ModelNode getResteasyProviders() {
        return resteasyProviders;
    }
    public void setResteasyProviders(ModelNode resteasyProviders) {
        this.resteasyProviders = resteasyProviders;
    }
    public ModelNode isResteasyRFC7232Preconditions() {
        return resteasyRFC7232Preconditions;
    }
    public void setResteasyRFC7232Preconditions(ModelNode resteasyRFC7232Preconditions) {
        this.resteasyRFC7232Preconditions = resteasyRFC7232Preconditions;
    }
    public ModelNode isResteasyRoleBasedSecurity() {
        return resteasyRoleBasedSecurity;
    }
    public void setResteasyRoleBasedSecurity(ModelNode resteasyRoleBasedSecurity) {
        this.resteasyRoleBasedSecurity = resteasyRoleBasedSecurity;
    }
    @Deprecated
    public ModelNode isResteasySecureDisableDTDs() {
        return resteasySecureDisableDTDs;
    }
    @Deprecated
    public void setResteasySecureDisableDTDs(ModelNode resteasySecureDisableDTDs) {
        this.resteasySecureDisableDTDs = resteasySecureDisableDTDs;
    }
    public ModelNode getResteasySecureRandomMaxUse() {
        return resteasySecureRandomMaxUse;
    }
    public void setResteasySecureRandomMaxUse(ModelNode resteasySecureRandomMaxUse) {
        this.resteasySecureRandomMaxUse = resteasySecureRandomMaxUse;
    }
    public ModelNode isResteasyUseBuiltinProviders() {
        return resteasyUseBuiltinProviders;
    }
    public void setResteasyUseBuiltinProviders(ModelNode resteasyUseBuiltinProviders) {
        this.resteasyUseBuiltinProviders = resteasyUseBuiltinProviders;
    }
    public ModelNode isResteasyUseContainerFormParams() {
        return resteasyUseContainerFormParams;
    }
    public void setResteasyUseContainerFormParams(ModelNode resteasyUseContainerFormParams) {
        this.resteasyUseContainerFormParams = resteasyUseContainerFormParams;
    }
    public ModelNode isResteasyWiderRequestMatching() {
        return resteasyWiderRequestMatching;
    }
    public void setResteasyWiderRequestMatching(ModelNode resteasyWiderRequestMatching) {
        this.resteasyWiderRequestMatching = resteasyWiderRequestMatching;
    }
}
