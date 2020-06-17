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
import org.apache.tika.Tika;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import static javax.mail.Part.ATTACHMENT;
import static javax.mail.Part.INLINE;

/**
 * Builds a new MIME message to be sent as an email
 */
public final class MessageBuilder {

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_TRANSFER_ENCODING_HEADER = "Content-Transfer-Encoding";
    private static final String MULTIPART_TYPE = "multipart/*";
    private static final String DEFAULT_CONTENT_TYPE = "text/html";
    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final String DEFAULT_CONTENT_TRANSFER_ENCODING = "Base64";

    private final MimeMessage message;

    private String attachments;
    private String contentType;
    private String contentTransferEncoding;
    private String content;

    private MessageBuilder(Session s) {

        this.message = new MimeMessage(s);
    }

    /**
     * Creates a new instance for the specified session.
     *
     * @param session the session for which the message is going to be created
     * @return a new instance.
     */
    public static MessageBuilder newMessage(Session session) {

        return new MessageBuilder(session);
    }

    /**
     * Adds the subject to the message that is being built.
     *
     * @param subject the subject of the email.
     * @return MessageBuilder instance
     * @throws MessagingException if failed to set subject
     */
    public MessageBuilder withSubject(String subject) throws MessagingException {

        if (StringUtils.isNotEmpty(subject)) {
            this.message.setSubject(subject);
        }
        return this;
    }

    /**
     * Adds the replyTo to the message that is being built.
     *
     * @param replyTo the replyTo email address.
     * @return MessageBuilder instance
     * @throws MessagingException if failed to add 'replyTo' address
     */
    public MessageBuilder replyTo(String replyTo) throws MessagingException {

        if (StringUtils.isNotEmpty(replyTo)) {
            message.setReplyTo(InternetAddress.parse(replyTo));
        }
        return this;
    }

    /**
     * Adds the from addresses to the messade that is being built.
     *
     * @param fromAddresses the from addresses of the email.
     * @return MessageBuilder instance
     * @throws MessagingException if failed to set 'from' address
     */
    public MessageBuilder fromAddresses(String fromAddresses) throws MessagingException {

        this.message.addFrom(InternetAddress.parse(fromAddresses));
        return this;
    }

    /**
     * Adds the "to" (primary) addresses to the message that is being built.
     *
     * @param toAddresses the primary addresses of the email.
     * @return MessageBuilder instance
     * @throws MessagingException if failed to set 'to' recipient
     */
    public MessageBuilder to(String toAddresses) throws MessagingException {

        this.setRecipient(toAddresses, this.message, Message.RecipientType.TO);
        return this;
    }

    /**
     * Adds the "cc" addresses to the message that is being built.
     *
     * @param ccAddresses the carbon copy addresses of the email.
     * @return MessageBuilder instance
     * @throws MessagingException if failed to set 'cc' recipients
     */
    public MessageBuilder cc(String ccAddresses) throws MessagingException {

        this.setRecipient(ccAddresses, this.message, Message.RecipientType.CC);
        return this;
    }

    /**
     * Adds the "bcc" addresses to the message that is being built.
     *
     * @param bccAddresses the blind carbon copy addresses of the email.
     * @return MessageBuilder instance
     * @throws MessagingException if failed to set 'bcc' recipients
     */
    public MessageBuilder bcc(String bccAddresses) throws MessagingException {

        this.setRecipient(bccAddresses, this.message, Message.RecipientType.BCC);
        return this;
    }

    /**
     * Adds custom headers to the message that is being built.
     *
     * @param headers the custom headers of the email.
     * @return MessageBuilder instance
     * @throws MessagingException if failed to set headers
     */
    public MessageBuilder withHeaders(Map<String, String> headers) throws MessagingException {

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                this.message.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    /**
     * Sets the recipient in the message
     *
     * @param recipient     email address of the recipient
     * @param message       message to which the recipient should be added
     * @param recipientType whether the recipient is 'to', 'cc', 'bcc'
     * @throws MessagingException if failed to set recipient
     */
    private void setRecipient(String recipient, Message message, Message.RecipientType recipientType)
            throws MessagingException {

        if (StringUtils.isNotEmpty(recipient)) {
            message.setRecipients(
                    recipientType,
                    InternetAddress.parse(recipient)
            );
        }
    }

    /**
     * Sets the email body
     *
     * @param content                 email content
     * @param contentType             content type of the email
     * @param encoding                encoding to be used
     * @param contentTransferEncoding content transfer encoding
     * @return MessageBuilder instance
     */
    public MessageBuilder withBody(String content, String contentType, String encoding,
                                   String contentTransferEncoding) {

        this.contentType = StringUtils.isEmpty(contentType) ? DEFAULT_CONTENT_TYPE : contentType;
        this.contentType = StringUtils.isEmpty(encoding)
                ? this.contentType + "; charset=" + DEFAULT_ENCODING
                : this.contentType + "; charset=" + encoding;
        if (StringUtils.isNotEmpty(content)) {
            this.content = content;
        }
        this.contentTransferEncoding = StringUtils.isEmpty(contentTransferEncoding)
                ? DEFAULT_CONTENT_TRANSFER_ENCODING : contentTransferEncoding;
        return this;
    }

    /**
     * Adds attachments to the email
     *
     * @param attachments file paths of the attachments to be added to the email
     * @return MessageBuilder instance
     */
    public MessageBuilder withAttachments(String attachments) {

        if (StringUtils.isNotEmpty(attachments)) {
            this.attachments = attachments;
        }
        return this;
    }

    /**
     * Build MIME Message using configured parameters
     *
     * @return MIME Message with the configured values
     * @throws MessagingException if failed to build message
     */
    public MimeMessage build() throws MessagingException, IOException {

        if (attachments != null && !attachments.isEmpty()) {
            MimeMultipart multipart = new MimeMultipart();
            MimeBodyPart body = new MimeBodyPart();
            setMessageContent(body);
            multipart.addBodyPart(body);
            String[] attachFiles = attachments.split(",");
            for (String filePath : attachFiles) {
                addAttachment(multipart, filePath);
            }
            message.setContent(multipart, MULTIPART_TYPE);
        } else {
            setMessageContent(message);
        }
        return message;
    }

    /**
     * Sets message content in message body
     *
     * @param part MimePart the content should be set to
     * @throws MessagingException if an error occurs while setting the content
     */
    private void setMessageContent(MimePart part) throws MessagingException {

        part.setDisposition(INLINE);
        part.setContent(content, contentType);
        part.setHeader(CONTENT_TYPE_HEADER, contentType);
        if (StringUtils.isNotEmpty(contentTransferEncoding)) {
            part.setHeader(CONTENT_TRANSFER_ENCODING_HEADER, contentTransferEncoding);
        }
    }

    /**
     * Add attachment to message
     *
     * @param multipart Multi part body the messages should be added to
     * @param filePath  File path of the attachment
     * @throws MessagingException if failed to set attachments
     * @throws IOException        if an error occurred while reading attachment content
     */
    private void addAttachment(MimeMultipart multipart, String filePath) throws MessagingException, IOException {

        MimeBodyPart part = new MimeBodyPart();
        File file = new File(filePath);
        try (InputStream fin = new FileInputStream(file)) {
            part.setDisposition(ATTACHMENT);
            part.setFileName(file.getName());
            Tika tika = new Tika();
            String fileContentType = tika.detect(file);
            DataHandler dataHandler = new DataHandler(new EmailAttachmentDataSource(file.getName(), fin,
                    fileContentType));
            part.setDataHandler(dataHandler);
            part.setHeader(CONTENT_TYPE_HEADER, dataHandler.getContentType());
            part.setHeader(CONTENT_TRANSFER_ENCODING_HEADER, this.contentTransferEncoding);
            multipart.addBodyPart(part);
        }
    }

    /**
     * Add attachment to message with a pre-specified content type
     *
     * @param multipart   Multi part body the messages should be added to
     * @param filePath    File path of the attachment
     * @param contentType Content Type of the attachment
     * @throws MessagingException if failed to set attachments
     * @throws IOException        if an error occurred while reading attachment content
     */
    private void addAttachment(MimeMultipart multipart, String filePath, String contentType) throws MessagingException,
            IOException {

        MimeBodyPart part = new MimeBodyPart();
        File file = new File(filePath);
        try (InputStream fin = new FileInputStream(file)) {
            part.setDisposition(ATTACHMENT);
            part.setFileName(file.getName());
            DataHandler dataHandler = new DataHandler(new EmailAttachmentDataSource(file.getName(), fin, contentType));
            part.setDataHandler(dataHandler);
            part.setHeader(CONTENT_TYPE_HEADER, dataHandler.getContentType());
            part.setHeader(CONTENT_TRANSFER_ENCODING_HEADER, this.contentTransferEncoding);
            multipart.addBodyPart(part);
        }
    }
}
