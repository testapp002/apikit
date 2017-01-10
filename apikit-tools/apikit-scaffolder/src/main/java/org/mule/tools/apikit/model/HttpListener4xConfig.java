/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.apikit.model;

import org.mule.tools.apikit.misc.APIKitTools;

import org.apache.commons.lang.StringUtils;

public class HttpListener4xConfig implements IHttpListenerConfig
{
    public static final String ELEMENT_NAME = "listener-config";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String PROTOCOL_ATTRIBUTE = "protocol";
    public static final String DEFAULT_CONFIG_NAME = "httpListenerConfig";

    private String name;
    private String basePath;
    private HttpListenerConnection connection;

    public static class Builder {
        private String name;
        private String host;
        private String port;
        private String protocol;
        private String basePath;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setBasePath(String basePath) {
            this.basePath = basePath;
            return this;
        }

        public HttpListener4xConfig build() {
            return new HttpListener4xConfig(this.name, this.host, this.port, this.protocol, this.basePath);
        }
    }

    public HttpListener4xConfig(final String name,
                                final String host,
                                final String port,
                                final String protocol,
                                final String basePath) {
        this.name = name;
        this.basePath = basePath;
        this.connection = new HttpListenerConnection.Builder(host, port, protocol).build();
    }

    public HttpListener4xConfig(final String name,
                                final String basePath,
                                final HttpListenerConnection httpListenerConnection) {
        this.name = name;
        this.basePath = basePath;
        this.connection = httpListenerConnection;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return connection.getHost();
    }

    public String getPort() {
        return connection.getPort();
    }

    public String getProtocol() {
        return connection.getProtocol();
    }

    public String getBasePath()
    {
        return basePath;
    }
}
