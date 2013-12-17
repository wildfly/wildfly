package org.jboss.as.test.integration.logging.perdeploy;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.logging.LoggingExtension;
import org.jboss.as.logging.logmanager.WildFlyLogContextSelector;

/**
 * <strong>NOTE:</strong> This uses a reflection hack to determine the size of the {@link
 * org.jboss.logmanager.LogContext log context} map from a {@link org.jboss.as.logging.logmanager.WildFlyLogContextSelector
 * log context selector}. This is rather fragile with a change to the {@link org.jboss.as.logging.LoggingExtension}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@WebServlet(LogContextHackServlet.SERVLET_URL)
public class LogContextHackServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public static final String SERVLET_URL = "/logContext";

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    private void processRequest(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        try {
            final WildFlyLogContextSelector wildflySelector = getFieldValue(LoggingExtension.class, "CONTEXT_SELECTOR", WildFlyLogContextSelector.class);
            response.getWriter().print(wildflySelector.registeredCount());
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private static <T> T getFieldValue(final Class<?> staticType, final String fieldName, final Class<T> type) throws NoSuchFieldException, IllegalAccessException {
        final Field field = staticType.getDeclaredField(fieldName);
        if (Modifier.isStatic(field.getModifiers()) && type.isAssignableFrom(field.getType())) {
            field.setAccessible(true);
            return type.cast(field.get(null));
        }
        return null;
    }
}
