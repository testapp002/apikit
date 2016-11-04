package org.mule.tools.apikit.output.scopes;

import static org.mule.tools.apikit.output.MuleConfigGenerator.HTTPN_NAMESPACE;

import org.mule.tools.apikit.model.API;
import org.mule.tools.apikit.model.HttpListenerConfig;

import org.jdom2.Element;


public class HttpListenerConfigMule4Scope implements Scope
{
    private final Element mule;
    private final Element httpListenerConfig;

    public HttpListenerConfigMule4Scope(API api, Element mule)
    {
        this.mule = mule;

        if (api.getHttpListenerConfig() != null)
        {
            httpListenerConfig = new Element(HttpListenerConfig.ELEMENT_NAME, HTTPN_NAMESPACE.getNamespace());
            httpListenerConfig.setAttribute("name", api.getHttpListenerConfig().getName());
            //httpListenerConfig.setAttribute("host", api.getHttpListenerConfig().getHost());
            //httpListenerConfig.setAttribute("port", api.getHttpListenerConfig().getPort());
            String basePath = api.getHttpListenerConfig().getBasePath();
            if (basePath != null && basePath != "/" && basePath != "")
            {
                httpListenerConfig.setAttribute("basePath", api.getHttpListenerConfig().getBasePath());
            }
            mule.addContent(httpListenerConfig);
            Element connection = new Element("connection", HTTPN_NAMESPACE.getNamespace());
            connection.setAttribute("host", api.getHttpListenerConfig().getHost());
            connection.setAttribute("port", api.getHttpListenerConfig().getPort());

            //httpListener.setAttribute("path", api.getPath());
            httpListenerConfig.addContent(connection);
        }
        else
            httpListenerConfig = null;
    }

    @Override
    public Element generate()
    {
        return httpListenerConfig;

    }
}
