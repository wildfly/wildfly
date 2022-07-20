package org.jboss.as.test.integration.web.handlestypes;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HandlesTypes;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author Stuart Douglas
 */
@HandlesTypes({SomeAnnotation.class})
public class AnnotationServletContainerInitializer implements ServletContainerInitializer {

    public static final Set<Class<?>> HANDLES_TYPES = new CopyOnWriteArraySet<>();

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        HANDLES_TYPES.addAll(c);
    }
}
