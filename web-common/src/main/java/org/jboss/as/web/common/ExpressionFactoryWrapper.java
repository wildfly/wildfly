package org.jboss.as.web.common;

import javax.el.ExpressionFactory;
import javax.servlet.ServletContext;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;

/**
 * @author Stuart Douglas
 */
public interface ExpressionFactoryWrapper {

    AttachmentKey<AttachmentList<ExpressionFactoryWrapper>> ATTACHMENT_KEY = AttachmentKey.createList(ExpressionFactoryWrapper.class);

    ExpressionFactory wrap(ExpressionFactory expressionFactory, ServletContext servletContext);

}
