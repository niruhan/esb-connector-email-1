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
package org.wso2.carbon.connector.pojo;

import org.apache.commons.mail.util.MimeMessageParser;
import org.wso2.carbon.connector.exception.EmailParsingException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.internet.MimeMessage;

/**
 * Contains the parsed email content
 */
public class EmailMessage {

    private String emailId;
    private String subject;
    private String to;
    private String from;
    private String cc;
    private String bcc;
    private String replyTo;
    private String htmlContent;
    private String textContent;
    private List<Attachment> attachments;

    public EmailMessage() {

    }

    public EmailMessage(MimeMessage message) throws EmailParsingException {
        MimeMessageParser parser = new MimeMessageParser(message);
        try {
            parser.parse();
            this.textContent = parser.getPlainContent();
            this.htmlContent = parser.getHtmlContent();
            this.emailId = parser.getMimeMessage().getMessageID();
            this.to = getAddressListAsString(parser.getTo());
            this.from = parser.getFrom();
            this.cc = getAddressListAsString(parser.getCc());
            this.bcc = getAddressListAsString(parser.getBcc());
            this.subject = parser.getSubject();
            this.replyTo = parser.getReplyTo();
            List<DataSource> dataSources = parser.getAttachmentList();
            if (!dataSources.isEmpty()) {
                attachments = new ArrayList<>();
            }
            for (DataSource dataSource : dataSources) {
                Attachment attachment = new Attachment(dataSource);
                this.attachments.add(attachment);
            }
        } catch (Exception e) {
            throw new EmailParsingException("Error occurred while retrieving message data.", e);
        }
    }

    public String getHtmlContent() {

        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {

        this.htmlContent = htmlContent;
    }

    public String getTextContent() {

        return textContent;
    }

    public void setTextContent(String textContent) {

        this.textContent = textContent;
    }

    public List<Attachment> getAttachments() {

        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {

        this.attachments = attachments;
    }

    public String getEmailId() {

        return emailId;
    }

    public void setEmailId(String emailId) {

        this.emailId = emailId;
    }

    public String getSubject() {

        return subject;
    }

    public void setSubject(String subject) {

        this.subject = subject;
    }

    public String getTo() {

        return to;
    }

    public void setTo(List<Address> to) {
        this.to = getAddressListAsString(to);
    }

    public String getFrom() {

        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getCc() {

        return cc;
    }

    public void setCc(List<Address> cc) {

        this.cc = getAddressListAsString(cc);
    }

    public String getBcc() {

        return bcc;
    }

    public void setBcc(List<Address> bcc) {

        this.bcc = getAddressListAsString(bcc);
    }

    public String getReplyTo() {

        return replyTo;
    }

    public void setReplyTo(String replyTo) {

        this.replyTo = replyTo;
    }

    private String getAddressListAsString(List<Address> addresses) {

        return String.join(",", addresses.stream().map(Address::toString).collect(Collectors.toList()));
    }
}
