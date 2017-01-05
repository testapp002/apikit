/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.apikit.output.scopes;

import static org.mule.tools.apikit.output.MuleConfigGenerator.HTTPN_NAMESPACE;
import static org.mule.tools.apikit.output.MuleConfigGenerator.HTTP_NAMESPACE;
import static org.mule.tools.apikit.output.MuleConfigGenerator.XMLNS_NAMESPACE;

import org.mule.tools.apikit.misc.APIKitTools;
import org.mule.tools.apikit.model.API;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Element;

public class FlowScope implements Scope {

    private final Element main;

    public FlowScope(Element mule, String exceptionStrategyRef, API api, String configRef, String httpListenerConfigRef) {
        main = new Element("flow", XMLNS_NAMESPACE.getNamespace());

        main.setAttribute("name", api.getId() + "-" + "main");

        if (httpListenerConfigRef != null)
        {
            if (APIKitTools.usesListenersMuleV3(api.getMuleVersion()))
            {
                Element httpListener = new Element("listener", HTTP_NAMESPACE.getNamespace());
                httpListener.setAttribute("config-ref", httpListenerConfigRef);
                httpListener.setAttribute("path", api.getPath());
                main.addContent(httpListener);
            }
            else
            {
                Element httpListener = new Element("listener", HTTPN_NAMESPACE.getNamespace());
                httpListener.setAttribute("config-ref", httpListenerConfigRef);
                httpListener.setAttribute("path", api.getPath());

                Element responseBuilder = new Element("response-builder", HTTPN_NAMESPACE.getNamespace());
                responseBuilder.setAttribute("statusCode", "#[httpStatus]");
                responseBuilder.setAttribute("headersRef", "#[_outboundHeaders_]");
                httpListener.addContent(responseBuilder);

                Element errorResponseBuilder = new Element("error-response-builder", HTTPN_NAMESPACE.getNamespace());
                errorResponseBuilder.setAttribute("statusCode", "#[httpStatus]");
                errorResponseBuilder.setAttribute("headersRef", "#[_outboundHeaders_]");
                httpListener.addContent(errorResponseBuilder);

                main.addContent(httpListener);

                Element setVariable = new Element("set-variable", XMLNS_NAMESPACE.getNamespace());
                setVariable.setAttribute("variableName", "_outboundHeaders_");
                setVariable.setAttribute("value", "#[new java.util.HashMap()]");
                main.addContent(setVariable);

            }
        }
        else
        {
            Element httpInboundEndpoint = new Element("inbound-endpoint", HTTP_NAMESPACE.getNamespace());
            httpInboundEndpoint.setAttribute("address", api.getBaseUri());
            main.addContent(httpInboundEndpoint);
        }

        Element restProcessor = new Element("router", APIKitTools.API_KIT_NAMESPACE.getNamespace());
        if(!StringUtils.isEmpty(configRef)) {
            restProcessor.setAttribute("config-ref", configRef);
        }
        main.addContent(restProcessor);

        Element exceptionStrategy = new Element("exception-strategy", XMLNS_NAMESPACE.getNamespace());
        exceptionStrategy.setAttribute("ref", exceptionStrategyRef);

        main.addContent(exceptionStrategy);

        mule.addContent(main);
    }

    @Override
    public Element generate() {
        return main;
    }
}
