/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit.transform;

import org.mule.common.metadata.datatype.DataTypeFactory;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.module.apikit.EventHelper;
import org.mule.runtime.api.message.Message;
//import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.module.apikit.RestContentTypeParser;
import org.mule.module.apikit.exception.ApikitRuntimeException;
import org.mule.raml.interfaces.model.IMimeType;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
//import org.mule.transformer.types.DataTypeFactory;
//import org.mule.transport.NullPayload;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.net.MediaType;

import java.beans.EventHandler;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

public class ApikitResponseTransformer extends AbstractMessageTransformer
{

    public static final String BEST_MATCH_REPRESENTATION = "_ApikitResponseTransformer_bestMatchRepresentation";
    public static final String CONTRACT_MIME_TYPES = "_ApikitResponseTransformer_contractMimeTypes";
    public static final String APIKIT_ROUTER_REQUEST = "_ApikitResponseTransformer_apikitRouterRequest";
    public static final String ACCEPT_HEADER = "_ApikitResponseTransformer_AcceptedHeaders";

    @Override
    public Object transformMessage(Event event, Charset encoding) throws TransformerException
    {
        if (event.getVariable(APIKIT_ROUTER_REQUEST) == null)
        {
            // request not originated from an apikit router
            return event;
        }
        String responseRepresentation = event.getVariable(BEST_MATCH_REPRESENTATION).toString();
        List<String> responseMimeTypes = (List<String>) event.getVariable(CONTRACT_MIME_TYPES);
        String acceptedHeader = event.getVariable(ACCEPT_HEADER).toString();
        if (responseRepresentation == null)
        {
            // clear response payload unless response status is manually set
        //TODO FIX METHOD
            //if (message.getOutboundProperty("http.status") == null)
            //{
            //    message.setPayload(NullPayload.getInstance());
            //}
            //return message;
        }
        return transformToExpectedContentType(event.getMessage(), responseRepresentation, responseMimeTypes, acceptedHeader);
    }

    public Object transformToExpectedContentType(Event event, String responseRepresentation, List<String> responseMimeTypes,
                                                 String acceptedHeader) throws TransformerException
    {
        //TODO FIX METHOD
        Object payload = event.getMessage().getPayload();
        String msgMimeType = null;
        DataType dataType = event.getMessage().getPayload().getDataType();
        if (dataType != null && dataType.getMediaType() != null)
        {
            msgMimeType = dataType.getMediaType() + ";charset=" + event.getMessage().getEncoding();
        }
        String msgContentType = ((HttpRequestAttributes)event.getMessage().getAttributes()).getHeaders().get("Content-Type");

        // user is in charge of setting content-type when using */*
        if ("*/*".equals(responseRepresentation))
        {
            if (msgContentType == null)
            {
                throw new ApikitRuntimeException("Content-Type must be set in the flow when declaring */* response type");
            }
            return event;
        }

        if (payload == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Response transformation not required. Message payload type is NullPayload");
            }
            return event;
        }

        Collection<String> conjunctionTypes = getBestMatchMediaTypes(responseMimeTypes, acceptedHeader);
        String msgAcceptedContentType = acceptedContentType(msgMimeType, msgContentType, conjunctionTypes);
        if (msgAcceptedContentType != null)
        {
            event = EventHelper.addHeader(event, "Content-Type", msgAcceptedContentType);
            if (logger.isDebugEnabled())
            {
                logger.debug("Response transformation not required. Message payload type is " + msgAcceptedContentType);
            }
            return event;
        }
        DataType sourceDataType = DataTypeFactory.create(message.getPayload().getClass(), msgMimeType);
        DataType resultDataType = DataTypeFactory.create(String.class, responseRepresentation);

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Resolving transformer between [source=%s] and [result=%s]", sourceDataType, resultDataType));
        }

        Transformer transformer;
        try
        {
            transformer = TransformerCache.getTransformerCache(muleContext).get(new DataTypePair(sourceDataType, resultDataType));
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("Transformer resolved to [transformer=%s]", transformer));
            }
            Object newPayload = transformer.transform(message.getPayload());

            message = EventHelper.addHeadersToMessage(message, "Content-Type", responseRepresentation);
            return newPayload;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }

    }

    private Collection<String> getBestMatchMediaTypes(List<String> responseMimeTypes, String acceptedHeader)
    {
        if(acceptedHeader.contains("*/*"))
        {
            return responseMimeTypes;
        }
        final Collection<String> acceptedTypes = transformAcceptedTypes(acceptedHeader);

        return filterAccepted(responseMimeTypes, acceptedTypes);
    }

    private Collection<String> filterAccepted(List<String> responseMimeTypes, final Collection<String> acceptedTypes)
    {
        return Collections2.filter(
                responseMimeTypes, new Predicate<String>()
                {
                    @Override
                    public boolean apply(String m)
                    {
                        return acceptedTypes.contains(m);
                    }
                }
        );
    }

    private Collection<String> transformAcceptedTypes(String acceptedHeader)
    {
        List<MediaType> acceptedMediaTypes = RestContentTypeParser.parseMediaTypes(acceptedHeader);

        return Collections2.transform(acceptedMediaTypes, new Function<MediaType, String>()
        {
            @Override
            public String apply(MediaType mediaType)
            {
                return mediaType.type() + "/" + mediaType.subtype();
            }
        });
    }

    /**
     * checks if the current payload type is any of the accepted ones.
     *
     * @return null if it is not
     */
    private String acceptedContentType(String msgMimeType, String msgContentType, Collection<String> conjunctionTypes)
    {
        for (String acceptedMediaType : conjunctionTypes)
        {
            if(msgMimeType != null && msgMimeType.contains(acceptedMediaType))
            {
                return msgMimeType;
            }
        }
        for (String acceptedMediaType : conjunctionTypes)
        {
            if(msgContentType != null && msgContentType.contains(acceptedMediaType))
            {
                return msgContentType;
            }
        }
        return null;
    }

}
