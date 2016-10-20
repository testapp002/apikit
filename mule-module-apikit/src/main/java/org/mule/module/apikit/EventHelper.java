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
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.module.http.internal.ParameterMap;

import java.util.HashMap;
import java.util.Map;

public class EventHelper
{
    private static String outboundHeadersName = "_outboundHeaders_";

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
        ParameterMap inboundQueryParams = new ParameterMap(oldAttributes.getQueryParams());
        inboundQueryParams.putAll(queryParams);
        HttpRequestAttributes newAttributes = new HttpRequestAttributes(oldAttributes.getHeaders(), oldAttributes.getListenerPath(), oldAttributes.getRelativePath(), oldAttributes.getVersion(), oldAttributes.getScheme(), oldAttributes.getMethod(), oldAttributes.getRequestPath(), oldAttributes.getRequestUri(), oldAttributes.getQueryString(), inboundQueryParams, oldAttributes.getUriParams(), oldAttributes.getRemoteAddress(), oldAttributes.getClientCertificate());

        messageBuilder.attributes(newAttributes);
        return builder.message(messageBuilder.build()).build();
    }


    public static Event addHeaders(Event event, Map<String, String> headers)
    {
        Event.Builder builder = Event.builder(event);
        InternalMessage.Builder messageBuilder = InternalMessage.builder(event.getMessage());

        HttpRequestAttributes oldAttributes = ((HttpRequestAttributes) event.getMessage().getAttributes());
        ParameterMap newHeaders = new ParameterMap(oldAttributes.getHeaders());
        newHeaders.putAll(headers);
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
