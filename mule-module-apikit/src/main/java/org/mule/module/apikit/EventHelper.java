/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit;


import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.util.SystemUtils;
import org.mule.runtime.module.http.internal.ParameterMap;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.KeyValue;

public class EventHelper
{
    private static String outboundHeadersName = "_outboundHeaders_";

    public static Charset getEncoding(Event event, MuleContext muleContext)
    {
        Optional<Charset> payloadEncoding = event.getMessage().getPayload().getDataType().getMediaType().getCharset();
        return payloadEncoding.orElse(SystemUtils.getDefaultEncoding(muleContext));
    }

    public static Event addOutboundProperty(Event event, String key, String value)
    {
        Map<String, String> header = new HashMap<>();
        header.put(key, value);
        return addOutboundProperties(event,header);
    }

    public static Event addOutboundProperties(Event event, Map<String, String> headers)
    {
        Event.Builder builder = Event.builder(event);
        Map<String, String> outboundHeaders = new HashMap<>();
        if (event.getVariable(outboundHeadersName) != null)
        {
            outboundHeaders = new HashMap<>((Map<String, String>) event.getVariable(outboundHeadersName).getValue());
        }
        outboundHeaders.putAll(headers);
        builder.addVariable(outboundHeadersName, outboundHeaders);
        return builder.build();
    }

    public static Event addVariable(Event event, String key, String value)
    {
        Event.Builder builder = Event.builder(event);
        Map<String, String> outboundHeaders = new HashMap<>();
        builder.addVariable(key, value);
        return builder.build();
    }

    public static Object getVariable(Event event, String key)
    {
        Object value;
        try
        {
            value = event.getVariable(key);
        }
        catch (Exception e)
        {
            return null;
        }
        return value;
    }

    public static String getOutboundProperty(Event event, String name)
    {
        if (event.getVariable(outboundHeadersName) != null)
        {
            Map<String, String> outboundPropertiesMap = (Map<String, String>)event.getVariable(outboundHeadersName).getValue();
            if (outboundPropertiesMap != null)
            {
                return outboundPropertiesMap.get(name);
            }
        }
        return null;
    }


    public static Message addHeadersToMessage(Message message, String key, String value)
    {
        Map<String, String> headers = new HashMap<>();
        headers.put(key, value);
        return addHeadersToMessage(message, headers);
    }


    public static Message addHeadersToMessage(Message message, Map<String, String> headers)
    {
        HttpRequestAttributes oldAttributes = ((HttpRequestAttributes)message.getAttributes());
        ParameterMap inboundHeaders = new ParameterMap(oldAttributes.getHeaders());
        inboundHeaders.putAll(headers);
        HttpRequestAttributes newAttributes = new HttpRequestAttributes(inboundHeaders, oldAttributes.getListenerPath(), oldAttributes.getRelativePath(), oldAttributes.getVersion(), oldAttributes.getScheme(), oldAttributes.getMethod(), oldAttributes.getRequestPath(), oldAttributes.getRequestUri(), oldAttributes.getQueryString(), oldAttributes.getQueryParams(), oldAttributes.getUriParams(), oldAttributes.getRemoteAddress(), oldAttributes.getClientCertificate());
        InternalMessage.Builder messageBuilder = InternalMessage.builder(message);
        messageBuilder.attributes(newAttributes);
        return messageBuilder.build();
    }

    public static Event setPayload(Event event, Object payload, String primaryType, String secondaryType)
    {
        Event.Builder builder = Event.builder(event);
        InternalMessage.Builder messageBuilder = InternalMessage.builder(event.getMessage());
        messageBuilder.payload(payload);
        messageBuilder.mediaType(MediaType.create(primaryType, secondaryType));
        return builder.message(messageBuilder.build()).build();
    }

    public static Event setPayload(Event event, Object payload, String mimetype)
    {
        Event.Builder builder = Event.builder(event);
        InternalMessage.Builder messageBuilder = InternalMessage.builder(event.getMessage());
        messageBuilder.payload(payload);
        messageBuilder.mediaType(MediaType.parse(mimetype));
        return builder.message(messageBuilder.build()).build();
    }

    public static Event setPayload(Event event, Object payload)
    {
        Event.Builder builder = Event.builder(event);
        InternalMessage.Builder messageBuilder = InternalMessage.builder(event.getMessage());
        messageBuilder.payload(payload);
        return builder.message(messageBuilder.build()).build();
    }

    public static Event setNullPayload(Event event)
    {
        Event.Builder builder = Event.builder(event);
        InternalMessage.Builder messageBuilder = InternalMessage.builder(event.getMessage());
        messageBuilder.nullPayload();
        return builder.message(messageBuilder.build()).build();
    }


    public static Event addQueryParameters(Event event, Map<String, String> queryParams)
    {
        Event.Builder builder = Event.builder(event);
        InternalMessage.Builder messageBuilder = InternalMessage.builder(event.getMessage());

        HttpRequestAttributes oldAttributes = ((HttpRequestAttributes) event.getMessage().getAttributes());
        //Map inboundQueryParams = new HashMap(oldAttributes.getQueryParams());
        //inboundQueryParams.putAll(queryParams);
        Map<String, LinkedList<String>> mapQueryParams = new HashMap<>();
        queryParams.putAll(oldAttributes.getQueryParams());
        for (Map.Entry<String, String> entry : queryParams.entrySet())
        {
            LinkedList<String> list = new LinkedList<>();
            list.add(entry.getValue());
            mapQueryParams.put(entry.getKey(), list);
        }
        ParameterMap inboundQueryParams = new ParameterMap(mapQueryParams);
        HttpRequestAttributes newAttributes = new HttpRequestAttributes(oldAttributes.getHeaders(), oldAttributes.getListenerPath(), oldAttributes.getRelativePath(), oldAttributes.getVersion(), oldAttributes.getScheme(), oldAttributes.getMethod(), oldAttributes.getRequestPath(), oldAttributes.getRequestUri(), oldAttributes.getQueryString(), inboundQueryParams, oldAttributes.getUriParams(), oldAttributes.getRemoteAddress(), oldAttributes.getClientCertificate());

        messageBuilder.attributes(newAttributes);
        return builder.message(messageBuilder.build()).build();
    }


    public static Event addHeaders(Event event, Map<String, String> headers)
    {
        Event.Builder builder = Event.builder(event);
        InternalMessage.Builder messageBuilder = InternalMessage.builder(event.getMessage());

        HttpRequestAttributes oldAttributes = ((HttpRequestAttributes) event.getMessage().getAttributes());
        Map <String, String> headersMap = new HashMap<>();
        oldAttributes.getHeaders();
        for (Map.Entry<String, String> kv : oldAttributes.getHeaders().entrySet())
        {
            headersMap.put(kv.getKey(), kv.getValue());
        }
        headersMap.putAll(headers);
        //for(Map.Entry<String, String> kv : headers.entrySet())
        //{
        //    .put(kv.getKey(), kv.getValue());
        //}
        ParameterMap newHeaders = new ParameterMap(headersMap);
        HttpRequestAttributes newAttributes = new HttpRequestAttributes(newHeaders, oldAttributes.getListenerPath(), oldAttributes.getRelativePath(), oldAttributes.getVersion(), oldAttributes.getScheme(), oldAttributes.getMethod(), oldAttributes.getRequestPath(), oldAttributes.getRequestUri(), oldAttributes.getQueryString(), oldAttributes.getQueryParams(), oldAttributes.getUriParams(), oldAttributes.getRemoteAddress(), oldAttributes.getClientCertificate());

        messageBuilder.attributes(newAttributes);
        return builder.message(messageBuilder.build()).build();
    }

    public static Event addResponseHeaders(Event event, Map<String, String> headers)
    {
        Event.Builder builder = Event.builder(event);
        InternalMessage.Builder messageBuilder = InternalMessage.builder(event.getMessage());

        HttpResponseAttributes oldAttributes = ((HttpResponseAttributes) event.getMessage().getAttributes());
        ParameterMap newHeaders = new ParameterMap(oldAttributes.getHeaders());
        newHeaders.putAll(headers);
        HttpResponseAttributes newAttributes = new HttpResponseAttributes(oldAttributes.getStatusCode(), oldAttributes.getReasonPhrase(), newHeaders);
//        HttpRequestAttributes newAttributes = new HttpResponseAttributes(oldAttributes. inboundHeaders, oldAttributes.getListenerPath(), oldAttributes.getRelativePath(), oldAttributes.getVersion(), oldAttributes.getScheme(), oldAttributes.getMethod(), oldAttributes.getRequestPath(), oldAttributes.getRequestUri(), oldAttributes.getQueryString(), oldAttributes.getQueryParams(), oldAttributes.getUriParams(), oldAttributes.getRemoteAddress(), oldAttributes.getClientCertificate());

        messageBuilder.attributes(newAttributes);
        return builder.message(messageBuilder.build()).build();
    }


    public static Event addHeader(Event event, String key, String value)
    {
        Map<String, String> headers = new HashMap<>();
        headers.put(key, value);
        return addHeaders(event, headers);
    }
    public static Event addResponseHeader(Event event, String key, String value)
    {
        Map<String, String> headers = new HashMap<>();
        headers.put(key, value);
        return addResponseHeaders(event, headers);
    }

}
