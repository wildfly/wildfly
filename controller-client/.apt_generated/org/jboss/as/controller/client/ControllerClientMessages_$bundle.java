
package org.jboss.as.controller.client;

import java.io.Serializable;
import javax.annotation.Generated;
import org.jboss.as.controller.client.helpers.domain.DeploymentAction.Type;


/**
 * Warning this class consists of generated code.
 * 
 */
@Generated(value = "org.jboss.logging.processor.generator.model.MessageBundleImplementor", date = "2013-10-24T15:58:26+0100")
public class ControllerClientMessages_$bundle
    implements Serializable, ControllerClientMessages
{

    private final static long serialVersionUID = 1L;
    public final static ControllerClientMessages_$bundle INSTANCE = new ControllerClientMessages_$bundle();
    private final static java.lang.String incompleteDeploymentReplace = "JBAS010630: Only one version of deployment with a given unique name can exist in the domain. The deployment plan specified that a new version of deployment %s replace an existing deployment with the same unique name, but did not apply the replacement to all server groups. Missing server groups were: %s";
    private final static java.lang.String rollbackCancelled = "JBAS010644: Rollback was cancelled";
    private final static java.lang.String domainDeploymentAlreadyExists = "JBAS010626: Deployment with name %s already present in the domain";
    private final static java.lang.String invalidActionType = "JBAS010631: Invalid action type %s";
    private final static java.lang.String noFailureDetails = "JBAS010638: No failure details provided";
    private final static java.lang.String cannotUseDeploymentPlan = "JBAS010624: Cannot use a DeploymentPlan not created by this manager";
    private final static java.lang.String cannotAddDeploymentActionsAfterStart = "JBAS010621: Cannot add deployment actions after starting creation of a rollout plan";
    private final static java.lang.String invalidPrecedingAction = "JBAS010632: Preceding action was not a %s";
    private final static java.lang.String notConfigured = "JBAS010639: No %s is configured";
    private final static java.lang.String failed = "JBAS010627: failed";
    private final static java.lang.String rollbackTimedOut = "JBAS010646: Rollback timed out";
    private final static java.lang.String objectIsClosed = "JBAS010641: %s is closed";
    private final static java.lang.String cannotConvert = "JBAS010622: Cannot convert %s to %s";
    private final static java.lang.String nullVar = "JBAS010640: %s is null";
    private final static java.lang.String rollbackRolledBack = "JBAS010645: Rollback was itself rolled back";
    private final static java.lang.String invalidValue3 = "JBAS010634: Illegal %s value %d -- must be greater than %d";
    private final static java.lang.String cannotDeriveDeploymentName = "JBAS010623: Cannot derive a deployment name from %s -- use an overloaded method variant that takes a 'name' parameter";
    private final static java.lang.String noFailureDescription = "JBAS010650: No failure description as the operation was successful.";
    private final static java.lang.String maxDisplayUnitLength = "JBAS010636: Screen real estate is expensive; displayUnits must be 5 characters or less";
    private final static java.lang.String globalRollbackNotCompatible = "JBAS010628: Global rollback is not compatible with a server restart";
    private final static java.lang.String operationOutcome = "JBAS010642: Operation outcome is %s";
    private final static java.lang.String gracefulShutdownAlreadyConfigured = "JBAS010629: Graceful shutdown already configured with a timeout of %d ms";
    private final static java.lang.String invalidUri = "JBAS010633: %s is not a valid URI";
    private final static java.lang.String controllerClientNotClosed = "JBAS010649: Allocation stack trace:";
    private final static java.lang.String cannotAddDeploymentAction = "JBAS010620: Cannot add deployment actions after starting creation of a rollout plan";
    private final static java.lang.String unknownActionType = "JBAS010648: Unknown action type %s";
    private final static java.lang.String channelClosed = "JBAS010625: Channel closed";
    private final static java.lang.String noActiveRequest = "JBAS010637: No active request found for %d";
    private final static java.lang.String operationNameNotFound = "JBAS010651: The operation name was not defined.";
    private final static java.lang.String operationsNotAllowed = "JBAS010643: %s operations are not allowed after content and deployment modifications";
    private final static java.lang.String invalidValue4 = "JBAS010635: Illegal %s value %d -- must be greater than %d and less than %d";
    private final static java.lang.String serverDeploymentAlreadyExists = "JBAS010647: Deployment with name %s already present in the server";
    private final static java.lang.String invalidAddressType = "JBAS010652: The address must be of type ModelType.LIST.";

    protected ControllerClientMessages_$bundle() {
    }

    protected java.lang.Object readResolve() {
        return INSTANCE;
    }

    public final java.lang.String incompleteDeploymentReplace(final java.lang.String deploymentName, final java.lang.String missingGroups) {
        java.lang.String result = java.lang.String.format(incompleteDeploymentReplace$str(), deploymentName, missingGroups);
        return result;
    }

    protected java.lang.String incompleteDeploymentReplace$str() {
        return incompleteDeploymentReplace;
    }

    public final org.jboss.as.controller.client.helpers.domain.RollbackCancelledException rollbackCancelled() {
        org.jboss.as.controller.client.helpers.domain.RollbackCancelledException result = new org.jboss.as.controller.client.helpers.domain.RollbackCancelledException(java.lang.String.format(rollbackCancelled$str()));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String rollbackCancelled$str() {
        return rollbackCancelled;
    }

    public final java.lang.String domainDeploymentAlreadyExists(final java.lang.String name) {
        java.lang.String result = java.lang.String.format(domainDeploymentAlreadyExists$str(), name);
        return result;
    }

    protected java.lang.String domainDeploymentAlreadyExists$str() {
        return domainDeploymentAlreadyExists;
    }

    public final java.lang.IllegalStateException invalidActionType(final Type type) {
        java.lang.IllegalStateException result = new java.lang.IllegalStateException(java.lang.String.format(invalidActionType$str(), type));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String invalidActionType$str() {
        return invalidActionType;
    }

    public final java.lang.String noFailureDetails() {
        java.lang.String result = java.lang.String.format(noFailureDetails$str());
        return result;
    }

    protected java.lang.String noFailureDetails$str() {
        return noFailureDetails;
    }

    public final java.lang.IllegalArgumentException cannotUseDeploymentPlan() {
        java.lang.IllegalArgumentException result = new java.lang.IllegalArgumentException(java.lang.String.format(cannotUseDeploymentPlan$str()));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String cannotUseDeploymentPlan$str() {
        return cannotUseDeploymentPlan;
    }

    public final java.lang.IllegalStateException cannotAddDeploymentActionsAfterStart() {
        java.lang.IllegalStateException result = new java.lang.IllegalStateException(java.lang.String.format(cannotAddDeploymentActionsAfterStart$str()));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String cannotAddDeploymentActionsAfterStart$str() {
        return cannotAddDeploymentActionsAfterStart;
    }

    public final java.lang.IllegalStateException invalidPrecedingAction(final java.lang.Object type) {
        java.lang.IllegalStateException result = new java.lang.IllegalStateException(java.lang.String.format(invalidPrecedingAction$str(), type));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String invalidPrecedingAction$str() {
        return invalidPrecedingAction;
    }

    public final java.lang.IllegalStateException notConfigured(final java.lang.String name) {
        java.lang.IllegalStateException result = new java.lang.IllegalStateException(java.lang.String.format(notConfigured$str(), name));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String notConfigured$str() {
        return notConfigured;
    }

    public final java.lang.String failed() {
        java.lang.String result = java.lang.String.format(failed$str());
        return result;
    }

    protected java.lang.String failed$str() {
        return failed;
    }

    public final org.jboss.as.controller.client.helpers.domain.RollbackCancelledException rollbackTimedOut() {
        org.jboss.as.controller.client.helpers.domain.RollbackCancelledException result = new org.jboss.as.controller.client.helpers.domain.RollbackCancelledException(java.lang.String.format(rollbackTimedOut$str()));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String rollbackTimedOut$str() {
        return rollbackTimedOut;
    }

    public final java.lang.IllegalStateException objectIsClosed(final java.lang.String name) {
        java.lang.IllegalStateException result = new java.lang.IllegalStateException(java.lang.String.format(objectIsClosed$str(), name));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String objectIsClosed$str() {
        return objectIsClosed;
    }

    public final java.lang.String cannotConvert(final java.lang.String first, final java.lang.String second) {
        java.lang.String result = java.lang.String.format(cannotConvert$str(), first, second);
        return result;
    }

    protected java.lang.String cannotConvert$str() {
        return cannotConvert;
    }

    public final java.lang.IllegalArgumentException nullVar(final java.lang.String name) {
        java.lang.IllegalArgumentException result = new java.lang.IllegalArgumentException(java.lang.String.format(nullVar$str(), name));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String nullVar$str() {
        return nullVar;
    }

    public final org.jboss.as.controller.client.helpers.domain.RollbackCancelledException rollbackRolledBack() {
        org.jboss.as.controller.client.helpers.domain.RollbackCancelledException result = new org.jboss.as.controller.client.helpers.domain.RollbackCancelledException(java.lang.String.format(rollbackRolledBack$str()));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String rollbackRolledBack$str() {
        return rollbackRolledBack;
    }

    public final java.lang.IllegalArgumentException invalidValue(final java.lang.String name, final int value, final int minValue) {
        java.lang.IllegalArgumentException result = new java.lang.IllegalArgumentException(java.lang.String.format(invalidValue3$str(), name, value, minValue));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String invalidValue3$str() {
        return invalidValue3;
    }

    public final java.lang.IllegalArgumentException cannotDeriveDeploymentName(final java.net.URL url) {
        java.lang.IllegalArgumentException result = new java.lang.IllegalArgumentException(java.lang.String.format(cannotDeriveDeploymentName$str(), url));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String cannotDeriveDeploymentName$str() {
        return cannotDeriveDeploymentName;
    }

    public final java.lang.IllegalArgumentException noFailureDescription() {
        java.lang.IllegalArgumentException result = new java.lang.IllegalArgumentException(java.lang.String.format(noFailureDescription$str()));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String noFailureDescription$str() {
        return noFailureDescription;
    }

    public final java.lang.RuntimeException maxDisplayUnitLength() {
        java.lang.RuntimeException result = new java.lang.RuntimeException(java.lang.String.format(maxDisplayUnitLength$str()));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String maxDisplayUnitLength$str() {
        return maxDisplayUnitLength;
    }

    public final java.lang.IllegalStateException globalRollbackNotCompatible() {
        java.lang.IllegalStateException result = new java.lang.IllegalStateException(java.lang.String.format(globalRollbackNotCompatible$str()));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String globalRollbackNotCompatible$str() {
        return globalRollbackNotCompatible;
    }

    public final java.lang.RuntimeException operationOutcome(final java.lang.String outcome) {
        java.lang.RuntimeException result = new java.lang.RuntimeException(java.lang.String.format(operationOutcome$str(), outcome));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String operationOutcome$str() {
        return operationOutcome;
    }

    public final java.lang.IllegalStateException gracefulShutdownAlreadyConfigured(final long timeout) {
        java.lang.IllegalStateException result = new java.lang.IllegalStateException(java.lang.String.format(gracefulShutdownAlreadyConfigured$str(), timeout));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String gracefulShutdownAlreadyConfigured$str() {
        return gracefulShutdownAlreadyConfigured;
    }

    public final java.lang.IllegalArgumentException invalidUri(final Throwable cause, final java.net.URL url) {
        java.lang.IllegalArgumentException result = new java.lang.IllegalArgumentException(java.lang.String.format(invalidUri$str(), url), cause);
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String invalidUri$str() {
        return invalidUri;
    }

    public final org.jboss.as.controller.client.ControllerClientMessages.LeakDescription controllerClientNotClosed() {
        org.jboss.as.controller.client.ControllerClientMessages.LeakDescription result = new org.jboss.as.controller.client.ControllerClientMessages.LeakDescription(java.lang.String.format(controllerClientNotClosed$str()));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String controllerClientNotClosed$str() {
        return controllerClientNotClosed;
    }

    public final java.lang.IllegalStateException cannotAddDeploymentAction() {
        java.lang.IllegalStateException result = new java.lang.IllegalStateException(java.lang.String.format(cannotAddDeploymentAction$str()));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String cannotAddDeploymentAction$str() {
        return cannotAddDeploymentAction;
    }

    public final java.lang.IllegalStateException unknownActionType(final java.lang.Object type) {
        java.lang.IllegalStateException result = new java.lang.IllegalStateException(java.lang.String.format(unknownActionType$str(), type));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String unknownActionType$str() {
        return unknownActionType;
    }

    public final java.io.IOException channelClosed(final java.io.IOException cause) {
        java.io.IOException result = new java.io.IOException(java.lang.String.format(channelClosed$str()), cause);
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String channelClosed$str() {
        return channelClosed;
    }

    public final java.io.IOException noActiveRequest(final int batchId) {
        java.io.IOException result = new java.io.IOException(java.lang.String.format(noActiveRequest$str(), batchId));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String noActiveRequest$str() {
        return noActiveRequest;
    }

    public final java.lang.IllegalArgumentException operationNameNotFound() {
        java.lang.IllegalArgumentException result = new java.lang.IllegalArgumentException(java.lang.String.format(operationNameNotFound$str()));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String operationNameNotFound$str() {
        return operationNameNotFound;
    }

    public final java.lang.IllegalStateException operationsNotAllowed(final java.lang.String name) {
        java.lang.IllegalStateException result = new java.lang.IllegalStateException(java.lang.String.format(operationsNotAllowed$str(), name));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String operationsNotAllowed$str() {
        return operationsNotAllowed;
    }

    public final java.lang.IllegalArgumentException invalidValue(final java.lang.String name, final int value, final int minValue, final int maxValue) {
        java.lang.IllegalArgumentException result = new java.lang.IllegalArgumentException(java.lang.String.format(invalidValue4$str(), name, value, minValue, maxValue));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String invalidValue4$str() {
        return invalidValue4;
    }

    public final java.lang.String serverDeploymentAlreadyExists(final java.lang.String name) {
        java.lang.String result = java.lang.String.format(serverDeploymentAlreadyExists$str(), name);
        return result;
    }

    protected java.lang.String serverDeploymentAlreadyExists$str() {
        return serverDeploymentAlreadyExists;
    }

    public final java.lang.IllegalArgumentException invalidAddressType() {
        java.lang.IllegalArgumentException result = new java.lang.IllegalArgumentException(java.lang.String.format(invalidAddressType$str()));
        java.lang.StackTraceElement[] st = result.getStackTrace();
        result.setStackTrace(java.util.Arrays.copyOfRange(st, 1, st.length));
        return result;
    }

    protected java.lang.String invalidAddressType$str() {
        return invalidAddressType;
    }

}
