/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jsf.logging;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.util.List;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.vfs.VirtualFile;

/**
 * Date: 05.11.2011
 *
 * @author Stuart Douglas
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYJSF", length = 4)
public interface JSFLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    JSFLogger ROOT_LOGGER = Logger.getMessageLogger(JSFLogger.class, "org.jboss.as.jsf");

//    @LogMessage(level = WARN)
//    @Message(id = 1, value = "WildFlyConversationAwareViewHandler was improperly initialized. Expected ViewHandler parent.")
//    void viewHandlerImproperlyInitialized();

//    @LogMessage(level = ERROR)
//    @Message(id = 2, value = "Could not load Jakarta Server Faces managed bean class: %s")
//    void managedBeanLoadFail(String managedBean);

//    @LogMessage(level = ERROR)
//    @Message(id = 3, value = "Jakarta Server Faces managed bean class %s has no default constructor")
//    void managedBeanNoDefaultConstructor(String managedBean);

    @LogMessage(level = ERROR)
    @Message(id = 4, value = "Failed to parse %s, Jakarta Server Faces artifacts defined in this file will not be available")
    void managedBeansConfigParseFailed(VirtualFile facesConfig);

    @LogMessage(level = WARN)
    @Message(id = 5, value = "Unknown Jakarta Server Faces version '%s'.  Default version '%s' will be used instead.")
    void unknownJSFVersion(String version, String defaultVersion);

    @LogMessage(level = WARN)
    @Message(id = 6, value = "Jakarta Server Faces version slot '%s' is missing from module %s")
    void missingJSFModule(String version, String module);

    @LogMessage(level = INFO)
    @Message(id = 7, value = "Activated the following Jakarta Server Faces Implementations: %s")
    void activatedJSFImplementations(List target);

    @Message(id = 8, value = "Failed to load annotated class: %s")
    String classLoadingFailed(DotName clazz);

    @Message(id = 9, value = "Annotation %s in class %s is only allowed on classes")
    String invalidAnnotationLocation(Object annotation, AnnotationTarget classInfo);

//    @Message(id = 10, value = "Instance creation failed")
//    RuntimeException instanceCreationFailed(@Cause Throwable t);
//
//    @Message(id = 11, value = "Instance destruction failed")
//    RuntimeException instanceDestructionFailed(@Cause Throwable t);
//
//    @Message(id = 12, value = "Thread local injection container not set")
//    IllegalStateException noThreadLocalInjectionContainer();

//    @Message(id = 13, value = "@ManagedBean is only allowed at class level %s")
//    String invalidManagedBeanAnnotation(AnnotationTarget target);

    @Message(id = 14, value = "Default Jakarta Server Faces implementation slot '%s' is invalid")
    DeploymentUnitProcessingException invalidDefaultJSFImpl(String defaultJsfVersion);

//    @LogMessage(level = ERROR)
//    @Message(id = 15, value = "Failed to parse %s, phase listeners defined in this file will not be available")
//    void phaseListenersConfigParseFailed(VirtualFile facesConfig);

    @Message(id = 16, value = "Failed to inject Jakarta Server Faces from slot %s")
    DeploymentUnitProcessingException jsfInjectionFailed(String slotName, @Cause Throwable cause);

    @LogMessage(level = DEBUG)
    @Message(id = 17, value = "Faces 1.2 classes detected. Using org.jboss.as.jsf.injection.weld.legacy.WeldApplicationFactoryLegacy.")
    void loadingJsf12();

    @LogMessage(level = DEBUG)
    @Message(id = 18, value = "Faces 1.2 classes not detected. Using org.jboss.as.jsf.injection.weld.WeldApplicationFactory.")
    void loadingJsf2x();

    @LogMessage(level = INFO)
    @Message(id = 19, value = "Jakarta Server Faces artifact %s with class %s has no default constructor so it will not be considered for injection")
    void jsfArtifactNoDefaultConstructor(String type, String className);

    @LogMessage(level = WARN)
    @Message(id = 20, value = "Lazy bean validation was enabled. This can result in missing @PreDestroy events when distributed web sessions expire.")
    void lazyBeanValidationEnabled();
}
