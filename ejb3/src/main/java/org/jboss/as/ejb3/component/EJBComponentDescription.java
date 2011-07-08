/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.ComponentViewInstance;
import org.jboss.as.ee.component.NamespaceConfigurator;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.EJBMethodIdentifier;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.deployment.EjbJarConfiguration;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.security.EJBSecurityViewConfigurator;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagementType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class EJBComponentDescription extends ComponentDescription {
    /**
     * EJB 3.1 FR 13.3.7, the default transaction attribute is <i>REQUIRED</i>.
     */
    private TransactionAttributeType beanTransactionAttribute = TransactionAttributeType.REQUIRED;
    /**
     * EJB 3.1 FR 13.3.1, the default transaction management type is container-managed transaction demarcation.
     */
    private TransactionManagementType transactionManagementType = TransactionManagementType.CONTAINER;

    private final Map<MethodIntf, TransactionAttributeType> txPerViewStyle1 = new HashMap<MethodIntf, TransactionAttributeType>();

    /**
     * The security-domain, if any, for this bean
     */
    private String securityDomain;

    /**
     * The @DeclareRoles (a.k.a security-role-ref) for the bean
     */
    private final Set<String> declaredRoles = new HashSet<String>();

    /**
     * The @RunAs role associated with this bean, if any
     */
    private String runAsRole;

    /**
     * The @DenyAll/exclude-list map of methods. The key is the view class name and the value is a collection of EJB methods
     * which are marked for @DenyAll/exclude-list
     */
    private final Map<String, Collection<EJBMethodIdentifier>> methodLevelDenyAll = new HashMap<String, Collection<EJBMethodIdentifier>>();

    /**
     * The class level @DenyAll/exclude-list map. The key is the view class name and the value is a collection of classes,
     * in the class hierarchy of the EJB implementation class (ex: EJB implementation class' super class),
     * which have been marked with @DenyAll.
     */
    private final Map<String, Collection<String>> classLevelDenyAll = new HashMap<String, Collection<String>>();

    /**
     * Method level roles allowed, per view. The key is the view class name and the value is a Map whose key is the EJB
     * method identifier and the value is the collection of role names.
     */
    private final Map<String, Map<EJBMethodIdentifier, Set<String>>> methodLevelRolesAllowed = new HashMap<String, Map<EJBMethodIdentifier, Set<String>>>();

    /**
     * Class level roles allowed, per view. The key is the view class name and the value is a Map whose key is the class
     * name on which the @RolesAllowed is applied and the value is the collection of role names
     */
    private final Map<String, Map<String, Set<String>>> classLevelRolesAllowed = new HashMap<String, Map<String, Set<String>>>();

    /**
     * Security role links. The key is the "from" role name and the value is a collection of "to" role names of the link.
     */
    private final Map<String, Collection<String>> securityRoleLinks = new HashMap<String, Collection<String>>();


    /**
     * The @PermitAll map of methods. The key is the view class name and the value is a collection of EJB methods
     * which are marked for @PermitAll
     */
    private final Map<String, Collection<EJBMethodIdentifier>> methodLevelPermitAll = new HashMap<String, Collection<EJBMethodIdentifier>>();

    /**
     * The class level @PermitAll map. The key is the view class name and the value is a collection of classes,
     * in the class hierarchy of the EJB implementation class (ex: EJB implementation class' super class),
     * which have been marked with @PermitAll.
     */
    private final Map<String, Collection<String>> classLevelPermitAll = new HashMap<String, Collection<String>>();


    /**
     * Stores around invoke methods that are referenced in the DD that cannot be resolved until the module is loaded
     */
    private final List<String> aroundInvokeDDMethods = new ArrayList<String>(0);
    private final List<String> preDestroyDDMethods = new ArrayList<String>(0);
    private final List<String> postConstructDDMethods = new ArrayList<String>(0);


    private final PopulatingMap<MethodIntf, Map<String, TransactionAttributeType>> txPerViewStyle2 = new PopulatingMap<MethodIntf, Map<String, TransactionAttributeType>>() {
        @Override
        Map<String, TransactionAttributeType> populate() {
            return new HashMap<String, TransactionAttributeType>();
        }
    };
    private final PopulatingMap<MethodIntf, PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>>> txPerViewStyle3 = new PopulatingMap<MethodIntf, PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>>>() {
        @Override
        PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>> populate() {
            return new PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>>() {
                @Override
                Map<ArrayKey, TransactionAttributeType> populate() {
                    return new HashMap<ArrayKey, TransactionAttributeType>();
                }
            };
        }
    };

    private final Map<String, TransactionAttributeType> txStyle1 = new HashMap<String, TransactionAttributeType>();
    private final Map<String, TransactionAttributeType> txStyle2 = new HashMap<String, TransactionAttributeType>();
    private final PopulatingMap<String, PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>>> txStyle3 = new PopulatingMap<String, PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>>>() {
        @Override
        PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>> populate() {
            return new PopulatingMap<String, Map<ArrayKey, TransactionAttributeType>>() {
                @Override
                Map<ArrayKey, TransactionAttributeType> populate() {
                    return new HashMap<ArrayKey, TransactionAttributeType>();
                }
            };
        }
    };

    /**
     * Construct a new instance.
     *
     * @param componentName      the component name
     * @param componentClassName the component instance class name
     * @param ejbJarDescription  the module
     */
    public EJBComponentDescription(final String componentName, final String componentClassName, final EjbJarDescription ejbJarDescription, final ServiceName deploymentUnitServiceName) {
        super(componentName, componentClassName, ejbJarDescription.getEEModuleDescription(), ejbJarDescription.getApplicationClassesDescription().getOrAddClassByName(componentClassName), deploymentUnitServiceName, ejbJarDescription.getApplicationClassesDescription());
        if (ejbJarDescription.isWar()) {
            setNamingMode(ComponentNamingMode.USE_MODULE);
        } else {
            setNamingMode(ComponentNamingMode.CREATE);
        }

        getConfigurators().addFirst(new NamespaceConfigurator());
        getConfigurators().add(new EjbJarConfigurationConfigurator());

        // setup a dependency on the EJBUtilities service
        this.addDependency(EJBUtilities.SERVICE_NAME, ServiceBuilder.DependencyType.REQUIRED);
        // setup a current invocation interceptor
        this.addCurrentInvocationContextFactory();

    }

    private static <K, V> V get(Map<K, V> map, K key) {
        if (map == null)
            return null;
        return map.get(key);
    }

    public TransactionAttributeType getTransactionAttribute(MethodIntf methodIntf, String className, String methodName, String... methodParams) {
        assert methodIntf != null : "methodIntf is null";
        assert methodName != null : "methodName is null";
        assert methodParams != null : "methodParams is null";

        ArrayKey methodParamsKey = new ArrayKey((Object[]) methodParams);
        TransactionAttributeType txAttr = get(get(get(txPerViewStyle3, methodIntf), methodName), methodParamsKey);
        if (txAttr != null)
            return txAttr;
        txAttr = get(get(txPerViewStyle2, methodIntf), methodName);
        if (txAttr != null)
            return txAttr;
        txAttr = get(txPerViewStyle1, methodIntf);
        if (txAttr != null)
            return txAttr;
        txAttr = get(get(get(txStyle3, className), methodName), methodParamsKey);
        if (txAttr != null)
            return txAttr;
        txAttr = get(txStyle2, methodName);
        if (txAttr != null)
            return txAttr;
        txAttr = get(txStyle1, className);
        if (txAttr != null)
            return txAttr;
        return beanTransactionAttribute;
    }

    public TransactionManagementType getTransactionManagementType() {
        return transactionManagementType;
    }

    /**
     * Style 1 (13.3.7.2.1 @1)
     *
     * @param methodIntf           the method-intf the annotations apply to or null if EJB class itself
     * @param transactionAttribute
     */
    public void setTransactionAttribute(MethodIntf methodIntf, String className, TransactionAttributeType transactionAttribute) {
        if (methodIntf != null && className != null)
            throw new IllegalArgumentException("both methodIntf and className are set on " + getComponentName());
        if (methodIntf == null) {
            txStyle1.put(className, transactionAttribute);
        } else
            txPerViewStyle1.put(methodIntf, transactionAttribute);
    }

    /**
     * Style 2 (13.3.7.2.1 @2)
     *
     * @param methodIntf           the method-intf the annotations apply to or null if EJB class itself
     * @param transactionAttribute
     * @param methodName
     */
    public void setTransactionAttribute(MethodIntf methodIntf, TransactionAttributeType transactionAttribute, String methodName) {
        if (methodIntf == null)
            txStyle2.put(methodName, transactionAttribute);
        else
            txPerViewStyle2.pick(methodIntf).put(methodName, transactionAttribute);
    }

    /**
     * Style 3 (13.3.7.2.1 @3)
     *
     * @param methodIntf           the method-intf the annotations apply to or null if EJB class itself
     * @param transactionAttribute
     * @param methodName
     * @param methodParams
     */
    public void setTransactionAttribute(MethodIntf methodIntf, TransactionAttributeType transactionAttribute, final String className, String methodName, String... methodParams) {
        ArrayKey methodParamsKey = new ArrayKey((Object[]) methodParams);
        if (methodIntf == null)
            txStyle3.pick(className).pick(methodName).put(methodParamsKey, transactionAttribute);
        else
            txPerViewStyle3.pick(methodIntf).pick(methodName).put(methodParamsKey, transactionAttribute);
    }

    public void setTransactionManagementType(TransactionManagementType transactionManagementType) {
        this.transactionManagementType = transactionManagementType;
    }

    public String getEJBName() {
        return this.getComponentName();
    }

    public String getEJBClassName() {
        return this.getComponentClassName();
    }

    protected void setupViewInterceptors(ViewDescription view) {
        this.addCurrentInvocationContextFactory(view);
        this.setupSecurityInterceptors(view);
    }

    protected void setupClientViewInterceptors(ViewDescription view) {
        // add a client side interceptor which handles the toString() method invocation on the bean's views
        this.addToStringMethodInterceptor(view);
    }

    /**
     * Setup the current invocation context interceptor, which will be used during the post-construct
     * lifecycle of the component instance
     */
    protected abstract void addCurrentInvocationContextFactory();

    /**
     * Setup the current invocation context interceptor, which will be used during the invocation on the view (methods)
     *
     * @param view The view for which the interceptor has to be setup
     */
    protected abstract void addCurrentInvocationContextFactory(ViewDescription view);

    protected void setupSecurityInterceptors(final ViewDescription view) {
        // setup security interceptor for the component
        view.getConfigurators().add(new EJBSecurityViewConfigurator());
    }

    private void addToStringMethodInterceptor(final ViewDescription view) {
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                final Method TO_STRING_METHOD;
                try {
                    TO_STRING_METHOD = Object.class.getMethod("toString", new Class<?>[0]);
                } catch (NoSuchMethodException nsme) {
                    throw new DeploymentUnitProcessingException(nsme);
                }
                Method[] methods = configuration.getProxyFactory().getCachedMethods();
                for (Method method : methods) {
                    if (TO_STRING_METHOD.equals(method)) {
                        configuration.addClientInterceptor(method, new ImmediateInterceptorFactory(new ToStringMethodInterceptor(EJBComponentDescription.this.getComponentName())), InterceptorOrder.Client.TO_STRING);
                        return;
                    }
                }
            }
        });
    }

    public boolean isMessageDriven() {
        return false;
    }

    public boolean isSingleton() {
        return false;
    }

    public boolean isStateful() {
        return false;
    }

    public boolean isStateless() {
        return false;
    }

    public void addDeclaredRoles(String... roles) {
        this.declaredRoles.addAll(Arrays.asList(roles));
    }

    public void setDeclaredRoles(Collection<String> roles) {
        if (roles == null) {
            throw new IllegalArgumentException("Cannot set security roles to null");
        }
        this.declaredRoles.clear();
        this.declaredRoles.addAll(roles);
    }

    public Set<String> getDeclaredRoles() {
        return Collections.unmodifiableSet(this.declaredRoles);
    }

    public void setRunAs(String role) {
        this.runAsRole = role;
    }

    public String getRunAs() {
        return this.runAsRole;
    }

    public void setSecurityDomain(String securityDomain) {
        this.securityDomain = securityDomain;
    }

    public String getSecurityDomain() {
        return this.securityDomain;
    }

    public void applyDenyAllOnAllViewsForClass(final String className) {
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("Classname cannot be null or empty: " + className);
        }
        for (final ViewDescription view : this.getViews()) {
            Collection<String> denyAllClasses = this.classLevelDenyAll.get(view.getViewClassName());
            if (denyAllClasses == null) {
                denyAllClasses = new HashSet<String>();
                this.classLevelDenyAll.put(view.getViewClassName(), denyAllClasses);
            }
            denyAllClasses.add(className);
        }
    }

    public void applyDenyAllOnAllViewsForMethod(final EJBMethodIdentifier ejbMethodIdentifier) {
        for (final ViewDescription view : this.getViews()) {
            Collection<EJBMethodIdentifier> denyAllViewMethods = this.methodLevelDenyAll.get(view.getViewClassName());
            if (denyAllViewMethods == null) {
                denyAllViewMethods = new ArrayList<EJBMethodIdentifier>();
                this.methodLevelDenyAll.put(view.getViewClassName(), denyAllViewMethods);
            }
            denyAllViewMethods.add(ejbMethodIdentifier);
        }
    }

    public void applyDenyAllOnViewTypeForMethod(final MethodIntf viewType, final EJBMethodIdentifier ejbMethodIdentifier) {
        // find the right view(s) to apply the @DenyAll
        for (final ViewDescription view : this.getViews()) {
            // shouldn't really happen
            if (view instanceof EJBViewDescription == false) {
                continue;
            }
            final EJBViewDescription ejbView = (EJBViewDescription) view;
            // skip irrelevant views
            if (ejbView.getMethodIntf() != viewType) {
                continue;
            }
            Collection<EJBMethodIdentifier> denyAllViewMethods = this.methodLevelDenyAll.get(view.getViewClassName());
            if (denyAllViewMethods == null) {
                denyAllViewMethods = new ArrayList<EJBMethodIdentifier>();
                this.methodLevelDenyAll.put(view.getViewClassName(), denyAllViewMethods);
            }
            denyAllViewMethods.add(ejbMethodIdentifier);
        }
    }

    public void applyDenyAllOnAllMethodsOfAllViews() {
        // "All methods" implies a class level @DenyAll (a.k.a exclude-list)
        this.applyDenyAllOnAllViewsForClass(this.getEJBClassName());
    }

    public void applyDenyAllOnAllMethodsOfViewType(final MethodIntf viewType) {
        // find the right view(s) to apply the @DenyAll
        for (final ViewDescription view : this.getViews()) {
            // shouldn't really happen
            if (view instanceof EJBViewDescription == false) {
                continue;
            }
            final EJBViewDescription ejbView = (EJBViewDescription) view;
            // skip irrelevant views
            if (ejbView.getMethodIntf() != viewType) {
                continue;
            }
            // now apply the @DenyAll on class level for this view
            Collection<String> denyAllApplicableClasses = this.classLevelDenyAll.get(ejbView.getViewClassName());
            if (denyAllApplicableClasses == null) {
                denyAllApplicableClasses = new HashSet<String>();
                this.classLevelDenyAll.put(ejbView.getViewClassName(), denyAllApplicableClasses);
            }
            denyAllApplicableClasses.add(this.getEJBClassName());
        }
    }

    public Collection<EJBMethodIdentifier> getDenyAllMethodsForView(final String viewClassName) {
        final Collection<EJBMethodIdentifier> denyAllMethods = this.methodLevelDenyAll.get(viewClassName);
        if (denyAllMethods != null) {
            return Collections.unmodifiableCollection(denyAllMethods);
        }
        return Collections.emptySet();
    }

    public boolean isDenyAllApplicableToClass(final String viewClassName, final String className) {
        final Collection<String> denyAllApplicableClasses = this.classLevelDenyAll.get(viewClassName);
        if (denyAllApplicableClasses == null) {
            return false;
        }
        return denyAllApplicableClasses.contains(className);
    }

    public void setRolesAllowedOnAllViewsForClass(final String className, final Set<String> roles) {
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("Classname cannot be null or empty: " + className);
        }
        if (roles == null) {
            throw new IllegalArgumentException("Cannot set null roles for class " + className);
        }
        for (final ViewDescription view : this.getViews()) {
            Map<String, Set<String>> perViewRoles = this.classLevelRolesAllowed.get(view.getViewClassName());
            if (perViewRoles == null) {
                perViewRoles = new HashMap<String, Set<String>>();
                this.classLevelRolesAllowed.put(view.getViewClassName(), perViewRoles);
            }
            // set the roles for the classname
            perViewRoles.put(className, roles);
        }
    }

    public void setRolesAllowedForAllMethodsOfAllViews(final Set<String> roles) {
        // "All methods" implies a class level @RolesAllowed (a.k.a security-role)
        this.setRolesAllowedOnAllViewsForClass(this.getEJBClassName(), roles);
    }

    public void setRolesAllowedOnAllViewsForMethod(final EJBMethodIdentifier ejbMethodIdentifier, final Set<String> roles) {
        if (ejbMethodIdentifier == null) {
            throw new IllegalArgumentException("EJB method identifier cannot be null while setting roles on method");
        }
        if (roles == null) {
            throw new IllegalArgumentException("Roles cannot be null while setting roles on method: " + ejbMethodIdentifier);
        }
        for (final ViewDescription view : this.getViews()) {
            Map<EJBMethodIdentifier, Set<String>> perViewMethodRoles = this.methodLevelRolesAllowed.get(view.getViewClassName());
            if (perViewMethodRoles == null) {
                perViewMethodRoles = new HashMap<EJBMethodIdentifier, Set<String>>();
                this.methodLevelRolesAllowed.put(view.getViewClassName(), perViewMethodRoles);
            }
            // set the roles on the method
            perViewMethodRoles.put(ejbMethodIdentifier, roles);
        }
    }

    public void setRolesAllowedForAllMethodsOnViewType(final MethodIntf viewType, final Set<String> roles) {
        if (roles == null) {
            throw new IllegalArgumentException("Roles cannot be null while setting roles on view type: " + viewType);
        }
        // find the right view(s) to apply the @RolesAllowed
        for (final ViewDescription view : this.getViews()) {
            // shouldn't really happen
            if (view instanceof EJBViewDescription == false) {
                continue;
            }
            final EJBViewDescription ejbView = (EJBViewDescription) view;
            // skip irrelevant views
            if (ejbView.getMethodIntf() != viewType) {
                continue;
            }
            // now apply the @RolesAllowed on class level for this view
            Map<String, Set<String>> perViewRoles = this.classLevelRolesAllowed.get(view.getViewClassName());
            if (perViewRoles == null) {
                perViewRoles = new HashMap<String, Set<String>>();
                this.classLevelRolesAllowed.put(view.getViewClassName(), perViewRoles);
            }
            // set the roles for the view class
            perViewRoles.put(view.getViewClassName(), roles);

        }
    }

    public void setRolesAllowedForMethodOnViewType(final MethodIntf viewType, final EJBMethodIdentifier ejbMethodIdentifier, final Set<String> roles) {
        if (ejbMethodIdentifier == null) {
            throw new IllegalArgumentException("EJB method identifier cannot be null while setting roles on view type: " + viewType);
        }
        if (roles == null) {
            throw new IllegalArgumentException("Roles cannot be null while setting roles on view type: " + viewType + " and method: " + ejbMethodIdentifier);
        }

        // find the right view(s) to apply the @RolesAllowed
        for (final ViewDescription view : this.getViews()) {
            // shouldn't really happen
            if (view instanceof EJBViewDescription == false) {
                continue;
            }
            final EJBViewDescription ejbView = (EJBViewDescription) view;
            // skip irrelevant views
            if (ejbView.getMethodIntf() != viewType) {
                continue;
            }
            Map<EJBMethodIdentifier, Set<String>> perViewMethodRoles = this.methodLevelRolesAllowed.get(view.getViewClassName());
            if (perViewMethodRoles == null) {
                perViewMethodRoles = new HashMap<EJBMethodIdentifier, Set<String>>();
                this.methodLevelRolesAllowed.put(view.getViewClassName(), perViewMethodRoles);
            }
            // set the roles on the method
            perViewMethodRoles.put(ejbMethodIdentifier, roles);
        }
    }

    public Set<String> getRolesAllowed(final String viewClassName, final EJBMethodIdentifier method) {
        final Map<EJBMethodIdentifier, Set<String>> methods = this.methodLevelRolesAllowed.get(viewClassName);
        if (methods == null || methods.get(method) == null) {
            return Collections.emptySet();
        }

        return methods.get(method);
    }

    public Set<String> getRolesAllowedForClass(final String viewClassName, final String className) {
        final Map<String, Set<String>> perClassRoles = this.classLevelRolesAllowed.get(viewClassName);
        if (perClassRoles == null || perClassRoles.get(className) == null) {
            return Collections.emptySet();
        }

        return perClassRoles.get(className);
    }

    public void linkSecurityRoles(final String fromRole, final String toRole) {
        if (fromRole == null || fromRole.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot link from a null or empty security role: " + fromRole);
        }
        if (toRole == null || toRole.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot link to a null or empty security role: " + toRole);
        }

        Collection<String> roleLinks = this.securityRoleLinks.get(fromRole);
        if (roleLinks == null) {
            roleLinks = new HashSet<String>();
            this.securityRoleLinks.put(fromRole, roleLinks);
        }
        roleLinks.add(toRole);
    }

    public Map<String, Collection<String>> getSecurityRoleLinks() {
        return Collections.unmodifiableMap(this.securityRoleLinks);
    }

    public void applyPermitAllOnAllViewsForClass(final String className) {
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("Classname cannot be null or empty: " + className);
        }
        for (final ViewDescription view : this.getViews()) {
            Collection<String> permitAllClasses = this.classLevelPermitAll.get(view.getViewClassName());
            if (permitAllClasses == null) {
                permitAllClasses = new HashSet<String>();
                this.classLevelPermitAll.put(view.getViewClassName(), permitAllClasses);
            }
            permitAllClasses.add(className);
        }
    }

    public void applyPermitAllOnAllViewsForMethod(final EJBMethodIdentifier ejbMethodIdentifier) {
        for (final ViewDescription view : this.getViews()) {
            Collection<EJBMethodIdentifier> permitAllMethods = this.methodLevelPermitAll.get(view.getViewClassName());
            if (permitAllMethods == null) {
                permitAllMethods = new ArrayList<EJBMethodIdentifier>();
                this.methodLevelPermitAll.put(view.getViewClassName(), permitAllMethods);
            }
            permitAllMethods.add(ejbMethodIdentifier);
        }
    }

    public Collection<EJBMethodIdentifier> getPermitAllMethodsForView(final String viewClassName) {
        final Collection<EJBMethodIdentifier> permitAllMethods = this.methodLevelPermitAll.get(viewClassName);
        if (permitAllMethods != null) {
            return Collections.unmodifiableCollection(permitAllMethods);
        }
        return Collections.emptySet();
    }

    public boolean isPermitAllApplicableToClass(final String viewClassName, final String className) {
        final Collection<String> permitAllApplicationClasses = this.classLevelPermitAll.get(viewClassName);
        if (permitAllApplicationClasses == null) {
            return false;
        }
        return permitAllApplicationClasses.contains(className);
    }

    /**
     * Returns true if this bean is secured. Else returns false.
     *
     * @return
     */
    public boolean isSecurityEnabled() {
        return this.securityDomain != null;
    }

    /**
     * A {@link ComponentConfigurator} which picks up {@link EjbJarConfiguration} from the attachment of the deployment
     * unit and sets it to the {@link EJBComponentCreateServiceFactory component create service factory} of the component
     * configuration.
     * <p/>
     * The component configuration is expected to be set with {@link EJBComponentCreateServiceFactory}, as its create
     * service factory, before this {@link EjbJarConfigurationConfigurator} is run.
     */
    private class EjbJarConfigurationConfigurator implements ComponentConfigurator {

        @Override
        public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
            final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
            final EjbJarConfiguration ejbJarConfiguration = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_CONFIGURATION);
            if (ejbJarConfiguration == null) {
                throw new DeploymentUnitProcessingException("EjbJarConfiguration not found as an attachment in deployment unit: " + deploymentUnit);
            }
            final EJBComponentCreateServiceFactory ejbComponentCreateServiceFactory = (EJBComponentCreateServiceFactory) configuration.getComponentCreateServiceFactory();
            ejbComponentCreateServiceFactory.setEjbJarConfiguration(ejbJarConfiguration);
        }
    }

    /**
     * {@link Interceptor} which returns the string representation for the view instance on which the {@link Object#toString()}
     * invocation happened. This interceptor (for performance reasons) does *not* check whether the invoked method is <code>toString()</code>
     * method, so it's the responsibility of the component to setup this interceptor *only* on <code>toString()</code> method on the component
     * views.
     */
    private static class ToStringMethodInterceptor implements Interceptor {

        private final String name;

        public ToStringMethodInterceptor(final String name) {
            this.name = name;
        }


        @Override
        public Object processInvocation(InterceptorContext context) throws Exception {
            final ComponentViewInstance componentViewInstance = context.getPrivateData(ComponentViewInstance.class);
            if (componentViewInstance == null) {
                throw new IllegalStateException("ComponentViewInstance not available in interceptor context: " + context);
            }
            return "Proxy for view class: " + componentViewInstance.getViewClass().getName() + " of EJB: " + name;
        }
    }

    public List<String> getAroundInvokeDDMethods() {
        return aroundInvokeDDMethods;
    }

    public List<String> getPostConstructDDMethods() {
        return postConstructDDMethods;
    }

    public List<String> getPreDestroyDDMethods() {
        return preDestroyDDMethods;
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
                "serviceName=" + getServiceName() +
                '}' + "@" + Integer.toHexString(hashCode());
    }
}
