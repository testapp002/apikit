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
//import org.mule.runtime.core.model.ParameterMap;
import org.mule.runtime.core.util.SystemUtils;
import org.mule.service.http.api.domain.ParameterMap;
//import org.mule.runtime.module.http.internal.ParameterMap;

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

    public static Event setPayload(Event event, Object payload, MediaType mediatype)
    {
        Event.Builder builder = Event.builder(event);
        InternalMessage.Builder messageBuilder = InternalMessage.builder(event.getMessage());
        messageBuilder.payload(payload);
        messageBuilder.mediaType(mediatype);
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

}
