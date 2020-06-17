/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.connector.operations;

import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.connection.EmailConnection;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.connection.ConnectionHandler;
import org.wso2.carbon.connector.core.exception.ContentBuilderException;
import org.wso2.carbon.connector.exception.EmailConnectionException;
import org.wso2.carbon.connector.exception.InvalidConfigurationException;
import org.wso2.carbon.connector.utils.EmailConstants;
import org.wso2.carbon.connector.utils.EmailUtils;
import org.wso2.carbon.connector.utils.Error;
import org.wso2.carbon.connector.utils.MessageBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import static java.lang.String.format;

/**
 * Sends an email
 */
public class EmailSend extends AbstractConnector {

    private static final String EMAIL_CONNECTOR_HEADER_PREFIX = "EMAIL_CONNECTOR_HEADER";
    private static final String HEADER_NAME_SEPARATOR = ":";

    @Override
    public void connect(MessageContext messageContext) {

        ConnectionHandler handler = ConnectionHandler.getConnectionHandler();
        try {
            String name = EmailUtils.getConnectionName(messageContext);
            EmailConnection connection = (EmailConnection) handler
                    .getConnection(EmailConstants.CONNECTOR_NAME, name);
            sendMessage(messageContext, connection);
            EmailUtils.generateOutput(messageContext, true);
        } catch (EmailConnectionException | ConnectException e) {
            EmailUtils.setErrorsInMessage(messageContext, Error.CONNECTIVITY);
            handleException(e.getMessage(), e, messageContext);
        } catch (InvalidConfigurationException e) {
            EmailUtils.setErrorsInMessage(messageContext, Error.INVALID_CONFIGURATION);
            handleException(e.getMessage(), e, messageContext);
        } catch (ContentBuilderException e) {
            EmailUtils.setErrorsInMessage(messageContext, Error.RESPONSE_GENERATION);
            handleException(e.getMessage(), e, messageContext);
        }
    }

    /**
     * Sends an email
     *
     * @param messageContext The message context that is generated for sending the email
     * @param session        Mail Session to be used
     */
    private void sendMessage(MessageContext messageContext, EmailConnection session) throws EmailConnectionException,
            InvalidConfigurationException {

        String to = (String) getParameter(messageContext, EmailConstants.TO);
        String from = (String) getParameter(messageContext, EmailConstants.FROM);
        String cc = (String) getParameter(messageContext, EmailConstants.CC);
        String bcc = (String) getParameter(messageContext, EmailConstants.BCC);
        String replyTo = (String) getParameter(messageContext, EmailConstants.REPLY_TO);
        String subject = (String) getParameter(messageContext, EmailConstants.SUBJECT);
        String content = (String) getParameter(messageContext, EmailConstants.CONTENT);
        String attachments = (String) getParameter(messageContext, EmailConstants.ATTACHMENTS);
        String contentType = (String) getParameter(messageContext, EmailConstants.CONTENT_TYPE);
        String encoding = (String) getParameter(messageContext, EmailConstants.ENCODING);
        String contentTransferEncoding = (String) getParameter(messageContext, EmailConstants.CONTENT_TRANSFER_ENCODING);

        if (StringUtils.isEmpty(to) && StringUtils.isEmpty(cc) && StringUtils.isEmpty(bcc)) {
            throw new InvalidConfigurationException("Error occurred while sending the email. " +
                    "Recipients are not provided.");
        } else {
            try {
                MimeMessage message = MessageBuilder.newMessage(session.getSession())
                        .to(to)
                        .fromAddresses(from)
                        .cc(cc)
                        .bcc(bcc)
                        .replyTo(replyTo)
                        .withSubject(subject)
                        .withBody(content, contentType, encoding, contentTransferEncoding)
                        .withAttachments(attachments)
                        .withHeaders(getEmailHeadersFromProperties(messageContext))
                        .build();
                sendMessage(message);
                if (log.isDebugEnabled()) {
                    log.debug(format("Email was sent successfully to %s..", to));
                }
            } catch (MessagingException e) {
                throw new EmailConnectionException(
                        format("Error occurred while sending the email with subject %s to %s.", subject, to), e);
            } catch (IOException e) {
                throw new EmailConnectionException(format("Error occurred while adding attachments with subject %s " +
                        "to the email to %s.", subject, to), e);
            }
        }
    }

    /**
     * Sends the message
     *
     * @param message Message to be sent
     * @throws MessagingException if failed to send the message
     */
    private void sendMessage(MimeMessage message) throws MessagingException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(javax.mail.Message.class.getClassLoader());
            Transport.send(message);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    /**
     * Retrieves custom headers from properties in messages context
     *
     * @param messageContext Message Context
     * @return map of headers
     */
    private Map<String, String> getEmailHeadersFromProperties(MessageContext messageContext) {

        Map<String, String> headers = new HashMap<>();
        for (Object key : messageContext.getPropertyKeySet()) {
            String propertyName = (String) key;
            if (propertyName.startsWith(EMAIL_CONNECTOR_HEADER_PREFIX)) {
                String propertyNameWithoutPrefix = propertyName.split(HEADER_NAME_SEPARATOR)[1];
                headers.put(propertyNameWithoutPrefix, (String) messageContext.getProperty(propertyName));
            }
        }
        return headers;
    }
}
