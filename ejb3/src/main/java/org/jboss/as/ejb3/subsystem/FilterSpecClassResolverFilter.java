/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.logging.EjbLogger.REMOTE_LOGGER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.jboss.as.controller.OperationContext;
import org.wildfly.common.Assert;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Class name filtering {@code Function<String, Boolean>} implementation that is configured by a {@code filterSpec}
 * string provided to the constructor. The function returns {@code true} if the given class name is acceptable
 * for class resolution, {@code false} otherwise. The function is meant to be used for implementation blocklists
 * or allowlists of classes that would be loaded when remote Jakarta Enterprise Beans invocations are received.
 * <p>
 * The {@code filterSpec} string is composed of one or more filter spec elements separated by the {@code ';'} char.
 * A filter spec element that begins with the {@code '!'} char is a 'rejecting element' and indicates resolution of a
 * class name should not be allowed if the rest of the element matches the class name. Otherwise the spec element
 * indicates the class name can be accepted if it matches.
 * </p>
 * <p>
 * Matching is done according to the following rules:
 * <ul>
 *     <li>If the spec element does not terminate in the {@code '*'} char the given class name must match.</li>
 *     <li>If the spec element terminates in the string {@code ".*"} the portion of the class name up to and
 *     including any final {@code '.'} char must match. Such a spec element indicates a single package in which a class
 *     must reside.</li>
 *     <li>If the spec element terminates in the string {@code ".**"} the class name must begin with the portion of the
 *     spec element before the first {@code '*'}. Such a spec element indicates a package hierarchy in which a class
 *     must reside.</li>
 *     <li>Otherwise the spec element ends in the {@code '*'} char and the class name must begin with portion
 *     spec element before the first {@code '*'}. Such a spec element indicates a general string 'starts with' match.</li>
 * </ul>
 * </>
 * <p>
 * The presence of the {@code '='} or {@code '/'} chars anywhere in the filter spec will result in an
 * {@link IllegalArgumentException} from the constructor. The presence of the {@code '*'} char in any substring
 * other than the ones described above will also result in an {@link IllegalArgumentException} from the constructor.
 * </p>
 * <p>
 * If any element in the filter spec indicates a class name should be rejected, it will be rejected. If any element
 * in the filter spec does not begin with the {@code '!'} char, then the filter will act like a allowlist, and
 * at least one non-rejecting filter spec element must match the class name for the filter to return {@code true}.
 * Rejecting elements can be used in an overall filter spec for a allowlist, for example to exclude a particular
 * class from a package that is otherwise allowlisted.
 * </p>
 *
 * @author Brian Stansberry
 */
final class FilterSpecClassResolverFilter implements Function<String, Boolean> {

    // Note -- the default filter spec represents a blocklist.
    /**
     * Value provided to {@link #FilterSpecClassResolverFilter(String)} by the default no-arg constructor.
     * Represents the default filtering rules for this library.
     */
    public static final String DEFAULT_FILTER_SPEC =
            "!org.apache.commons.collections.functors.InvokerTransformer;"
                    + "!org.apache.commons.collections.functors.InstantiateTransformer;"
                    + "!org.apache.commons.collections4.functors.InvokerTransformer;"
                    + "!org.apache.commons.collections4.functors.InstantiateTransformer;"
                    + "!org.codehaus.groovy.runtime.ConvertedClosure;"
                    + "!org.codehaus.groovy.runtime.MethodClosure;"
                    + "!org.springframework.beans.factory.ObjectFactory;"
                    + "!org.springframework.beans.factory.config.MethodInvokingFactoryBean;"
                    + "!org.springframework.jndi.support.SimpleJndiBeanFactory;"
                    + "!org.springframework.transaction.jta.JtaTransactionManager;"
                    + "!com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;"
                    + "!org.apache.xalan.xsltc.trax.TemplatesImpl;"
                    + "!bsh.XThis$Handler;"
                    + "!com.mchange.v2.c3p0.PoolBackedDataSource;"
                    + "!clojure.inspector.proxy$javax.swing.table.AbstractTableModel$ff19274a;"
                    + "!org.apache.commons.fileupload.disk.DiskFileItem;"
                    + "!org.apache.wicket.util.upload.DiskFileItem;"
                    + "!org.hibernate.engine.spi.TypedValue;"
                    + "!org.python.core.PyFunction;"
                    + "!org.mozilla.javascript.NativeJavaObject;"
                    + "!org.apache.myfaces.view.facelets.el.ValueExpressionMethodExpression;"
                    + "!javax.swing.UIDefaults;"
                    + "!javax.swing.UIDefaults$ProxyLazyValue;"
                    + "!javax.swing.UIDefaults$TextAndMnemonicHashMap;"
                    + "!sun.swing.SwingLazyValue;"
                    + "!java.rmi.registry.Registry;"
                    + "!java.rmi.server.UnicastRemoteObject;"
                    + "!sun.rmi.server.UnicastRef;"
                    + "!sun.rmi.server.UnicastRef2;"
                    + "!org.aspectj.weaver.tools.cache.SimpleCache$StoreableCachingMap;"
                    + "!org.jboss.as.connector.subsystems.datasources.WildFlyDataSource";

    private static final OperationContext.AttachmentKey<FilterSpecClassResolverFilter> ATTACHMENT_KEY =
            OperationContext.AttachmentKey.create(FilterSpecClassResolverFilter.class);

    /**
     * Gets a {@link FilterSpecClassResolverFilter#FilterSpecClassResolverFilter() default}
     * {@code FilterSpecClassResolverFilter} that can be shared amongst steps executing
     * with the same {@link OperationContext}. This is used in preference to a shared static
     * default filter in order to allow different settings to be applied during a server reload.
     *
     * @param operationContext the operation context. Cannot be {@code null}
     * @return the filter. Will not return {@code null}
     */
    static FilterSpecClassResolverFilter getFilterForOperationContext(OperationContext operationContext) {
        FilterSpecClassResolverFilter result = operationContext.getAttachment(ATTACHMENT_KEY);
        if (result == null) {
            result = new FilterSpecClassResolverFilter();
            operationContext.attach(ATTACHMENT_KEY, result);
        }
        return result;
    }

    private final String filterSpec;
    private final List<String> parsedFilterSpecs;
    private final List<Function<String, Boolean>> unmarshallingFilters;
    private final boolean allowlistUnmarshalling;

    /**
     * Creates a filter using the default rules.
     */
    FilterSpecClassResolverFilter() {
        this(getUnmarshallingFilterSpec());
    }


    private static String getUnmarshallingFilterSpec() {
        // The default blocklisting can be disabled via system property
        String disabled = WildFlySecurityManager.getPropertyPrivileged("jboss.ejb.unmarshalling.filter.disabled", null);
        if ("true".equalsIgnoreCase(disabled)) {
            return "";  // empty string disables filtering
        }
        // Allow external specification of the filter spec. This config mechanism may change incompatibly
        // in a future major or minor release. Note that JBoss Marshalling also recognizes and reacts to the
        // https://openjdk.org/jeps/290 configuration mechanisms.
        String spec = WildFlySecurityManager.getPropertyPrivileged("jboss.experimental.ejb.unmarshalling.filter.spec", null);
        if (spec != null) {
            return spec;
        }
        return DEFAULT_FILTER_SPEC;
    }

    /**
     * Create a filter using the given {@code filterSpec}.
     * @param filterSpec filter configuration as described in the class javadoc. Cannot be {@code null}
     *
     * @throws IllegalArgumentException if the form of {@code filterSpec} violates any of the rules for this class
     */
    FilterSpecClassResolverFilter(String filterSpec) {
        Assert.checkNotNullParam("filterSpec", filterSpec);
        this.filterSpec = filterSpec;
        if (filterSpec.isEmpty()) {
            parsedFilterSpecs = null;
            unmarshallingFilters = null;
            allowlistUnmarshalling = false;
        } else {

            parsedFilterSpecs = new ArrayList<>(Arrays.asList(filterSpec.split(";")));
            unmarshallingFilters = new ArrayList<>(parsedFilterSpecs.size());
            ExactMatchFilter exactMatchAllowlist = null;
            ExactMatchFilter exactMatchBlocklist = null;
            boolean allowlist = false;

            for (String spec : parsedFilterSpecs) {

                if (spec.contains("=") || spec.contains("/")) {
                    // perhaps this is an attempt to pass a JEPS 290 style limit or module name pattern; not supported
                    throw REMOTE_LOGGER.invalidFilterSpec(spec);
                }

                boolean blocklistElement = spec.startsWith("!");
                allowlist |= !blocklistElement;

                // For a blocklist element, return FALSE for a match; i.e. don't resolve
                // For a allowlist, return TRUE for a match; i.e. definitely do resolve
                // For any non-match, return null which means that check has no opinion
                final Boolean matchReturn = blocklistElement ? Boolean.FALSE : Boolean.TRUE;

                if (blocklistElement) {
                    if (spec.length() == 1) {
                        throw REMOTE_LOGGER.invalidFilterSpec(spec);
                    }
                    spec = spec.substring(1);
                }

                Function<String, Boolean> filter = null;
                int lastStar = spec.lastIndexOf('*');
                if (lastStar >= 0) {
                    if (lastStar != spec.length() - 1) {
                        // wildcards only allowed at the end
                        throw REMOTE_LOGGER.invalidFilterSpec(spec);
                    }
                    int firstStar = spec.indexOf('*');
                    if (firstStar != lastStar) {
                        if (firstStar == lastStar - 1 && spec.endsWith(".**")) {
                            if (spec.length() == 3) {
                                throw REMOTE_LOGGER.invalidFilterSpec(spec);
                            }
                            String pkg = spec.substring(0, spec.length() - 2);
                            filter = cName -> cName.startsWith(pkg) ? matchReturn : null;
                        } else {
                            // there's an extra star in some spot other than between a final '.' and '*'
                            throw REMOTE_LOGGER.invalidFilterSpec(spec);
                        }
                    } else if (spec.endsWith(".*")) {
                        if (spec.length() == 2) {
                            throw REMOTE_LOGGER.invalidFilterSpec(spec);
                        }
                        String pkg = spec.substring(0, spec.length() - 1);
                        filter = cName -> cName.startsWith(pkg) && cName.lastIndexOf('.') == pkg.length() - 1 ? matchReturn : null;
                    } else {
                        String startsWith = spec.substring(0, spec.length() - 1); // note that an empty 'startsWith' is ok; e.g. from a "*" spec to allow all
                        filter = cName -> cName.startsWith(startsWith) ? matchReturn : null;
                    }
                } else {
                    // For exact matches store them in a set and just do a single set.contains check
                    if (blocklistElement) {
                        if (exactMatchBlocklist == null) {
                            filter = exactMatchBlocklist = new ExactMatchFilter(false);
                        }
                        exactMatchBlocklist.addMatchingClass(spec);
                    } else {
                        if (exactMatchAllowlist == null) {
                            filter = exactMatchAllowlist = new ExactMatchFilter(true);
                        }
                        exactMatchAllowlist.addMatchingClass(spec);
                    }
                     if (filter == null) {
                         // An ExactMatchFilter earlier in the list would have already handled this.
                         // Just add a no-op placeholder function to keep the list of specs and functions in sync
                         filter = cName -> null;
                     }
                }
                unmarshallingFilters.add(filter);
            }
            if (allowlist) {
                // Don't force users to allowlist the classes we send. Add a allowlist spec for their package
                // TODO is this a good idea?
                final String pkg = "org.jboss.ejb.client.";
                parsedFilterSpecs.add(pkg + "*");
                unmarshallingFilters.add(cName -> cName.startsWith(pkg) && cName.lastIndexOf('.') == pkg.length() - 1 ? true : null);
            }
            assert parsedFilterSpecs.size() == unmarshallingFilters.size();
            allowlistUnmarshalling = allowlist;
        }
    }

    @Override
    public Boolean apply(String className) {
        Assert.checkNotNullParam("className", className);
        boolean anyAccept = false;
        if (unmarshallingFilters != null) {

            for (int i = 0; i < unmarshallingFilters.size(); i++) {
                Function<String, Boolean> func = unmarshallingFilters.get(i);
                Boolean accept = func.apply(className);
                if (accept == Boolean.FALSE) {
                    String failedSpec = func instanceof ExactMatchFilter ? "!" + className : parsedFilterSpecs.get(i);
                    REMOTE_LOGGER.debugf("Class %s has been explicitly rejected by filter spec element %s", className, failedSpec);
                    return false;
                } else {
                    anyAccept |= accept != null;
                }
            }
            if (allowlistUnmarshalling && !anyAccept) {
                REMOTE_LOGGER.debugf("Class %s has not been explicitly allowlisted by filter spec %s", className, filterSpec);
                return false;
            }
        }
        return true;
    }

    private static class ExactMatchFilter implements Function<String, Boolean> {
        private final Set<String> matches = new HashSet<>();
        private final Boolean matchResult;

        private ExactMatchFilter(boolean forAllowlist) {
            this.matchResult = forAllowlist;
        }

        private void addMatchingClass(String name) {
            matches.add(name);
        }

        @Override
        public Boolean apply(String s) {
            return matches.contains(s) ? matchResult : null;
        }
    }
}
