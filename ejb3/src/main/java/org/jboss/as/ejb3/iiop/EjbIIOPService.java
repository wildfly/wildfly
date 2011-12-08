/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.iiop;


import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ejb.EJBHome;
import javax.ejb.EJBMetaData;
import javax.rmi.PortableRemoteObject;

import org.jacorb.ssl.SSLPolicyValue;
import org.jacorb.ssl.SSLPolicyValueHelper;
import org.jacorb.ssl.SSL_POLICY_TYPE;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.stateless.StatelessSessionComponent;
import org.jboss.as.jacorb.csiv2.CSIv2Policy;
import org.jboss.as.jacorb.rmi.ir.InterfaceRepository;
import org.jboss.as.jacorb.rmi.marshal.strategy.SkeletonStrategy;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.EntityEJBLocator;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.ejb.iiop.EJBMetaDataImplIIOP;
import org.jboss.ejb.iiop.HomeHandleImplIIOP;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.marshalling.OutputStreamByteOutput;
import org.jboss.marshalling.river.RiverMarshallerFactory;
import org.jboss.metadata.ejb.jboss.IIOPMetaData;
import org.jboss.metadata.ejb.jboss.IORSecurityConfigMetaData;
import org.jboss.metadata.ejb.jboss.IORTransportConfigMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.omg.CORBA.Any;
import org.omg.CORBA.InterfaceDef;
import org.omg.CORBA.InterfaceDefHelper;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CORBA.Repository;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.Current;
import org.omg.PortableServer.CurrentHelper;
import org.omg.PortableServer.POA;

/**
 * This is an IIOP "proxy factory" for <code>EJBHome</code>s and
 * <code>EJBObject</code>s. Rather than creating Java proxies (as the JRMP
 * proxy factory does), this factory creates CORBA IORs.
 * <p/>
 * An <code>EjbIIOPService</code> is associated to a given enterprise bean. It
 * registers with the IIOP invoker two CORBA servants: an
 * <code>EjbHomeCorbaServant</code> for the bean's
 * <code>EJBHome</code> and an <code>EjbObjectCorbaServant</code> for the
 * bean's <code>EJBObject</code>s.
 */
public class EjbIIOPService implements Service<EjbIIOPService> {

    private static final Logger logger = Logger.getLogger(EjbIIOPService.class);

    /**
     * The service name
     */
    public static final ServiceName SERVICE_NAME = ServiceName.of("EjbIIOPService");

    /**
     * The component
     */
    private final InjectedValue<EJBComponent> ejbComponentInjectedValue = new InjectedValue<EJBComponent>();

    /**
     * The home view
     */
    private final InjectedValue<ComponentView> homeView = new InjectedValue<ComponentView>();

    /**
     * the ejbObject view
     */
    private final InjectedValue<ComponentView> remoteView = new InjectedValue<ComponentView>();


    private final InjectedValue<POARegistry> poaRegistry = new InjectedValue<POARegistry>();

    /**
     * The corba naming context
     */
    private final InjectedValue<NamingContextExt> corbaNamingContext = new InjectedValue<NamingContextExt>();

    /**
     * A reference for the ORB.
     */
    private final InjectedValue<ORB> orb = new InjectedValue<ORB>();

    /**
     * The module loader
     */
    private final InjectedValue<ServiceModuleLoader> serviceModuleLoaderInjectedValue = new InjectedValue<ServiceModuleLoader>();

    /**
     * Used for serializing EJB id's
     */
    private MarshallerFactory factory;
    private MarshallingConfiguration configuration;

    /**
     * <code>EJBMetaData</code> the enterprise bean in the container.
     */
    private EJBMetaData ejbMetaData;

    /**
     * Mapping from bean methods to <code>SkeletonStrategy</code> instances.
     */
    private final Map<String, SkeletonStrategy> beanMethodMap;

    /**
     * Mapping from home methods to <code>SkeletonStrategy</code> instances.
     */
    private final Map<String, SkeletonStrategy> homeMethodMap;

    /**
     * CORBA repository ids of the RMI-IDL interfaces implemented by the bean
     * (<code>EJBObject</code> instance).
     */
    private final String[] beanRepositoryIds;

    /**
     * CORBA repository ids of the RMI-IDL interfaces implemented by the bean's
     * home (<code>EJBHome</code> instance).
     */
    private final String[] homeRepositoryIds;

    private final boolean useQualifiedName;

    private final Module module;

    /**
     * <code>ServantRegistry</code> for the container's <code>EJBHome</code>.
     */
    private ServantRegistry homeServantRegistry;

    /**
     * <code>ServantRegistry</code> for the container's <code>EJBObject</code>s.
     */
    private ServantRegistry beanServantRegistry;

    /**
     * <code>ReferenceFactory</code> for <code>EJBObject</code>s.
     */
    private ReferenceFactory beanReferenceFactory;

    /**
     * The container's <code>EJBHome</code>.
     */
    private EJBHome ejbHome;

    /**
     * The enterprise bean's interface repository implementation, or null
     * if the enterprise bean does not have its own interface repository.
     */
    private InterfaceRepository iri;

    /**
     * POA for the enterprise bean's interface repository.
     */
    private final InjectedValue<POA> irPoa = new InjectedValue<POA>();

    /**
     * The IIOP metadata, configured in the assembly descriptor.
     */
    private IIOPMetaData iiopMetaData;

    /**
     * The fully qualified name
     */
    private String name = null;

    public EjbIIOPService(final Map<String, SkeletonStrategy> beanMethodMap, final String[] beanRepositoryIds,
                          final Map<String, SkeletonStrategy> homeMethodMap, final String[] homeRepositoryIds,
                          final boolean useQualifiedName, final IIOPMetaData iiopMetaData, final Module module) {
        this.useQualifiedName = useQualifiedName;
        this.module = module;
        this.beanMethodMap = Collections.unmodifiableMap(beanMethodMap);
        this.beanRepositoryIds = beanRepositoryIds;
        this.homeMethodMap = Collections.unmodifiableMap(homeMethodMap);
        this.homeRepositoryIds = homeRepositoryIds;
        this.iiopMetaData = iiopMetaData;
    }


    public synchronized void start(final StartContext startContext) throws StartException {

        try {
            final RiverMarshallerFactory factory = new RiverMarshallerFactory();
            final MarshallingConfiguration configuration = new MarshallingConfiguration();
            configuration.setClassResolver(ModularClassResolver.getInstance(serviceModuleLoaderInjectedValue.getValue()));
            this.configuration = configuration;
            this.factory = factory;


            // Should create a CORBA interface repository?
            final boolean interfaceRepositorySupported = false;

            // Build binding name of the bean.
            final EJBComponent component = ejbComponentInjectedValue.getValue();
            final String earApplicationName = component.getEarApplicationName();
            if(iiopMetaData != null && iiopMetaData.getBindingName() != null) {
                name = iiopMetaData.getBindingName();
            } else if (useQualifiedName) {
                if (component.getDistinctName() == null || component.getDistinctName().isEmpty()) {
                    name = earApplicationName == null || earApplicationName.isEmpty() ? "" : earApplicationName + "/";
                    name = name + component.getModuleName() + "/" + component.getComponentName();
                } else {
                    name = earApplicationName == null || earApplicationName.isEmpty() ? "" : earApplicationName + "/";
                    name = name + component.getModuleName() + "/" + component.getDistinctName() + "/" + component.getComponentName();
                }
            } else {
                name = component.getComponentName();
            }
            name = name.replace(".", "_");

            final ORB orb = this.orb.getValue();
            if (interfaceRepositorySupported) {
                // Create a CORBA interface repository for the enterprise bean
                iri = new InterfaceRepository(orb, irPoa.getValue(), name);
                // Add bean interface info to the interface repository
                iri.mapClass(remoteView.getValue().getViewClass());
                iri.mapClass(homeView.getValue().getViewClass());
                iri.finishBuild();
                logger.info("CORBA interface repository for " + name + ": " + orb.object_to_string(iri.getReference()));
            }

            IORSecurityConfigMetaData iorSecurityConfigMetaData = null;
            if (this.iiopMetaData != null)
                iorSecurityConfigMetaData = this.iiopMetaData.getIorSecurityConfigMetaData();

            // Create security policies if security metadata has been provided.
            List<Policy> policyList = new ArrayList<Policy>();
            if (iorSecurityConfigMetaData != null) {

                // Create csiv2Policy for both home and remote containing IorSecurityConfigMetadata.
                final Any secPolicy = orb.create_any();
                secPolicy.insert_Value(iorSecurityConfigMetaData);
                Policy csiv2Policy = orb.create_policy(CSIv2Policy.TYPE, secPolicy);
                policyList.add(csiv2Policy);

                // Create SSLPolicy (SSL_REQUIRED ensures home and remote IORs will have port 0 in the primary address).
                boolean sslRequired = false;
                if (iorSecurityConfigMetaData != null) {
                    IORTransportConfigMetaData tc = iorSecurityConfigMetaData.getTransportConfig();
                    sslRequired = IORTransportConfigMetaData.INTEGRITY_REQUIRED.equals(tc.getIntegrity())
                            || IORTransportConfigMetaData.CONFIDENTIALITY_REQUIRED.equals(tc.getConfidentiality())
                            || IORTransportConfigMetaData.ESTABLISH_TRUST_IN_CLIENT_REQUIRED.equals(tc.getEstablishTrustInClient());
                }
                final Any sslPolicyValue = orb.create_any();
                SSLPolicyValueHelper.insert(sslPolicyValue, (sslRequired) ? SSLPolicyValue.SSL_REQUIRED : SSLPolicyValue.SSL_NOT_REQUIRED);
                Policy sslPolicy = orb.create_policy(SSL_POLICY_TYPE.value, sslPolicyValue);
                policyList.add(sslPolicy);

                logger.debug("container's SSL policy: " + sslPolicy);
            }

            String securityDomain = "CORBA_REMOTE"; //TODO: what should this default to
            if(component.getSecurityMetaData() != null) {
                securityDomain = component.getSecurityMetaData().getSecurityDomain();
            }

            Policy[] policies = policyList.toArray(new Policy[policyList.size()]);

            // If there is an interface repository, then get the homeInterfaceDef from the IR
            InterfaceDef homeInterfaceDef = null;
            if (iri != null) {
                Repository ir = iri.getReference();
                homeInterfaceDef = InterfaceDefHelper.narrow(ir.lookup_id(homeRepositoryIds[0]));
            }

            // Get the POACurrent object
            Current poaCurrent = CurrentHelper.narrow(orb.resolve_initial_references("POACurrent"));

            // Instantiate home servant, bind it to the servant registry, and create CORBA reference to the EJBHome.
            final EjbCorbaServant homeServant = new EjbCorbaServant(poaCurrent, homeMethodMap, homeRepositoryIds, homeInterfaceDef,
                    orb, homeView.getValue(),factory, configuration, component.getTransactionManager(), module.getClassLoader(), true, securityDomain);

            homeServantRegistry = poaRegistry.getValue().getRegistryWithPersistentPOAPerServant();
            ReferenceFactory homeReferenceFactory = homeServantRegistry.bind(homeServantName(name), homeServant, policies);

            final org.omg.CORBA.Object corbaRef = homeReferenceFactory.createReference(homeRepositoryIds[0]);
            ejbHome = (EJBHome) PortableRemoteObject.narrow(corbaRef, EJBHome.class);
            final HomeHandleImplIIOP homeHandle = new HomeHandleImplIIOP(orb.object_to_string(corbaRef));
            homeServant.setHomeHandle(homeHandle);

            // Initialize beanPOA and create metadata depending on the kind of bean
            if (component instanceof EntityBeanComponent) {

                // This is an entity bean (lifespan: persistent)
                beanServantRegistry = poaRegistry.getValue().getRegistryWithPersistentPOAPerServant();
                final EntityBeanComponent entityBeanComponent = (EntityBeanComponent) component;
                final Class pkClass = entityBeanComponent.getPrimaryKeyClass();
                ejbMetaData = new EJBMetaDataImplIIOP(entityBeanComponent.getRemoteClass(), entityBeanComponent.getHomeClass(), pkClass, false, false, homeHandle);
            } else {
                // This is a session bean (lifespan: transient)
                beanServantRegistry = poaRegistry.getValue().getRegistryWithTransientPOAPerServant();
                if (component instanceof StatelessSessionComponent) {
                    // Stateless session bean
                    ejbMetaData = new EJBMetaDataImplIIOP(remoteView.getValue().getViewClass(), homeView.getValue().getViewClass(), null, true, true, homeHandle);
                } else {
                    // Stateful session bean
                    ejbMetaData = new EJBMetaDataImplIIOP(remoteView.getValue().getViewClass(), homeView.getValue().getViewClass(), null, true, false, homeHandle);
                }
            }
            homeServant.setEjbMetaData(ejbMetaData);

            // If there is an interface repository, then get the beanInterfaceDef from the IR
            InterfaceDef beanInterfaceDef = null;
            if (iri != null) {
                final Repository ir = iri.getReference();
                beanInterfaceDef = InterfaceDefHelper.narrow(ir.lookup_id(beanRepositoryIds[0]));
            }

            // Instantiate the ejb object servant and bind it to the servant registry.
            final EjbCorbaServant beanServant = new EjbCorbaServant(poaCurrent, beanMethodMap, beanRepositoryIds,
                    beanInterfaceDef, orb, remoteView.getValue(), factory, configuration, component.getTransactionManager(),
                    module.getClassLoader(), false, securityDomain);
            beanReferenceFactory = beanServantRegistry.bind(beanServantName(name), beanServant, policies);

            // Register bean home in local CORBA naming context
            rebind(corbaNamingContext.getValue(), name, corbaRef);
            logger.debug("Home IOR for " + component.getComponentName() + " bound to " + this.name + " in CORBA naming service");
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        // Get local (in-VM) CORBA naming context
        final NamingContextExt corbaContext = corbaNamingContext.getValue();

        // Unregister bean home from local CORBA naming context
        try {
            NameComponent[] name = corbaContext.to_name(this.name);
            corbaContext.unbind(name);
        } catch (InvalidName invalidName) {
            logger.error("Cannot unregister EJBHome from CORBA naming service", invalidName);
        } catch (NotFound notFound) {
            logger.error("Cannot unregister EJBHome from CORBA naming service", notFound);
        } catch (CannotProceed cannotProceed) {
            logger.error("Cannot unregister EJBHome from CORBA naming service", cannotProceed);
        }

        // Deactivate the home servant and the bean servant
        try {
            homeServantRegistry.unbind(homeServantName(this.name));
        } catch (Exception e) {
            logger.error("Cannot deactivate home servant", e);
        }
        try {
            beanServantRegistry.unbind(beanServantName(this.name));
        } catch (Exception e) {
            logger.error("Cannot deactivate bean servant", e);
        }

        if (iri != null) {
            // Deactivate the interface repository
            iri.shutdown();
        }
        this.name = null;
    }

    /**
     * Returns a corba reference for the given locator
     *
     * @param locator The locator
     * @return The corba reference
     */
    public Object referenceForLocator(final EJBLocator<?> locator) {
        final EJBComponent ejbComponent = ejbComponentInjectedValue.getValue();
        try {
            final String earApplicationName = ejbComponent.getEarApplicationName() == null ? "" : ejbComponent.getEarApplicationName();
            if (locator.getBeanName().equals(ejbComponent.getComponentName()) &&
                    locator.getAppName().equals(earApplicationName) &&
                    locator.getModuleName().equals(ejbComponent.getModuleName()) &&
                    locator.getDistinctName().equals(ejbComponent.getDistinctName())) {
                if (locator instanceof EJBHomeLocator) {
                    return ejbHome;
                } else if (locator instanceof StatelessEJBLocator) {
                    return beanReferenceFactory.createReference(beanRepositoryIds[0]);
                } else if (locator instanceof StatefulEJBLocator) {
                    final Marshaller marshaller = factory.createMarshaller(configuration);
                    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    marshaller.start(new OutputStreamByteOutput(stream));
                    marshaller.writeObject(((StatefulEJBLocator) locator).getSessionId());
                    marshaller.finish();
                    return beanReferenceFactory.createReferenceWithId(stream.toByteArray(), beanRepositoryIds[0]);
                } else if (locator instanceof EntityEJBLocator) {
                    final Marshaller marshaller = factory.createMarshaller(configuration);
                    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    marshaller.start(new OutputStreamByteOutput(stream));
                    marshaller.writeObject(((EntityEJBLocator) locator).getPrimaryKey());
                    marshaller.finish();
                    return beanReferenceFactory.createReferenceWithId(stream.toByteArray(), beanRepositoryIds[0]);
                }
                throw new RuntimeException("Unkown locator type " + locator);
            } else {
                throw new IllegalArgumentException("Provided locator " + locator + " was not for EJB " + ejbComponent);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not create CORBA Object for " + locator + " for EJB " + ejbComponent, e);
        }
    }

    /**
     * (Re)binds an object to a name in a given CORBA naming context, creating
     * any non-existent intermediate contexts along the way.
     * <p/>
     * This method is synchronized on the class object, if multiple services attempt to bind the
     * same context name at once it will fail
     *
     * @param ctx a reference to the COSNaming service.
     * @param strName the name under which the CORBA object is to be bound.
     * @param obj the CORBA object to be bound.
     * @throws Exception if an error occurs while binding the object.
     */
    public static synchronized void rebind(final NamingContextExt ctx, final String strName, final org.omg.CORBA.Object obj) throws Exception {
        final NameComponent[] name = ctx.to_name(strName);
        NamingContext intermediateCtx = ctx;

        for (int i = 0; i < name.length - 1; i++) {
            final NameComponent[] relativeName = new NameComponent[]{name[i]};
            try {
                intermediateCtx = NamingContextHelper.narrow(
                        intermediateCtx.resolve(relativeName));
            } catch (NotFound e) {
                intermediateCtx = intermediateCtx.bind_new_context(relativeName);
            }
        }
        intermediateCtx.rebind(new NameComponent[]{name[name.length - 1]}, obj);
    }

    /**
     * Returns the name of a home servant for an EJB with the given jndiName.
     * The home servant will be bound to this name in a ServantRegistry.
     */
    private static String homeServantName(final String jndiName) {
        return jndiName + "/home";
    }

    /**
     * Returns the name of a bean servant for an EJB with the given jndiName.
     * The bean servant will be bound to this name in a ServantRegistry.
     */
    private static String beanServantName(final String jndiName) {
        return jndiName + "/remote";
    }

    @Override
    public EjbIIOPService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<ComponentView> getRemoteView() {
        return remoteView;
    }

    public InjectedValue<ComponentView> getHomeView() {
        return homeView;
    }

    public InjectedValue<EJBComponent> getEjbComponentInjectedValue() {
        return ejbComponentInjectedValue;
    }

    public InjectedValue<ORB> getOrb() {
        return orb;
    }

    public InjectedValue<NamingContextExt> getCorbaNamingContext() {
        return corbaNamingContext;
    }

    public InjectedValue<POARegistry> getPoaRegistry() {
        return poaRegistry;
    }

    public InjectedValue<POA> getIrPoa() {
        return irPoa;
    }

    public InjectedValue<ServiceModuleLoader> getServiceModuleLoaderInjectedValue() {
        return serviceModuleLoaderInjectedValue;
    }
}
