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
package org.wso2.carbon.connector.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.wso2.carbon.connector.connection.EmailConnection;
import org.wso2.carbon.connector.connection.EmailConnectionFactory;
import org.wso2.carbon.connector.connection.EmailProtocol;
import org.wso2.carbon.connector.connection.MailBoxConnection;
import org.wso2.carbon.connector.core.connection.ConnectionHandler;
import org.wso2.carbon.connector.core.exception.ContentBuilderException;
import org.wso2.carbon.connector.core.util.PayloadUtils;
import org.wso2.carbon.connector.exception.EmailConnectionException;
import org.wso2.carbon.connector.exception.EmailNotFoundException;
import org.wso2.carbon.connector.exception.InvalidConfigurationException;
import org.wso2.carbon.connector.pojo.Attachment;
import org.wso2.carbon.connector.pojo.ConnectionConfiguration;
import org.wso2.carbon.connector.pojo.EmailMessage;

import java.util.List;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.search.MessageIDTerm;
import javax.mail.search.SearchTerm;

import static java.lang.String.format;

/**
 * Utilities for manipulating emails
 */
public final class EmailUtils {

    private static final Log log = LogFactory.getLog(EmailUtils.class);

    // Response constants
    private static final String START_TAG = "<result><success>";
    private static final String END_TAG = "</success></result>";

    private EmailUtils() {

    }

    /**
     * Creates a connection with the given configuration
     *
     * @param connectionConfiguration connection configuration
     */
    public static void createConnection(ConnectionConfiguration connectionConfiguration) {

        String connectorName = EmailConstants.CONNECTOR_NAME;
        String connectionName = connectionConfiguration.getConnectionName();
        ConnectionHandler handler = ConnectionHandler.getConnectionHandler();
        if (!handler.checkIfConnectionExists(connectorName, connectionName)) {
            if (connectionConfiguration.getProtocol().getName().equalsIgnoreCase(EmailProtocol.SMTP.name())) {
                // For SMTP protocols a connection pool is not required as they require only a session, which need not be
                // manipulated as the connection.
                EmailConnection connection = new EmailConnection(connectionConfiguration);
                handler.createConnection(EmailConstants.CONNECTOR_NAME, connectionName, connection);
            } else {
                // For other protocols, such as IMAP and POP3, connections to a store and folder is made which requires to
                // handled. Hence, for these instances, we will create a connection pool to optimize the use of these
                // connections.
                handler.createConnection(EmailConstants.CONNECTOR_NAME, connectionName,
                        new EmailConnectionFactory(connectionConfiguration), connectionConfiguration.getConfiguration());
            }
        } else {
            log.debug(format("Connection exists for connection name: %s.", connectionName));
        }
    }

    /**
     * Changes the email state by setting flags
     *
     * @param connection Mailbox connection to be used to connect to server
     * @param folderName Mailbox name
     * @param emailID    Email ID of the message of which the state is to be changed
     * @param flag       Flag to be set
     * @param expunge    whether to delete messages marked for deletion
     * @return true if the status update was successful, false otherwise
     * @throws EmailConnectionException thrown if failed to set the flags on the message
     */
    public static boolean changeEmailState(MailBoxConnection connection, String folderName, String emailID,
                                           Flags.Flag flag, boolean expunge)
            throws EmailConnectionException, EmailNotFoundException, InvalidConfigurationException {

        boolean success = false;
        if (StringUtils.isEmpty(folderName)) {
            folderName = EmailConstants.DEFAULT_FOLDER;
        }
        if (StringUtils.isEmpty(emailID)) {
            throw new InvalidConfigurationException("Mandatory parameter 'Email ID' is not configured.");
        }

        try {
            Folder folder = connection.getFolder(folderName, Folder.READ_WRITE);
            SearchTerm searchTerm = new MessageIDTerm(emailID);
            Message[] messages = folder.search(searchTerm);

            if (messages.length > 0) {
                Message message = messages[0];
                if (flag != null) {
                    message.setFlag(flag, true);
                    success = true;
                    if (log.isDebugEnabled()) {
                        log.debug(format("%s flag updated for message with ID: %s...", getFlagName(flag), emailID));
                    }
                }
            } else {
                log.error(format("No emails found with ID: %s.", emailID));
                throw new EmailNotFoundException(format("No emails found with ID: %s.", emailID));
            }
        } catch (MessagingException e) {
            throw new EmailConnectionException("Error occurred while changing email state.", e);
        } finally {
            connection.closeFolder(expunge);
        }
        return success;
    }

    /**
     * Retrieve flag name by mask
     *
     * @param flag Flag
     * @return Flag Name
     */
    private static String getFlagName(Flags.Flag flag) {

        String flagName = "";
        if (flag == Flags.Flag.DELETED) {
            flagName = EmailConstants.FLAG_DELETED;
        }
        if (flag == Flags.Flag.SEEN) {
            flagName = EmailConstants.FLAG_SEEN;
        }
        return flagName;
    }

    /**
     * Gets email of respective index from list
     *
     * @param emailMessages List of Email Messages
     * @param emailIndex    Index of the email to be retrieved
     * @return Email message in the relevant index
     */
    public static EmailMessage getEmail(List<EmailMessage> emailMessages, String emailIndex)
            throws InvalidConfigurationException {

        EmailMessage message;
        try {
            message = emailMessages.get(Integer.parseInt(emailIndex));
        } catch (IndexOutOfBoundsException e) {
            throw new InvalidConfigurationException("Failed to retrieve email. Invalid index set for email index.", e);
        }
        return message;
    }

    /**
     * Gets attachment of respective index from list
     *
     * @param emailMessage    Email message to retrieve attachment from
     * @param attachmentIndex Index of the attachment to be retrieved
     * @return Attachment in the relevant index
     */
    public static Attachment getEmailAttachment(EmailMessage emailMessage, String attachmentIndex)
            throws InvalidConfigurationException {

        Attachment attachment;
        try {
            attachment = emailMessage.getAttachments().get(Integer.parseInt(attachmentIndex));
        } catch (IndexOutOfBoundsException e) {
            throw new InvalidConfigurationException("Failed to retrieve attachment. " +
                    "Invalid index set for attachment index.", e);
        }
        return attachment;
    }

    /**
     * Retrieves connection name from message context if configured as configKey attribute
     * or from the template parameter
     *
     * @param messageContext Message Context from which the parameters should be extracted from
     * @return connection name
     */
    public static String getConnectionName(MessageContext messageContext) throws InvalidConfigurationException {

        String connectionName = (String) messageContext.getProperty(EmailConstants.NAME);
        if (connectionName == null) {
            throw new InvalidConfigurationException("Connection name is not set.");
        }
        return connectionName;
    }

    /**
     * Generates the output payload with result status
     *
     * @param messageContext The message context that is processed
     * @param resultStatus   Result of the status
     */
    public static void generateOutput(MessageContext messageContext, boolean resultStatus)
            throws ContentBuilderException {

        String response = START_TAG + resultStatus + END_TAG;
        PayloadUtils.preparePayload(((Axis2MessageContext) messageContext).getAxis2MessageContext(), response);
    }

    /**
     * Sets the error code and error detail in message
     *
     * @param messageContext Message Context
     * @param error          Error to be set
     */
    public static void setErrorsInMessage(MessageContext messageContext, Error error) {

        messageContext.setProperty(ResponseConstants.PROPERTY_ERROR_CODE, error.getErrorCode());
        messageContext.setProperty(ResponseConstants.PROPERTY_ERROR_MESSAGE, error.getErrorDetail());
    }

}
