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

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.connector.connection.EmailProtocol;
import org.wso2.carbon.connector.core.pool.Configuration;
import org.wso2.carbon.connector.exception.InvalidConfigurationException;

/**
 * Configuration parameters used to establish a connection to the email server
 */
public class ConnectionConfiguration {

    private String host;
    private String port;
    private String connectionName;
    private String username;
    private String password;
    private EmailProtocol protocol;
    private String readTimeout;
    private String connectionTimeout;
    private String writeTimeout;
    private boolean requireTLS;
    private boolean checkServerIdentity;
    private String trustedHosts;
    private String sslProtocols;
    private String cipherSuites;
    private Configuration configuration;

    public ConnectionConfiguration() {

        this.configuration = new Configuration();
        // Set default values
        this.configuration.setExhaustedAction("WHEN_EXHAUSTED_FAIL");
        this.configuration.setTestOnBorrow(true);
    }

    public String getHost() {

        return host;
    }

    public void setHost(String host) throws InvalidConfigurationException {

        if (StringUtils.isEmpty(host)) {
            throw new InvalidConfigurationException("Mandatory parameter 'host' is not set.");
        }
        this.host = host;
    }

    public String getPort() {

        return port;
    }

    public void setPort(String port) throws InvalidConfigurationException {

        if (StringUtils.isEmpty(port)) {
            throw new InvalidConfigurationException("Mandatory parameter 'port' is not set.");
        } else if (!StringUtils.isNumeric(port)) {
            throw new InvalidConfigurationException("Parameter 'port' must be a numeric value.");
        }
        this.port = port;
    }

    public String getConnectionName() {

        return connectionName;
    }

    public void setConnectionName(String connectionName) throws InvalidConfigurationException {

        if (StringUtils.isNumeric(connectionName)) {
            throw new InvalidConfigurationException("Mandatory parameter 'connectionName' is not set.");
        }
        this.connectionName = connectionName;
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) throws InvalidConfigurationException {

        if (StringUtils.isEmpty(username)) {
            throw new InvalidConfigurationException("Mandatory parameter 'username' is not set.");
        }
        this.username = username;
    }

    public String getPassword() {

        return password;
    }

    public void setPassword(String password) throws InvalidConfigurationException {

        if (StringUtils.isEmpty(password)) {
            throw new InvalidConfigurationException("Mandatory parameter 'password' is not set.");
        }
        this.password = password;
    }

    public EmailProtocol getProtocol() {

        return protocol;
    }

    public void setProtocol(String protocol) throws InvalidConfigurationException {

        if (StringUtils.isEmpty(protocol)) {
            throw new InvalidConfigurationException("Mandatory parameter 'protocol' is not set.");
        }
        this.protocol = EmailProtocol.valueOf(protocol);
    }

    public String getReadTimeout() {

        return readTimeout;
    }

    public void setReadTimeout(String readTimeout) throws InvalidConfigurationException {

        if (readTimeout != null && !StringUtils.isNumeric(readTimeout)) {
            throw new InvalidConfigurationException("Parameter 'read timeout' must be a numeric value.");
        }
        this.readTimeout = readTimeout;
    }

    public String getConnectionTimeout() {

        return connectionTimeout;
    }

    public void setConnectionTimeout(String connectionTimeout) throws InvalidConfigurationException {

        if (connectionTimeout != null && !StringUtils.isNumeric(connectionTimeout)) {
            throw new InvalidConfigurationException("Parameter 'connection timeout' must be a numeric value.");
        }
        this.connectionTimeout = connectionTimeout;
    }

    public String getWriteTimeout() {

        return writeTimeout;
    }

    public void setWriteTimeout(String writeTimeout) throws InvalidConfigurationException {

        if (writeTimeout != null && !StringUtils.isNumeric(writeTimeout)) {
            throw new InvalidConfigurationException("Parameter 'write timeout' must be a numeric value.");
        }
        this.writeTimeout = writeTimeout;
    }

    public boolean isRequireTLS() {

        return requireTLS;
    }

    public void setRequireTLS(boolean requireTLS) {

        this.requireTLS = requireTLS;
    }

    public boolean isCheckServerIdentity() {

        return checkServerIdentity;
    }

    public void setCheckServerIdentity(boolean checkServerIdentity) {

        this.checkServerIdentity = checkServerIdentity;
    }

    public String getTrustedHosts() {

        return trustedHosts;
    }

    public void setTrustedHosts(String trustedHosts) throws InvalidConfigurationException {

        if (trustedHosts != null && trustedHosts.isEmpty()) {
            throw new InvalidConfigurationException("Parameter 'trusted hosts' cannot be empty.");
        }
        this.trustedHosts = trustedHosts;
    }

    public String getSslProtocols() {

        return sslProtocols;
    }

    public void setSslProtocols(String sslProtocols) throws InvalidConfigurationException {

        if (sslProtocols != null && sslProtocols.isEmpty()) {
            throw new InvalidConfigurationException("Parameter 'ssl protocols' cannot be empty.");
        }
        this.sslProtocols = sslProtocols;
    }

    public String getCipherSuites() {

        return cipherSuites;
    }

    public void setCipherSuites(String cipherSuites) throws InvalidConfigurationException {

        if (cipherSuites != null && cipherSuites.isEmpty()) {
            throw new InvalidConfigurationException("Parameter 'cipher suites' cannot be empty.");
        }
        this.cipherSuites = cipherSuites;
    }

    public int getMaxActiveConnections() {

        return configuration.getMaxActiveConnections();
    }

    public void setMaxActiveConnections(int maxActiveConnections) {

        this.configuration.setMaxActiveConnections(maxActiveConnections);
    }

    public int getMaxIdleConnections() {

        return configuration.getMaxIdleConnections();
    }

    public void setMaxIdleConnections(int maxIdleConnections) {

        this.configuration.setMaxIdleConnections(maxIdleConnections);
    }

    public long getMaxWaitTime() {

        return configuration.getMaxWaitTime();
    }

    public void setMaxWaitTime(long maxWaitTime) {

        this.configuration.setMaxWaitTime(maxWaitTime);
    }

    public long getMinEvictionTime() {

        return configuration.getMinEvictionTime();
    }

    public void setMinEvictionTime(long minEvictionTime) {

        this.configuration.setMinEvictionTime(minEvictionTime);
    }

    public long getEvictionCheckInterval() {

        return configuration.getEvictionCheckInterval();
    }

    public void setEvictionCheckInterval(long evictionCheckInterval) {

        this.configuration.setEvictionCheckInterval(evictionCheckInterval);
    }

    public String getExhaustedAction() {

        return configuration.getExhaustedAction();
    }

    public void setExhaustedAction(String exhaustedAction) {

        this.configuration.setExhaustedAction(exhaustedAction);
    }

    public Configuration getConfiguration() {

        return configuration;
    }

    public void setConfiguration(Configuration configuration) {

        this.configuration = configuration;
    }
}
