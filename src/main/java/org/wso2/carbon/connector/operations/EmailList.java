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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.connection.MailBoxConnection;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.connection.ConnectionHandler;
import org.wso2.carbon.connector.core.exception.ContentBuilderException;
import org.wso2.carbon.connector.core.util.ConnectorUtils;
import org.wso2.carbon.connector.core.util.PayloadUtils;
import org.wso2.carbon.connector.exception.EmailConnectionException;
import org.wso2.carbon.connector.exception.EmailParsingException;
import org.wso2.carbon.connector.exception.InvalidConfigurationException;
import org.wso2.carbon.connector.pojo.Attachment;
import org.wso2.carbon.connector.pojo.EmailMessage;
import org.wso2.carbon.connector.pojo.MailboxConfiguration;
import org.wso2.carbon.connector.utils.EmailConstants;
import org.wso2.carbon.connector.utils.EmailUtils;
import org.wso2.carbon.connector.utils.Error;
import org.wso2.carbon.connector.utils.ResponseConstants;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;
import javax.xml.namespace.QName;

import static java.lang.String.format;
import static java.util.Date.from;

/**
 * Lists emails
 */
public class EmailList extends AbstractConnector {

    private static final QName EMAILS_ELEMENT = new QName("emails");
    private static final QName EMAIL_ELEMENT = new QName("email");
    private static final QName ATTACHMENTS_ELEMENT = new QName("attachments");
    private static final QName ATTACHMENT_ELEMENT = new QName("attachment");
    private static final QName INDEX_ELEMENT = new QName("index");
    private static final QName EMAIL_ID_ELEMENT = new QName("emailID");
    private static final QName EMAIL_TO_ELEMENT = new QName("to");
    private static final QName EMAIL_FROM_ELEMENT = new QName("from");
    private static final QName EMAIL_CC_ELEMENT = new QName("cc");
    private static final QName EMAIL_BCC_ELEMENT = new QName("bcc");
    private static final QName EMAIL_REPLY_TO_ELEMENT = new QName("replyTo");
    private static final QName EMAIL_SUBJECT_ELEMENT = new QName("subject");
    private static final QName ATTACHMENT_CONTENT_TYPE = new QName("contentType");
    private static final QName ATTACHMENT_NAME = new QName("name");

    @Override
    public void connect(MessageContext messageContext) {

        String errorString = "Error occurred while retrieving messages from folder: %s.";
        String connectionName = null;
        String folderName = StringUtils.EMPTY;
        ConnectionHandler handler = ConnectionHandler.getConnectionHandler();
        MailBoxConnection connection = null;
        try {
            connectionName = EmailUtils.getConnectionName(messageContext);
            connection = (MailBoxConnection) handler
                    .getConnection(EmailConstants.CONNECTOR_NAME, connectionName);
            MailboxConfiguration mailboxConfiguration = getMailboxConfigFromContext(messageContext);
            folderName = mailboxConfiguration.getFolder();
            List<EmailMessage> messageList = retrieveMessages(connection, mailboxConfiguration);
            messageContext.setProperty(ResponseConstants.PROPERTY_EMAILS, messageList);
            setEmailListResponse(messageList, messageContext);
        } catch (EmailConnectionException | ConnectException e) {
            EmailUtils.setErrorsInMessage(messageContext, Error.CONNECTIVITY);
            handleException(format(errorString, folderName), e, messageContext);
        } catch (InvalidConfigurationException e) {
            EmailUtils.setErrorsInMessage(messageContext, Error.INVALID_CONFIGURATION);
            handleException(format(errorString, folderName), e, messageContext);
        } catch (EmailParsingException | ContentBuilderException e) {
            EmailUtils.setErrorsInMessage(messageContext, Error.RESPONSE_GENERATION);
            handleException(format(errorString, folderName), e, messageContext);
        } finally {
            if (connection != null) {
                handler.returnConnection(EmailConstants.CONNECTOR_NAME, connectionName, connection);
            }
        }
    }

    /**
     * Sets email response in body
     *
     * @param emailMessages  List of emails
     * @param messageContext The message context that is processed
     */
    private static void setEmailListResponse(List<EmailMessage> emailMessages, MessageContext messageContext)
            throws ContentBuilderException {

        org.apache.axis2.context.MessageContext axis2MsgCtx = ((org.apache.synapse.core.axis2.
                Axis2MessageContext) messageContext).getAxis2MessageContext();

        SOAPFactory factory = OMAbstractFactory.getSOAP12Factory();
        OMElement emailsElement = factory.createOMElement(EMAILS_ELEMENT);
        for (int i = 0; i < emailMessages.size(); i++) {
            EmailMessage emailMessage = emailMessages.get(i);
            OMElement emailElement = factory.createOMElement(EMAIL_ELEMENT);
            addTextElement(factory, emailElement, INDEX_ELEMENT, Integer.toString(i));
            addTextElement(factory, emailElement, EMAIL_ID_ELEMENT, emailMessage.getEmailId());
            addTextElement(factory, emailElement, EMAIL_TO_ELEMENT, emailMessage.getTo());
            addTextElement(factory, emailElement, EMAIL_FROM_ELEMENT, emailMessage.getFrom());
            addTextElement(factory, emailElement, EMAIL_CC_ELEMENT, emailMessage.getCc());
            addTextElement(factory, emailElement, EMAIL_BCC_ELEMENT, emailMessage.getBcc());
            addTextElement(factory, emailElement, EMAIL_REPLY_TO_ELEMENT, emailMessage.getReplyTo());
            addTextElement(factory, emailElement, EMAIL_SUBJECT_ELEMENT, emailMessage.getSubject());
            if (emailMessage.getAttachments() != null){
                OMElement attachmentsElement = factory.createOMElement(ATTACHMENTS_ELEMENT);
                for (int j = 0; j < emailMessage.getAttachments().size(); j++) {
                    Attachment attachment = emailMessage.getAttachments().get(j);
                    OMElement attachmentElement = factory.createOMElement(ATTACHMENT_ELEMENT);
                    addTextElement(factory, attachmentElement, INDEX_ELEMENT, Integer.toString(j));
                    addTextElement(factory, attachmentElement, ATTACHMENT_NAME, attachment.getName());
                    addTextElement(factory, attachmentElement, ATTACHMENT_CONTENT_TYPE, attachment.getContentType());
                    attachmentsElement.addChild(attachmentElement);
                }
                emailElement.addChild(attachmentsElement);
            }
            emailsElement.addChild(emailElement);
        }
        PayloadUtils.setPayloadInEnvelope(axis2MsgCtx, emailsElement);
        PayloadUtils.handleSpecialProperties(ResponseConstants.RESPONSE_CONTENT_TYPE, axis2MsgCtx);
    }

    /**
     * Adds text element to parent OMElement
     *
     * @param factory SOAP factory to create element
     * @param parent  Parent OMElement
     * @param qName   QName of the new text element
     * @param value   Value of the new text element
     */
    private static void addTextElement(SOAPFactory factory, OMElement parent, QName qName, String value) {

        if (StringUtils.isNotEmpty(value)) {
            OMElement newElement = factory.createOMElement(qName);
            newElement.addChild(factory.createOMText(value));
            parent.addChild(newElement);
        }
    }

    /**
     * Gets email content and attachments
     *
     * @param messages List of messages to be parsed
     * @return Parsed messages
     * @throws EmailParsingException if failed to parse content
     */
    private static List<EmailMessage> parseMessageList(List<Message> messages) throws EmailParsingException {

        List<EmailMessage> messagesList = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(javax.mail.Message.class.getClassLoader());
            for (Message message : messages) {
                messagesList.add(new EmailMessage((MimeMessage) message));
            }
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        return messagesList;
    }

    /**
     * Retrieves messages that matches given filtering criteria
     *
     * @param connection           Mailbox connection to be used
     * @param mailboxConfiguration Mailbox Configurations
     */
    private List<EmailMessage> retrieveMessages(MailBoxConnection connection, MailboxConfiguration mailboxConfiguration)
            throws EmailConnectionException, EmailParsingException {

        List<EmailMessage> messageList;
        boolean deleteAfterRetrieval = mailboxConfiguration.getDeleteAfterRetrieve();
        try {
            String folderName = mailboxConfiguration.getFolder();
            Folder mailbox;
            if (deleteAfterRetrieval) {
                mailbox = connection.getFolder(folderName, Folder.READ_WRITE);
            } else {
                mailbox = connection.getFolder(folderName, Folder.READ_ONLY);
            }
            if (log.isDebugEnabled()) {
                log.debug(format("Retrieving messages from Mail folder: %s ...", folderName));
            }
            Message[] messages = mailbox.search(getSearchTerm(mailboxConfiguration));
            messageList = parseMessageList(getPaginatedMessages(messages,
                    mailboxConfiguration.getOffset(), mailboxConfiguration.getLimit(), deleteAfterRetrieval));
        } catch (MessagingException e) {
            throw new EmailConnectionException("Error occurred when searching emails. %s", e);
        } finally {
            connection.closeFolder(deleteAfterRetrieval);
        }
        return messageList;
    }

    /**
     * Retrieves paginated messages
     *
     * @param messages Messages to filter
     * @param offset   Record index to start filtering from
     * @param limit    Number of emails to be retrieved
     * @return List of paginated messages
     */
    private List<Message> getPaginatedMessages(Message[] messages, int offset, int limit, boolean deleteAfterRetrieval) {

        List<Message> messageList = Arrays.asList(messages);
        int toIndex;
        if (limit != -1) {
            toIndex = offset + limit;
            if (toIndex > messageList.size()) {
                toIndex = messageList.size();
            }
            if (log.isDebugEnabled()) {
                log.debug(format("Retrieving messages from index %d to %d ...", offset, toIndex));
            }
            if (messageList.size() >= offset) {
                messageList = messageList.subList(offset, toIndex);
            }
            if (log.isDebugEnabled()) {
                log.debug(format("Retrieved %d message(s)...", messageList.size()));
            }
        } else {
            toIndex = messages.length;
        }
        if (deleteAfterRetrieval) {
            markMessagesAsDeleted(messages, offset, toIndex);
        }
        return messageList;
    }

    /**
     * Marks emails within a range as deleted
     *
     * @param messages List of messages
     * @param from     Start index
     * @param to       End index
     */
    private void markMessagesAsDeleted(Message[] messages, int from, int to) {

        if (log.isDebugEnabled()) {
            log.debug(format("Marking messages from %d to %d as deleted...", from, to));
        }
        for (int i = from; i < to; i++) {
            try {
                messages[i].setFlag(Flags.Flag.DELETED, true);
            } catch (MessagingException e) {
                log.error("Failed to mark message as deleted.", e);
            }
        }
    }

    /**
     * Builds search term
     *
     * @param mailboxConfiguration Configured parameters containing the filters
     * @return {@link SearchTerm} containing all the filters
     * @throws EmailConnectionException if failed to parse email address
     */
    private SearchTerm getSearchTerm(MailboxConfiguration mailboxConfiguration) throws EmailConnectionException {

        FlagTerm seenFlagTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), mailboxConfiguration.getSeen());
        FlagTerm answeredFlagTerm = new FlagTerm(new Flags(Flags.Flag.ANSWERED), mailboxConfiguration.getAnswered());
        FlagTerm recentFlagTerm = new FlagTerm(new Flags(Flags.Flag.RECENT), mailboxConfiguration.getRecent());
        FlagTerm deletedFlagTerm = new FlagTerm(new Flags(Flags.Flag.DELETED), mailboxConfiguration.getDeleted());

        AndTerm searchTerm = new AndTerm(deletedFlagTerm, new AndTerm(seenFlagTerm, new AndTerm(answeredFlagTerm,
                recentFlagTerm)));

        String subjectRegex = mailboxConfiguration.getSubjectRegex();
        if (StringUtils.isNotEmpty(subjectRegex)) {
            SubjectTerm subjectTerm = new SubjectTerm(subjectRegex);
            searchTerm = new AndTerm(searchTerm, subjectTerm);
        }

        String fromRegex = mailboxConfiguration.getFromRegex();
        if (StringUtils.isNotEmpty(fromRegex)) {
            try {
                FromTerm fromTerm = new FromTerm(new InternetAddress(fromRegex));
                searchTerm = new AndTerm(searchTerm, fromTerm);
            } catch (AddressException e) {
                throw new EmailConnectionException("Error occurred when parsing 'from' email address. %s");
            }
        }

        String receivedSince = mailboxConfiguration.getReceivedSince();
        if (receivedSince != null) {
            LocalDateTime date = LocalDateTime.parse(receivedSince);
            ReceivedDateTerm receivedSinceDateTerm = new ReceivedDateTerm(ComparisonTerm.GT,
                    from(date.atZone(ZoneId.systemDefault()).toInstant()));
            searchTerm = new AndTerm(searchTerm, receivedSinceDateTerm);
        }

        String receivedUntil = mailboxConfiguration.getReceivedUntil();
        if (receivedUntil != null) {
            LocalDateTime date = LocalDateTime.parse(receivedUntil);
            ReceivedDateTerm receivedUntilDateTerm = new ReceivedDateTerm(ComparisonTerm.LT,
                    from(date.atZone(ZoneId.systemDefault()).toInstant()));
            searchTerm = new AndTerm(searchTerm, receivedUntilDateTerm);
        }

        String sentSince = mailboxConfiguration.getSentSince();
        if (sentSince != null) {
            LocalDateTime date = LocalDateTime.parse(sentSince);
            ReceivedDateTerm sentSinceDateTerm = new ReceivedDateTerm(ComparisonTerm.GT,
                    from(date.atZone(ZoneId.systemDefault()).toInstant()));
            searchTerm = new AndTerm(searchTerm, sentSinceDateTerm);
        }

        String sentUntil = mailboxConfiguration.getSentUntil();
        if (sentUntil != null) {
            LocalDateTime date = LocalDateTime.parse(sentUntil);
            ReceivedDateTerm sentUntilDateTerm = new ReceivedDateTerm(ComparisonTerm.LT,
                    from(date.atZone(ZoneId.systemDefault()).toInstant()));
            searchTerm = new AndTerm(searchTerm, sentUntilDateTerm);
        }
        return searchTerm;
    }

    /**
     * Extracts mailbox connection configurations from operation template
     *
     * @param messageContext Message Context from which the parameters should be extracted from
     * @return Mailbox Configurations set
     */
    private MailboxConfiguration getMailboxConfigFromContext(MessageContext messageContext) {

        String folder = (String) ConnectorUtils.lookupTemplateParamater(messageContext, EmailConstants.FOLDER);
        String deleteAfterRetrieve = (String) ConnectorUtils.lookupTemplateParamater(messageContext,
                EmailConstants.DELETE_AFTER_RETRIEVE);
        String seen = (String) ConnectorUtils.lookupTemplateParamater(messageContext,
                EmailConstants.FLAG_SEEN);
        String answered = (String) ConnectorUtils.lookupTemplateParamater(messageContext,
                EmailConstants.FLAG_ANSWERED);
        String recent = (String) ConnectorUtils.lookupTemplateParamater(messageContext,
                EmailConstants.FLAG_RECENT);
        String deleted = (String) ConnectorUtils.lookupTemplateParamater(messageContext,
                EmailConstants.FLAG_DELETED);
        String receivedSince = (String) ConnectorUtils.lookupTemplateParamater(messageContext,
                EmailConstants.RECEIVED_SINCE);
        String receivedUntil = (String) ConnectorUtils.lookupTemplateParamater(messageContext,
                EmailConstants.RECEIVED_UNTIL);
        String sentSince = (String) ConnectorUtils.lookupTemplateParamater(messageContext,
                EmailConstants.SENT_SINCE);
        String sentUntil = (String) ConnectorUtils.lookupTemplateParamater(messageContext,
                EmailConstants.SENT_UNTIL);
        String subjectRegex = (String) ConnectorUtils.lookupTemplateParamater(messageContext,
                EmailConstants.SUBJECT_REGEX);
        String fromRegex = (String) ConnectorUtils.lookupTemplateParamater(messageContext,
                EmailConstants.FROM_REGEX);
        String offset = (String) ConnectorUtils.lookupTemplateParamater(messageContext,
                EmailConstants.OFFSET);
        String limit = (String) ConnectorUtils.lookupTemplateParamater(messageContext,
                EmailConstants.LIMIT);

        if (StringUtils.isEmpty(folder)) {
            folder = EmailConstants.DEFAULT_FOLDER;
        }

        boolean seenFlag = true;
        if (seen != null) {
            seenFlag = Boolean.parseBoolean(seen);
        }

        boolean answeredFlag = true;
        if (answered != null) {
            answeredFlag = Boolean.parseBoolean(answered);
        }

        boolean recentFlag = true;
        if (recent != null) {
            recentFlag = Boolean.parseBoolean(recent);
        }

        boolean deletedFlag = true;
        if (deleted != null) {
            deletedFlag = Boolean.parseBoolean(deleted);
        }

        int offSetValue = EmailConstants.DEFAULT_OFFSET;
        if (offset != null) {
            offSetValue = Integer.parseInt(offset);
        }

        int limitValue = EmailConstants.DEFAULT_LIMIT;
        if (limit != null) {
            limitValue = Integer.parseInt(limit);
        }

        MailboxConfiguration mailboxConfiguration = new MailboxConfiguration();
        mailboxConfiguration.setFolder(folder);
        mailboxConfiguration.setDeleteAfterRetrieve(Boolean.parseBoolean(deleteAfterRetrieve));
        mailboxConfiguration.setSeen(seenFlag);
        mailboxConfiguration.setAnswered(answeredFlag);
        mailboxConfiguration.setRecent(recentFlag);
        mailboxConfiguration.setDeleted(deletedFlag);
        mailboxConfiguration.setReceivedSince(receivedSince);
        mailboxConfiguration.setReceivedUntil(receivedUntil);
        mailboxConfiguration.setSentSince(sentSince);
        mailboxConfiguration.setSentUntil(sentUntil);
        mailboxConfiguration.setSubjectRegex(subjectRegex);
        mailboxConfiguration.setFromRegex(fromRegex);
        mailboxConfiguration.setOffset(offSetValue);
        mailboxConfiguration.setLimit(limitValue);

        return mailboxConfiguration;
    }
}
