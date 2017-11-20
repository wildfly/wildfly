/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld;

import java.util.Set;

import org.jboss.as.weld.discovery.AnnotationType;
import org.jboss.jandex.DotName;
import org.jboss.weld.util.collections.ImmutableSet;

/**
 * Class that stores the {@link DotName}s of CDI annotations.
 *
 */
public enum CdiAnnotations {
    /**
     * javax.decorator.Decorator CDI annotation.
     */
    DECORATOR(Constants.JAVAX_DECORATOR, "Decorator"),
    /**
     * javax.decorator.Delegate CDI annotation.
     */
    DELEGATE(Constants.JAVAX_DECORATOR, "Delegate"),
    /**
     * javax.enterprise.context.ApplicationScoped CDI annotation.
     */
    APP_SCOPED(Constants.JAVAX_ENT_CONTEXT, "ApplicationScoped"),
    /**
     * javax.enterprise.context.ConversationScoped CDI annotation.
     */
    CONV_SCOPED(Constants.JAVAX_ENT_CONTEXT, "ConversationScoped"),
    /**
     * javax.enterprise.context.RequestScoped CDI annotation.
     */
    REQ_SCOPED(Constants.JAVAX_ENT_CONTEXT, "RequestScoped"),
    /**
     * javax.enterprise.context.SessionScoped CDI annotation.
     */
    SESS_SCOPED(Constants.JAVAX_ENT_CONTEXT, "SessionScoped"),
    /**
     * javax.enterprise.context.NormalScope CDI annotation.
     */
    NORM_SCOPE(Constants.JAVAX_ENT_CONTEXT, "NormalScope"),
    /**
     * javax.enterprise.context.Dependent CDI annotation.
     */
    DEPENDENT(Constants.JAVAX_ENT_CONTEXT, "Dependent"),
    /**
     * javax.inject.Singleton annotation.
     */
    SINGLETON(Constants.JAVAX_INJ, "Singleton"),
    /**
     * javax.enterprise.event.Observes CDI annotation.
     */
    OBSERVES(Constants.JAVAX_ENT_EVT, "Observes"),
    /**
     * javax.enterprise.inject.Alternative CDI annotation.
     */
    ALTERNATIVE(Constants.JAVAX_ENT_INJ, "Alternative"),
    /**
     * javax.enterprise.inject.Any CDI annotation.
     */
    ANY(Constants.JAVAX_ENT_INJ, "Any"),
    /**
     * javax.enterprise.inject.Default CDI annotation.
     */
    DEFAULT(Constants.JAVAX_ENT_INJ, "Default"),
    /**
     * javax.enterprise.inject.Disposes CDI annotation.
     */
    DISPOSES(Constants.JAVAX_ENT_INJ, "Disposes"),
    /**
     * javax.enterprise.inject.Model CDI annotation.
     */
    MODEL(Constants.JAVAX_ENT_INJ, "Model"),
    /**
     * javax.enterprise.inject.New CDI annotation.
     */
    NEW(Constants.JAVAX_ENT_INJ, "New"),
    /**
     * javax.enterprise.inject.Produces CDI annotation.
     */
    PRODUCES(Constants.JAVAX_ENT_INJ, "Produces"),
    /**
     * javax.enterprise.inject.Specializes CDI annotation.
     */
    SPECIALIZES(Constants.JAVAX_ENT_INJ, "Specializes"),
    /**
     * javax.enterprise.inject.Stereotype CDI annotation.
     */
    STEREOTYPE(Constants.JAVAX_ENT_INJ, "Stereotype"),
    /**
     * javax.enterprise.inject.Typed CDI annotation.
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
        /**
         * javax package.
         */
        public static final DotName JAVAX = DotName.createComponentized(null, "javax");

        /**
         * javax.interceptor package.
        */
        public static final DotName JAVAX_INTERCEPTOR = DotName.createComponentized(JAVAX, "interceptor");

        /**
         * javax.decorator package.
         */
        public static final DotName JAVAX_DECORATOR = DotName.createComponentized(JAVAX, "decorator");

        /**
         * javax.enterprise package.
         */
        public static final DotName JAVAX_ENT = DotName.createComponentized(JAVAX, "enterprise");

        /**
         * javax.enterprise.context package.
         */
        public static final DotName JAVAX_ENT_CONTEXT = DotName.createComponentized(JAVAX_ENT, "context");

        /**
         * javax.enterprise.event package.
         */
        public static final DotName JAVAX_ENT_EVT = DotName.createComponentized(JAVAX_ENT, "event");

        /**
         * javax.enterprise.inject package.
         */
        public static final DotName JAVAX_ENT_INJ = DotName.createComponentized(JAVAX_ENT, "inject");

        /**
         * javax.inject package.
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
