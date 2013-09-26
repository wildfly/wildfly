package org.jboss.as.ee.concurrent.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.concurrent.ConcurrentContext;
import org.jboss.as.ee.concurrent.ConcurrentContextInterceptor;
import org.jboss.as.ee.concurrent.ConcurrentContextSetupAction;
import org.jboss.as.ee.concurrent.handle.ClassLoaderContextHandleFactory;
import org.jboss.as.ee.concurrent.handle.NamingContextHandleFactory;
import org.jboss.as.ee.concurrent.handle.OtherEESetupActionsContextHandleFactory;
import org.jboss.as.ee.concurrent.handle.SecurityContextHandleFactory;
import org.jboss.as.ee.concurrent.handle.TransactionLeakContextHandleFactory;
import org.jboss.as.ee.concurrent.service.ConcurrentContextService;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import javax.transaction.TransactionManager;
import java.util.Collection;

import static org.jboss.as.server.deployment.Attachments.MODULE;

/**
 * The DUP responsible for the base concurrent context configuration setup.
 *
 * @author Eduardo Martins
 */
public class EEConcurrentContextProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if(eeModuleDescription == null) {
            return;
        }
        processModuleDescription(eeModuleDescription, deploymentUnit, phaseContext);
        final Collection<ComponentDescription> componentDescriptions = eeModuleDescription.getComponentDescriptions();
        if (componentDescriptions == null) {
            return;
        }
        for (ComponentDescription componentDescription : componentDescriptions) {
            if (componentDescription.getNamingMode() == ComponentNamingMode.NONE) {
                // skip components without namespace
                continue;
            }
            processComponentDescription(componentDescription, deploymentUnit);
        }
    }

    private void processModuleDescription(final EEModuleDescription moduleDescription, DeploymentUnit deploymentUnit, DeploymentPhaseContext phaseContext) {
        final ConcurrentContext concurrentContext = moduleDescription.getConcurrentContext();
        // setup context
        setupConcurrentContext(concurrentContext, deploymentUnit.getAttachment(MODULE).getClassLoader(), moduleDescription.getNamespaceContextSelector(), moduleDescription.isReadOnlyNamingContexts(), deploymentUnit, phaseContext.getServiceTarget());
        // add setup action for web modules
        final ConcurrentContextSetupAction setupAction = new ConcurrentContextSetupAction(concurrentContext);
        deploymentUnit.getAttachmentList(Attachments.WEB_SETUP_ACTIONS).add(setupAction);
    }

    private void processComponentDescription(final ComponentDescription componentDescription, final DeploymentUnit deploymentUnit) {
        final ComponentConfigurator componentConfigurator = new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                final ConcurrentContext concurrentContext = configuration.getConcurrentContext();
                // setup context
                setupConcurrentContext(concurrentContext, configuration.getModuleClassLoader(), configuration.getNamespaceContextSelector(), componentDescription.getModuleDescription().isReadOnlyNamingContexts(), deploymentUnit, context.getServiceTarget());
                // add the interceptor which manages the concurrent context
                final ConcurrentContextInterceptor interceptor = new ConcurrentContextInterceptor(concurrentContext);
                final InterceptorFactory interceptorFactory = new ImmediateInterceptorFactory(interceptor);
                configuration.addPostConstructInterceptor(interceptorFactory, InterceptorOrder.ComponentPostConstruct.CONCURRENT_CONTEXT);
                configuration.addPreDestroyInterceptor(interceptorFactory, InterceptorOrder.ComponentPreDestroy.CONCURRENT_CONTEXT);
                if (description.isPassivationApplicable()) {
                    configuration.addPrePassivateInterceptor(interceptorFactory, InterceptorOrder.ComponentPassivation.CONCURRENT_CONTEXT);
                    configuration.addPostActivateInterceptor(interceptorFactory, InterceptorOrder.ComponentPassivation.CONCURRENT_CONTEXT);
                }
                configuration.addComponentInterceptor(interceptorFactory, InterceptorOrder.Component.CONCURRENT_CONTEXT, false);
            }
        };
        componentDescription.getConfigurators().add(componentConfigurator);
    }

    private void setupConcurrentContext(ConcurrentContext concurrentContext, ClassLoader moduleClassLoader, NamespaceContextSelector namespaceContextSelector, boolean readOnlyNamingContexts, DeploymentUnit deploymentUnit, ServiceTarget serviceTarget) {
        // add default factories
        concurrentContext.addFactory(new NamingContextHandleFactory(namespaceContextSelector, readOnlyNamingContexts ? null : deploymentUnit.getServiceName()));
        concurrentContext.addFactory(new ClassLoaderContextHandleFactory(moduleClassLoader));
        concurrentContext.addFactory(SecurityContextHandleFactory.INSTANCE);
        concurrentContext.addFactory(new OtherEESetupActionsContextHandleFactory(deploymentUnit.getAttachmentList(Attachments.OTHER_EE_SETUP_ACTIONS)));
        final TransactionLeakContextHandleFactory transactionLeakContextHandleFactory = new TransactionLeakContextHandleFactory();
        concurrentContext.addFactory(transactionLeakContextHandleFactory);

        final ConcurrentContextService service = new ConcurrentContextService(concurrentContext);
        final ServiceName serviceName = ConcurrentServiceNames.getConcurrentContextServiceName(concurrentContext.getApplicationName(), concurrentContext.getModuleName(), concurrentContext.getComponentName());
        serviceTarget.addService(serviceName, service)
                .addDependency(ServiceName.JBOSS.append("txn", "TransactionManager"), TransactionManager.class, transactionLeakContextHandleFactory)
                .install();
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
