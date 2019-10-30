package org.jboss.as.web.common;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;

import javax.el.ExpressionFactory;
import javax.servlet.ServletContext;

/**
 * @author Stuart Douglas
 */
public interface ExpressionFactoryWrapper {

    AttachmentKey<AttachmentList<ExpressionFactoryWrapper>> ATTACHMENT_KEY = AttachmentKey.createList(ExpressionFactoryWrapper.class);

    ExpressionFactory wrap(ExpressionFactory expressionFactory, ServletContext servletContext);

}
