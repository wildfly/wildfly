/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mail.basic;


import java.io.IOException;

import jakarta.annotation.Resource;
import jakarta.mail.Address;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.as.test.shared.TimeoutUtil;

/**
 * A servlet that sends an email using SMTP and verifies the message is in the recipient inbox using pop3.
 */
@WebServlet(name = "MailServlet", urlPatterns = { "/mail_test" })
public final class MailTesterServlet extends HttpServlet {

    @Resource(mappedName = "java:jboss/mail/mail-test-basic")
    private Session session;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("user01@james.local"));
            Address toAddress = new InternetAddress("user02@james.local");
            message.addRecipient(Message.RecipientType.TO, toAddress);
            message.setSubject("test email subject");

            final String emailContent = "This is the content of an email sent from user01 to user02";
            message.setContent(emailContent, "text/plain");
            Transport.send(message);

            long endTime = System.currentTimeMillis() + TimeoutUtil.adjust(5000);
            boolean success = false;
            do {
                Thread.sleep(500);
                try {
                    tryToReadEmails(emailContent);
                    return;
                } catch (ServletException e){
                    if (e.getMessage().equals("Message read is not equals to the message sent")){
                        throw e;
                    }
                }
            } while (System.currentTimeMillis() < endTime && !success);
            throw new ServletException("Reached attempt timeout but no messages were received");
        } catch (MessagingException | InterruptedException e) {
            throw new ServletException(e);
        }
    }

    private void tryToReadEmails(String emailContent) throws ServletException, MessagingException, IOException {

        // Read the email using pop3
        Store store = session.getStore("pop3");
        store.connect("user02@james.local", "1234");
        Folder inbox = store.getFolder("Inbox");
        inbox.open(Folder.READ_ONLY);

        // get the list of inbox messages
        Message[] messages = inbox.getMessages();

        if (messages.length == 0) {
            throw new ServletException("no message was found when reading using pop3");
        }

        if (!emailContent.equals(messages[0].getContent().toString().trim())) {
            throw new ServletException("Message read is not equals to the message sent");
        }

    }
}
