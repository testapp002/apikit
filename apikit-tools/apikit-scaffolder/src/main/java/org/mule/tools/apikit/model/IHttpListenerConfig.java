package org.mule.tools.apikit.model;

public interface IHttpListenerConfig
{
    public String getName();

    public String getHost();

    public String getPort();

    public String getProtocol();

    public String getBasePath();
}