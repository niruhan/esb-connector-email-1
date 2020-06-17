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
import org.wso2.carbon.connector.connection.MailBoxConnection;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.connection.ConnectionHandler;
import org.wso2.carbon.connector.core.exception.ContentBuilderException;
import org.wso2.carbon.connector.exception.EmailConnectionException;
import org.wso2.carbon.connector.exception.InvalidConfigurationException;
import org.wso2.carbon.connector.utils.EmailConstants;
import org.wso2.carbon.connector.utils.EmailUtils;
import org.wso2.carbon.connector.utils.Error;

import javax.mail.Folder;

import static java.lang.String.format;

/**
 * Deletes emails marked for deletion
 */
public class EmailExpungeFolder extends AbstractConnector {

    @Override
    public void connect(MessageContext messageContext) {

        String errorString = "Error occurred while expunging folder: %s.";
        String folder = (String) getParameter(messageContext, EmailConstants.FOLDER);
        String connectionName = null;
        ConnectionHandler handler = ConnectionHandler.getConnectionHandler();
        MailBoxConnection connection = null;
        try {
            if (StringUtils.isEmpty(folder)) {
                folder = EmailConstants.DEFAULT_FOLDER;
            }
            connectionName = EmailUtils.getConnectionName(messageContext);
            connection = (MailBoxConnection) handler
                    .getConnection(EmailConstants.CONNECTOR_NAME, connectionName);
            expungeFolder(connection, folder);
            if (log.isDebugEnabled()) {
                log.debug(format("Expunged folder: %s...", folder));
            }
            EmailUtils.generateOutput(messageContext, true);
        } catch (EmailConnectionException | ConnectException e) {
            EmailUtils.setErrorsInMessage(messageContext, Error.CONNECTIVITY);
            handleException(format(errorString, folder), e, messageContext);
        } catch (InvalidConfigurationException e) {
            EmailUtils.setErrorsInMessage(messageContext, Error.INVALID_CONFIGURATION);
            handleException(format(errorString, folder), e, messageContext);
        } catch (ContentBuilderException e) {
            EmailUtils.setErrorsInMessage(messageContext, Error.RESPONSE_GENERATION);
            handleException(format(errorString, folder), e, messageContext);
        } finally {
            if (connection != null) {
                handler.returnConnection(EmailConstants.CONNECTOR_NAME, connectionName, connection);
            }
        }
    }

    /**
     * Deletes emails marked for deletion in a particular folder
     *
     * @param connection Email Connection to be used
     * @param folder     Folder to delete messages from
     */
    private void expungeFolder(MailBoxConnection connection, String folder) throws EmailConnectionException {

        try {
            if (StringUtils.isEmpty(folder)) {
                folder = EmailConstants.DEFAULT_FOLDER;
            }
            connection.getFolder(folder, Folder.READ_WRITE);
        } finally {
            connection.closeFolder(true);
        }
    }
}
