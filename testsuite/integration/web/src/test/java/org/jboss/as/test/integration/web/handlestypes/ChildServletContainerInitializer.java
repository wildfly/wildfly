package org.jboss.as.test.integration.web.handlestypes;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author Stuart Douglas
 */
@HandlesTypes({HandlesTypesChild.class, HandlesTypesImplementor.class})
public class ChildServletContainerInitializer implements ServletContainerInitializer {

    public static final Set<Class<?>> HANDLES_TYPES = new CopyOnWriteArraySet<>();

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        HANDLES_TYPES.addAll(c);
    }
}
