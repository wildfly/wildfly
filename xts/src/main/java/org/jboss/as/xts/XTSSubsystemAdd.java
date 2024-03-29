/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.xts;

import static org.jboss.as.xts.XTSSubsystemDefinition.DEFAULT_CONTEXT_PROPAGATION;
import static org.jboss.as.xts.XTSSubsystemDefinition.ENVIRONMENT_URL;
import static org.jboss.as.xts.XTSSubsystemDefinition.HOST_NAME;
import static org.jboss.as.xts.XTSSubsystemDefinition.ASYNC_REGISTRATION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.arjuna.schemas.ws._2005._10.wsarjtx.TerminationCoordinatorRPCService;
import com.arjuna.schemas.ws._2005._10.wsarjtx.TerminationCoordinatorService;
import com.arjuna.schemas.ws._2005._10.wsarjtx.TerminationParticipantService;
import com.arjuna.webservices11.wsarjtx.sei.TerminationCoordinatorPortTypeImpl;
import com.arjuna.webservices11.wsarjtx.sei.TerminationCoordinatorRPCPortTypeImpl;
import com.arjuna.webservices11.wsarjtx.sei.TerminationParticipantPortTypeImpl;
import com.arjuna.webservices11.wsat.sei.CompletionCoordinatorPortTypeImpl;
import com.arjuna.webservices11.wsat.sei.CompletionCoordinatorRPCPortTypeImpl;
import com.arjuna.webservices11.wsat.sei.CompletionInitiatorPortTypeImpl;
import com.arjuna.webservices11.wsat.sei.CoordinatorPortTypeImpl;
import com.arjuna.webservices11.wsat.sei.ParticipantPortTypeImpl;
import com.arjuna.webservices11.wsba.sei.BusinessAgreementWithCoordinatorCompletionCoordinatorPortTypeImpl;
import com.arjuna.webservices11.wsba.sei.BusinessAgreementWithCoordinatorCompletionParticipantPortTypeImpl;
import com.arjuna.webservices11.wsba.sei.BusinessAgreementWithParticipantCompletionCoordinatorPortTypeImpl;
import com.arjuna.webservices11.wsba.sei.BusinessAgreementWithParticipantCompletionParticipantPortTypeImpl;
import com.arjuna.webservices11.wscoor.sei.ActivationPortTypeImpl;
import com.arjuna.webservices11.wscoor.sei.CoordinationFaultPortTypeImpl;
import com.arjuna.webservices11.wscoor.sei.RegistrationPortTypeImpl;
import com.arjuna.webservices11.wscoor.sei.RegistrationResponsePortTypeImpl;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.as.webservices.service.EndpointPublishService;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.as.xts.logging.XtsAsLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.jbossts.XTSService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.wsf.spi.invocation.RejectionRule;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.publish.Context;
import org.oasis_open.docs.ws_tx.wsat._2006._06.CompletionCoordinatorRPCService;
import org.oasis_open.docs.ws_tx.wsat._2006._06.CompletionCoordinatorService;
import org.oasis_open.docs.ws_tx.wsat._2006._06.CompletionInitiatorService;
import org.oasis_open.docs.ws_tx.wsat._2006._06.CoordinatorService;
import org.oasis_open.docs.ws_tx.wsat._2006._06.ParticipantService;
import org.oasis_open.docs.ws_tx.wsba._2006._06.BusinessAgreementWithCoordinatorCompletionCoordinatorService;
import org.oasis_open.docs.ws_tx.wsba._2006._06.BusinessAgreementWithCoordinatorCompletionParticipantService;
import org.oasis_open.docs.ws_tx.wsba._2006._06.BusinessAgreementWithParticipantCompletionCoordinatorService;
import org.oasis_open.docs.ws_tx.wsba._2006._06.BusinessAgreementWithParticipantCompletionParticipantService;
import org.oasis_open.docs.ws_tx.wscoor._2006._06.ActivationService;
import org.oasis_open.docs.ws_tx.wscoor._2006._06.CoordinationFaultService;
import org.oasis_open.docs.ws_tx.wscoor._2006._06.RegistrationResponseService;
import org.oasis_open.docs.ws_tx.wscoor._2006._06.RegistrationService;


/**
 * Adds the transaction management subsystem.
 *
 * @author <a href="mailto:adinn@redhat.com">Andrew Dinn</a>
 */
class XTSSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final XTSSubsystemAdd INSTANCE = new XTSSubsystemAdd();

    private static final String WSAT_ASYNC_REGISTRATION_PARAM_NAME = "wsat.async.registration";

    /**
     * class used to record the url pattern and service endpoint implementation class name of
     * an XTS JaxWS endpoint associated with one of the XTS context paths. this is equivalent
     * to the information contained in a single matched pair of servlet:servletName and
     * servlet-mapping:url-pattern fields in the web.xml
     */
    private static class EndpointInfo {
        String SEIClassname;
        String URLPattern;

        EndpointInfo(String seiClassname, String urlPattern) {
            this.SEIClassname = seiClassname;
            this.URLPattern = urlPattern;
        }
    }

    /**
     * class grouping togeher details of all XTS JaxWS endpoints associated with a given XTS context
     * path. this groups together all the paired servlet:servletName and* servlet-mapping:url-pattern
     * fields in the web.xml
     */
    static class ContextInfo {
        String contextPath;
        EndpointInfo[] endpointInfo;

        ContextInfo(String contextPath, EndpointInfo[] endpointInfo) {
            this.contextPath = contextPath;
            this.endpointInfo = endpointInfo;
        }
    }

    /**
     * a collection of all the context and associated endpoint information for the XTS JaxWS endpoints.
     * this is the bits of the variosu web.xml files which are necessary to deploy via the endpoint
     * publisher API rather than via war files containing web.xml descriptors
     */
    private static final ContextInfo[] contextDefinitions = {
            // ContextInfo ws-c11 filled at method getContextDefinitions
            new ContextInfo("ws-t11-coordinator",
                    new EndpointInfo[]{
                            new EndpointInfo(CoordinatorPortTypeImpl.class.getName(), CoordinatorService.class.getSimpleName()),
                            new EndpointInfo(CompletionCoordinatorPortTypeImpl.class.getName(), CompletionCoordinatorService.class.getSimpleName()),
                            new EndpointInfo(CompletionCoordinatorRPCPortTypeImpl.class.getName(), CompletionCoordinatorRPCService.class.getSimpleName()),
                            new EndpointInfo(BusinessAgreementWithCoordinatorCompletionCoordinatorPortTypeImpl.class.getName(), BusinessAgreementWithCoordinatorCompletionCoordinatorService.class.getSimpleName()),
                            new EndpointInfo(BusinessAgreementWithParticipantCompletionCoordinatorPortTypeImpl.class.getName(), BusinessAgreementWithParticipantCompletionCoordinatorService.class.getSimpleName()),
                            new EndpointInfo(TerminationCoordinatorPortTypeImpl.class.getName(), TerminationCoordinatorService.class.getSimpleName()),
                            new EndpointInfo(TerminationCoordinatorRPCPortTypeImpl.class.getName(), TerminationCoordinatorRPCService.class.getSimpleName())
                    }),
            new ContextInfo("ws-t11-participant",
                    new EndpointInfo[]{
                            new EndpointInfo(ParticipantPortTypeImpl.class.getName(), ParticipantService.class.getSimpleName()),
                            new EndpointInfo(BusinessAgreementWithCoordinatorCompletionParticipantPortTypeImpl.class.getName(), BusinessAgreementWithCoordinatorCompletionParticipantService.class.getSimpleName()),
                            new EndpointInfo(BusinessAgreementWithParticipantCompletionParticipantPortTypeImpl.class.getName(), BusinessAgreementWithParticipantCompletionParticipantService.class.getSimpleName()),
                    }),
            new ContextInfo("ws-t11-client",
                    new EndpointInfo[]{
                            new EndpointInfo(CompletionInitiatorPortTypeImpl.class.getName(), CompletionInitiatorService.class.getSimpleName()),
                            new EndpointInfo(TerminationParticipantPortTypeImpl.class.getName(), TerminationParticipantService.class.getSimpleName())
                    })
    };

    private static final String WS_C11_CONTEXT_DEFINITION_NAME = "ws-c11";
    static final EndpointInfo[] wsC11 = new EndpointInfo[] {
            new EndpointInfo(ActivationPortTypeImpl.class.getName(), ActivationService.class.getSimpleName()),
            new EndpointInfo(RegistrationPortTypeImpl.class.getName(), RegistrationService.class.getSimpleName())
    };
    static final EndpointInfo[] wsC11Async = new EndpointInfo[] {
            new EndpointInfo(RegistrationResponsePortTypeImpl.class.getName(), RegistrationResponseService.class.getSimpleName()),
            new EndpointInfo(CoordinationFaultPortTypeImpl.class.getName(), CoordinationFaultService.class.getSimpleName())
    };

    private XTSSubsystemAdd() {
    }

    static Iterable<ContextInfo> getContextDefinitions(OperationContext context, ModelNode model) throws IllegalArgumentException, OperationFailedException {
        Collection<ContextInfo> updatedContextDefinitions = new ArrayList<>(Arrays.asList(contextDefinitions));
        Collection<EndpointInfo> wsC11EndpointInfos = new ArrayList<>(Arrays.asList(wsC11));
        if(ASYNC_REGISTRATION.resolveModelAttribute(context, model).asBoolean() || Boolean.getBoolean(XTSSubsystemAdd.WSAT_ASYNC_REGISTRATION_PARAM_NAME)) {
            wsC11EndpointInfos.addAll(Arrays.asList(wsC11Async));
        }

        ContextInfo wsC11ContextInfo = new ContextInfo(WS_C11_CONTEXT_DEFINITION_NAME, wsC11EndpointInfos.toArray(new EndpointInfo[wsC11EndpointInfos.size()]));
        updatedContextDefinitions.add(wsC11ContextInfo);

        return updatedContextDefinitions;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        HOST_NAME.validateAndSet(operation, model);
        ENVIRONMENT_URL.validateAndSet(operation, model);
        DEFAULT_CONTEXT_PROPAGATION.validateAndSet(operation, model);
        ASYNC_REGISTRATION.validateAndSet(operation, model);
    }


    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String hostName = HOST_NAME.resolveModelAttribute(context, model).asString();

        final ModelNode coordinatorURLAttribute = ENVIRONMENT_URL.resolveModelAttribute(context, model);
        String coordinatorURL = coordinatorURLAttribute.isDefined() ? coordinatorURLAttribute.asString() : null;

        // formatting possible IPv6 address to contain square brackets
        if (coordinatorURL != null) {
            // [1] http://, [2] ::1, [3] 8080, [4] /ws-c11/ActivationService
            Pattern urlPattern = Pattern.compile("^([a-zA-Z]+://)(.*):([^/]*)(/.*)$");
            Matcher urlMatcher = urlPattern.matcher(coordinatorURL);
            if(urlMatcher.matches()) {
                String address = NetworkUtils.formatPossibleIpv6Address(urlMatcher.group(2));
                coordinatorURL = String.format("%s%s:%s%s", urlMatcher.group(1), address, urlMatcher.group(3), urlMatcher.group(4));
            }
        }

        if (coordinatorURL != null) {
            XtsAsLogger.ROOT_LOGGER.debugf("nodeIdentifier=%s%n", coordinatorURL);
        }

        final boolean isDefaultContextPropagation = DEFAULT_CONTEXT_PROPAGATION.resolveModelAttribute(context, model).asBoolean(false); // TODO WFLY-14350 make the 'false' the default value of DEFAULT_CONTEXT_PROPAGATION

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(XTSExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_XTS_SOAP_HANDLERS, new XTSHandlerDeploymentProcessor());
                processorTarget.addDeploymentProcessor(XTSExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_XTS, new XTSDependenciesDeploymentProcessor());
                processorTarget.addDeploymentProcessor(XTSExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_XTS_PORTABLE_EXTENSIONS, new GracefulShutdownDeploymentProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        final ServiceTarget target = context.getServiceTarget();

        // TODO eventually we should add a config service which manages the XTS configuration
        // this will allow us to include a switch enabling or disabling deployment of
        // endpoints specific to client, coordinator or participant and then deploy
        // and redeploy the relevant endpoints as needed/ the same switches can be used
        // byte the XTS service to decide whether to perfomr client, coordinator or
        // participant initialisation. we should also provide config switches which
        // decide whether to initialise classes and deploy services for AT, BA or both.
        // for now we will just deploy all the endpoints and always do client, coordinator
        // and participant init for both AT and BA.

        // add an endpoint publisher service for each of the required endpoint contexts
        // specifying all the relevant URL patterns and SEI classes

        final ClassLoader loader = XTSService.class.getClassLoader();
        ServiceBuilder<Context> endpointBuilder;
        ArrayList<ServiceController<Context>> controllers = new ArrayList<ServiceController<Context>>();
        Map<Class<?>, Object> attachments = new HashMap<>();
        attachments.put(RejectionRule.class, new GracefulShutdownRejectionRule());
        for (ContextInfo contextInfo : getContextDefinitions(context, model)) {
            String contextName = contextInfo.contextPath;
            Map<String, String> map = new HashMap<String, String>();
            for (EndpointInfo endpointInfo : contextInfo.endpointInfo) {
                map.put(endpointInfo.URLPattern, endpointInfo.SEIClassname);
            }
            endpointBuilder = EndpointPublishService.createServiceBuilder(target, contextName, loader, hostName, map, null,
                    null, null, attachments, context.getCapabilityServiceSupport());

            controllers.add(endpointBuilder.setInitialMode(Mode.ACTIVE)
                    .install());
        }

        XTSHandlersService.install(target, isDefaultContextPropagation);

        // add an XTS service which depends on all the WS endpoints

        final XTSManagerService xtsService = new XTSManagerService(coordinatorURL);

        // this service needs to depend on the transaction recovery service
        // because it can only initialise XTS recovery once the transaction recovery
        // service has initialised the orb layer

        ServiceBuilder<?> xtsServiceBuilder = target.addService(XTSServices.JBOSS_XTS_MAIN, xtsService);
        xtsServiceBuilder.requires(TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER);

        // this service needs to depend on JBossWS Config Service to be notified of the JBoss WS config (bind address, port etc)
        xtsServiceBuilder.addDependency(WSServices.CONFIG_SERVICE, ServerConfig.class, xtsService.getWSServerConfig());
        xtsServiceBuilder.requires(WSServices.XTS_CLIENT_INTEGRATION_SERVICE);

        // the service also needs to depend on the endpoint services
        for (ServiceController<Context> controller : controllers) {
            xtsServiceBuilder.requires(controller.getName());
        }

        xtsServiceBuilder
                .setInitialMode(Mode.ACTIVE)
                .install();

        // WS-AT / Jakarta Transactions Transaction bridge services:

        final TxBridgeInboundRecoveryService txBridgeInboundRecoveryService = new TxBridgeInboundRecoveryService();
        ServiceBuilder<?> txBridgeInboundRecoveryServiceBuilder =
                target.addService(XTSServices.JBOSS_XTS_TXBRIDGE_INBOUND_RECOVERY, txBridgeInboundRecoveryService);
        txBridgeInboundRecoveryServiceBuilder.requires(XTSServices.JBOSS_XTS_MAIN);

        txBridgeInboundRecoveryServiceBuilder.setInitialMode(Mode.ACTIVE).install();

        final TxBridgeOutboundRecoveryService txBridgeOutboundRecoveryService = new TxBridgeOutboundRecoveryService();
        ServiceBuilder<?> txBridgeOutboundRecoveryServiceBuilder =
                target.addService(XTSServices.JBOSS_XTS_TXBRIDGE_OUTBOUND_RECOVERY, txBridgeOutboundRecoveryService);
        txBridgeOutboundRecoveryServiceBuilder.requires(XTSServices.JBOSS_XTS_MAIN);

        txBridgeOutboundRecoveryServiceBuilder.setInitialMode(Mode.ACTIVE).install();

    }
}
