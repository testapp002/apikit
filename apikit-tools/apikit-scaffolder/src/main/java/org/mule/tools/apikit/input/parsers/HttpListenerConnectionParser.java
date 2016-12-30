/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.apikit.input.parsers;

import static org.mule.tools.apikit.output.MuleConfigGenerator.HTTPN_NAMESPACE;

import org.mule.tools.apikit.model.HttpListener3xConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

public class HttpListenerConnectionParser implements MuleConfigFileParser
{
    public Map<String, HttpListener3xConfig> parse(Document document){
        Map<String,HttpListener3xConfig> httpListenerConfigMap = new HashMap<String, HttpListener3xConfig>();
        XPathExpression<Element> xp = XPathFactory.instance().compile("//*/*[local-name()='" + HttpListener3xConfig.ELEMENT_NAME + "']",
                                                                      Filters.element(HTTPN_NAMESPACE.getNamespace()));
        List<Element> elements = xp.evaluate(document);
        for (Element element : elements) {
            String name = element.getAttributeValue("name");
            if (name == null)
            {
                throw new IllegalStateException("Cannot retrieve name.");
            }
            //String host = element.getAttributeValue("host");
            //if (host == null)
            //{
            //    throw new IllegalStateException("Cannot retrieve host.");
            //}
            //String port = element.getAttributeValue("port");
            //if (port == null)
            //{
            //    port = Integer.toString(API.DEFAULT_PORT);
            //}
            //String basePath = element.getAttributeValue("basePath");
            //if (basePath == null)
            //{
            //    basePath = "/";
            //}
            //else  if (!basePath.startsWith("/")) {
            //    basePath = "/" + basePath;
            //}
            httpListenerConfigMap.put(name, new HttpListener3xConfig(name, host, port, basePath));
        }
        return httpListenerConfigMap;
    }

}
