/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld;

import java.util.Set;

import org.jboss.as.weld.discovery.AnnotationType;
import org.jboss.jandex.DotName;
import org.jboss.weld.util.collections.ImmutableSet;

import jakarta.enterprise.inject.spi.BeanManager;

/**
 * Class that stores the {@link DotName}s of CDI annotations.
 *
 */
public enum CdiAnnotations {
    /**
     * jakarta.decorator.Decorator CDI annotation.
     */
    DECORATOR(Constants.JAVAX_DECORATOR, "Decorator"),
    /**
     * jakarta.decorator.Delegate CDI annotation.
     */
    DELEGATE(Constants.JAVAX_DECORATOR, "Delegate"),
    /**
     * jakarta.enterprise.context.ApplicationScoped CDI annotation.
     */
    APP_SCOPED(Constants.JAVAX_ENT_CONTEXT, "ApplicationScoped"),
    /**
     * jakarta.enterprise.context.ConversationScoped CDI annotation.
     */
    CONV_SCOPED(Constants.JAVAX_ENT_CONTEXT, "ConversationScoped"),
    /**
     * jakarta.enterprise.context.RequestScoped CDI annotation.
     */
    REQ_SCOPED(Constants.JAVAX_ENT_CONTEXT, "RequestScoped"),
    /**
     * jakarta.enterprise.context.SessionScoped CDI annotation.
     */
    SESS_SCOPED(Constants.JAVAX_ENT_CONTEXT, "SessionScoped"),
    /**
     * jakarta.enterprise.context.NormalScope CDI annotation.
     */
    NORM_SCOPE(Constants.JAVAX_ENT_CONTEXT, "NormalScope"),
    /**
     * jakarta.enterprise.context.Dependent CDI annotation.
     */
    DEPENDENT(Constants.JAVAX_ENT_CONTEXT, "Dependent"),
    /**
     * jakarta.inject.Singleton annotation.
     */
    SINGLETON(Constants.JAVAX_INJ, "Singleton"),
    /**
     * jakarta.enterprise.event.Observes CDI annotation.
     */
    OBSERVES(Constants.JAVAX_ENT_EVT, "Observes"),
    /**
     * jakarta.enterprise.inject.Alternative CDI annotation.
     */
    ALTERNATIVE(Constants.JAVAX_ENT_INJ, "Alternative"),
    /**
     * jakarta.enterprise.inject.Any CDI annotation.
     */
    ANY(Constants.JAVAX_ENT_INJ, "Any"),
    /**
     * jakarta.enterprise.inject.Default CDI annotation.
     */
    DEFAULT(Constants.JAVAX_ENT_INJ, "Default"),
    /**
     * jakarta.enterprise.inject.Disposes CDI annotation.
     */
    DISPOSES(Constants.JAVAX_ENT_INJ, "Disposes"),
    /**
     * jakarta.enterprise.inject.Model CDI annotation.
     */
    MODEL(Constants.JAVAX_ENT_INJ, "Model"),
    /**
     * jakarta.enterprise.inject.New CDI annotation.
     */
    NEW(Constants.JAVAX_ENT_INJ, "New"),
    /**
     * jakarta.enterprise.inject.Produces CDI annotation.
     */
    PRODUCES(Constants.JAVAX_ENT_INJ, "Produces"),
    /**
     * jakarta.enterprise.inject.Specializes CDI annotation.
     */
    SPECIALIZES(Constants.JAVAX_ENT_INJ, "Specializes"),
    /**
     * jakarta.enterprise.inject.Stereotype CDI annotation.
     */
    STEREOTYPE(Constants.JAVAX_ENT_INJ, "Stereotype"),
    /**
     * jakarta.enterprise.inject.Typed CDI annotation.
     */
    TYPED(Constants.JAVAX_ENT_INJ, "Typed");

    /**
     * CDI annotation name.
     */
    private final String simpleName;

    /**
     * CDI annotation fully qualified name.
     */
    private final DotName dotName;

    /**
     * Constructor.
     *
     * @param prefix qualified name part
     * @param simpleName simple class name
     */
    private CdiAnnotations(final DotName prefix, final String simpleName) {
        this.simpleName = simpleName;
        this.dotName = DotName.createComponentized(prefix, simpleName);
    }

    /**
     * this can't go on the enum itself.
     */
    private static class Constants {

        private static final String EE_NAMESPACE = BeanManager.class.getName().substring(0, BeanManager.class.getName().indexOf("."));

        /**
         * javax package.
         */
        public static final DotName JAVAX = DotName.createComponentized(null, EE_NAMESPACE);

        /**
         * jakarta.interceptor package.
        */
        public static final DotName JAVAX_INTERCEPTOR = DotName.createComponentized(JAVAX, "interceptor");

        /**
         * jakarta.decorator package.
         */
        public static final DotName JAVAX_DECORATOR = DotName.createComponentized(JAVAX, "decorator");

        /**
         * javax.enterprise package.
         */
        public static final DotName JAVAX_ENT = DotName.createComponentized(JAVAX, "enterprise");

        /**
         * jakarta.enterprise.context package.
         */
        public static final DotName JAVAX_ENT_CONTEXT = DotName.createComponentized(JAVAX_ENT, "context");

        /**
         * jakarta.enterprise.event package.
         */
        public static final DotName JAVAX_ENT_EVT = DotName.createComponentized(JAVAX_ENT, "event");

        /**
         * jakarta.enterprise.inject package.
         */
        public static final DotName JAVAX_ENT_INJ = DotName.createComponentized(JAVAX_ENT, "inject");

        /**
         * jakarta.inject package.
         */
        public static final DotName JAVAX_INJ = DotName.createComponentized(JAVAX, "inject");
    }

    /**
     * @return fully qualified name
     */
    public DotName getDotName() {
        return dotName;
    }

    /**
     * @return simple name
     */
    public String getSimpleName() {
        return simpleName;
    }

    public static final DotName SCOPE = DotName.createComponentized(Constants.JAVAX_INJ, "Scope");

    public static final Set<DotName> BUILT_IN_SCOPE_NAMES = ImmutableSet.<DotName>of(DEPENDENT.getDotName(), REQ_SCOPED.getDotName(), CONV_SCOPED.getDotName(), SESS_SCOPED.getDotName(), APP_SCOPED.getDotName(), SINGLETON.getDotName());

    public static final Set<AnnotationType> BUILT_IN_SCOPES = BUILT_IN_SCOPE_NAMES.stream().map(dotName -> new AnnotationType(dotName, true)).collect(ImmutableSet.collector());

    public static final Set<AnnotationType> BEAN_DEFINING_ANNOTATIONS = ImmutableSet.of(
            new AnnotationType(DotName.createComponentized(Constants.JAVAX_INTERCEPTOR, "Interceptor"), false), asAnnotationType(DECORATOR, false),
            asAnnotationType(DEPENDENT), asAnnotationType(REQ_SCOPED), asAnnotationType(CONV_SCOPED), asAnnotationType(SESS_SCOPED),
            asAnnotationType(APP_SCOPED));

    public static final Set<AnnotationType> BEAN_DEFINING_META_ANNOTATIONS = ImmutableSet.of(asAnnotationType(NORM_SCOPE, false),
            asAnnotationType(STEREOTYPE, false));

    private static AnnotationType asAnnotationType(CdiAnnotations annotation) {
        return new AnnotationType(annotation.getDotName(), true);
    }

    private static AnnotationType asAnnotationType(CdiAnnotations annotation, boolean inherited) {
        return new AnnotationType(annotation.getDotName(), inherited);
    }

}
