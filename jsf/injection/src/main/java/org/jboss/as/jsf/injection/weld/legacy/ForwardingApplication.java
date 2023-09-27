/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsf.injection.weld.legacy;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import jakarta.el.ELContextListener;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.ValueExpression;
import jakarta.faces.FacesException;
import jakarta.faces.application.Application;
import jakarta.faces.application.NavigationHandler;
import jakarta.faces.application.ProjectStage;
import jakarta.faces.application.Resource;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.application.StateManager;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.behavior.Behavior;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.event.ActionListener;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;
import jakarta.faces.flow.FlowHandler;
import jakarta.faces.validator.Validator;

/**
 * @author pmuir
 *
 * Bring this class back to allow Faces 1.2 to be used with WildFly
 * See https://issues.jboss.org/browse/WFLY-9708
 */
@SuppressWarnings({"deprecation"})
public abstract class ForwardingApplication extends Application {

    protected abstract Application delegate();

    @Override
    public void addBehavior(String behaviorId, String behaviorClass) {
        delegate().addBehavior(behaviorId, behaviorClass);
    }

    @Override
    public void addComponent(String componentType, String componentClass) {
        delegate().addComponent(componentType, componentClass);
    }

    @Override
    public void addConverter(String converterId, String converterClass) {
        delegate().addConverter(converterId, converterClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addConverter(Class targetClass, String converterClass) {
        delegate().addConverter(targetClass, converterClass);
    }

    @Override
    public void addDefaultValidatorId(String validatorId) {
        delegate().addDefaultValidatorId(validatorId);
    }

    @Override
    public void addELContextListener(ELContextListener listener) {
        delegate().addELContextListener(listener);
    }

    @Override
    public void addELResolver(ELResolver resolver) {
        delegate().addELResolver(resolver);
    }

    @Override
    public void addValidator(String validatorId, String validatorClass) {
        delegate().addValidator(validatorId, validatorClass);
    }

    @Override
    public Behavior createBehavior(String behaviorId) throws FacesException {
        return delegate().createBehavior(behaviorId);
    }

    @Override
    public UIComponent createComponent(FacesContext context, Resource componentResource) {
        return delegate().createComponent(context, componentResource);
    }

    @Override
    public UIComponent createComponent(FacesContext context, String componentType, String rendererType) {
        return delegate().createComponent(context, componentType, rendererType);
    }

    @Override
    public UIComponent createComponent(ValueExpression componentExpression, FacesContext context, String componentType)
            throws FacesException {
        return delegate().createComponent(componentExpression, context, componentType);
    }

    @Override
    public UIComponent createComponent(ValueExpression componentExpression, FacesContext context, String componentType,
            String rendererType) {
        return delegate().createComponent(componentExpression, context, componentType, rendererType);
    }

    @Override
    public UIComponent createComponent(String componentType) throws FacesException {
        return delegate().createComponent(componentType);
    }

    @Override
    public Converter createConverter(String converterId) {
        return delegate().createConverter(converterId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Converter createConverter(Class targetClass) {
        return delegate().createConverter(targetClass);
    }


    @Override
    public Validator createValidator(String validatorId) throws FacesException {
        return delegate().createValidator(validatorId);
    }

    @Override
    public <T> T evaluateExpressionGet(FacesContext context, String expression, Class<? extends T> expectedType)
            throws ELException {
        return delegate().evaluateExpressionGet(context, expression, expectedType);
    }

    @Override
    public ActionListener getActionListener() {
        return delegate().getActionListener();
    }

    @Override
    public Iterator<String> getBehaviorIds() {
        return delegate().getBehaviorIds();
    }

    @Override
    public Iterator<String> getComponentTypes() {
        return delegate().getComponentTypes();
    }

    @Override
    public Iterator<String> getConverterIds() {
        return delegate().getConverterIds();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Class<?>> getConverterTypes() {
        return delegate().getConverterTypes();
    }

    @Override
    public Locale getDefaultLocale() {
        return delegate().getDefaultLocale();
    }

    @Override
    public String getDefaultRenderKitId() {
        return delegate().getDefaultRenderKitId();
    }

    @Override
    public Map<String, String> getDefaultValidatorInfo() {
        return delegate().getDefaultValidatorInfo();
    }

    @Override
    public ELContextListener[] getELContextListeners() {
        return delegate().getELContextListeners();
    }

    @Override
    public ELResolver getELResolver() {
        return delegate().getELResolver();
    }

    @Override
    public ExpressionFactory getExpressionFactory() {
        return delegate().getExpressionFactory();
    }

    @Override
    public FlowHandler getFlowHandler() {
        return delegate().getFlowHandler();
    }

    @Override
    public String getMessageBundle() {
        return delegate().getMessageBundle();
    }

    @Override
    public NavigationHandler getNavigationHandler() {
        return delegate().getNavigationHandler();
    }

    @Override
    public ProjectStage getProjectStage() {
        return delegate().getProjectStage();
    }

    @Override
    public ResourceBundle getResourceBundle(FacesContext ctx, String name) {
        return delegate().getResourceBundle(ctx, name);
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return delegate().getResourceHandler();
    }

    @Override
    public StateManager getStateManager() {
        return delegate().getStateManager();
    }

    @Override
    public Iterator<Locale> getSupportedLocales() {
        return delegate().getSupportedLocales();
    }

    @Override
    public Iterator<String> getValidatorIds() {
        return delegate().getValidatorIds();
    }

    @Override
    public ViewHandler getViewHandler() {
        return delegate().getViewHandler();
    }

    @Override
    public void publishEvent(FacesContext context, Class<? extends SystemEvent> systemEventClass, Class<?> sourceBaseType,
            Object source) {
        delegate().publishEvent(context, systemEventClass, sourceBaseType, source);
    }

    @Override
    public void publishEvent(FacesContext context, Class<? extends SystemEvent> systemEventClass, Object source) {
        delegate().publishEvent(context, systemEventClass, source);
    }

    @Override
    public void removeELContextListener(ELContextListener listener) {
        delegate().removeELContextListener(listener);
    }

    @Override
    public void setActionListener(ActionListener listener) {
        delegate().setActionListener(listener);
    }

    @Override
    public void setDefaultLocale(Locale locale) {
        delegate().setDefaultLocale(locale);
    }

    @Override
    public void setDefaultRenderKitId(String renderKitId) {
        delegate().setDefaultRenderKitId(renderKitId);
    }

    @Override
    public void setFlowHandler(FlowHandler newHandler) {
        delegate().setFlowHandler(newHandler);
    }

    @Override
    public void setMessageBundle(String bundle) {
        delegate().setMessageBundle(bundle);
    }

    @Override
    public void setNavigationHandler(NavigationHandler handler) {
        delegate().setNavigationHandler(handler);
    }

    @Override
    public void setResourceHandler(ResourceHandler resourceHandler) {
        delegate().setResourceHandler(resourceHandler);
    }

    @Override
    public void setStateManager(StateManager manager) {
        delegate().setStateManager(manager);
    }

    @Override
    public void setSupportedLocales(Collection<Locale> locales) {
        delegate().setSupportedLocales(locales);

    }

    @Override
    public void setViewHandler(ViewHandler handler) {
        delegate().setViewHandler(handler);
    }

    @Override
    public void subscribeToEvent(Class<? extends SystemEvent> systemEventClass, Class<?> sourceClass,
            SystemEventListener listener) {
        delegate().subscribeToEvent(systemEventClass, sourceClass, listener);
    }

    @Override
    public void subscribeToEvent(Class<? extends SystemEvent> systemEventClass, SystemEventListener listener) {
        delegate().subscribeToEvent(systemEventClass, listener);
    }

    @Override
    public void unsubscribeFromEvent(Class<? extends SystemEvent> systemEventClass, Class<?> sourceClass,
            SystemEventListener listener) {
        delegate().unsubscribeFromEvent(systemEventClass, sourceClass, listener);
    }

    @Override
    public void unsubscribeFromEvent(Class<? extends SystemEvent> systemEventClass, SystemEventListener listener) {
        delegate().unsubscribeFromEvent(systemEventClass, listener);
    }

    @Override
    public boolean equals(Object obj) {
        return delegate().equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public String toString() {
        return delegate().toString();
    }

}
