/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit.transform;

import org.mule.module.apikit.EventHelper;
import org.mule.module.apikit.HttpVariableNames;
import org.mule.module.apikit.RestContentTypeParser;
import org.mule.module.apikit.exception.ApikitRuntimeException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.util.SystemUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.net.MediaType;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;


public class ApikitResponseTransformer extends AbstractMessageTransformer
{

    public static final String BEST_MATCH_REPRESENTATION = "_ApikitResponseTransformer_bestMatchRepresentation";
    public static final String CONTRACT_MIME_TYPES = "_ApikitResponseTransformer_contractMimeTypes";
    public static final String APIKIT_ROUTER_REQUEST = "_ApikitResponseTransformer_apikitRouterRequest";
    public static final String ACCEPT_HEADER = "_ApikitResponseTransformer_AcceptedHeaders";

    @Override
    public Object transformMessage(Event event, Charset encoding) throws TransformerException
    {
        try
        {
            event.getVariable(APIKIT_ROUTER_REQUEST);
        }
        catch (NoSuchElementException e)
        {
            // request not originated from an apikit router
            return event.getMessage();
        }
        String responseRepresentation;
        try
        {
            responseRepresentation = event.getVariable(BEST_MATCH_REPRESENTATION).getValue().toString();
        }
        catch (Exception e)
        {
            responseRepresentation = null;
        }
        List<String> responseMimeTypes;
        try
        {
            responseMimeTypes = (List<String>) event.getVariable(CONTRACT_MIME_TYPES).getValue();
        }
        catch (Exception e)
        {
            responseMimeTypes = null;
        }
        String acceptedHeader;
        try
        {
            acceptedHeader = event.getVariable(ACCEPT_HEADER).getValue().toString();
        }
        catch (Exception e)
        {
            acceptedHeader = null;
        }
        if (responseRepresentation == null)
        {
            // clear response payload unless response status is manually set
            //if (((HttpResponseAttributes)event.getMessage().getAttributes()).getHeaders().get("http.status") == null)
            if (EventHelper.getVariable(event, HttpVariableNames.HTTP_STATUS) == null)
            {
                event = EventHelper.setNullPayload(event);
            }
            return event.getMessage();
        }
        return transformToExpectedContentType(event, responseRepresentation, responseMimeTypes, acceptedHeader);
    }

    public Object transformToExpectedContentType(Event event, String responseRepresentation, List<String> responseMimeTypes,
                                                 String acceptedHeader) throws TransformerException
    {
        DataType dataType = event.getMessage().getPayload().getDataType();
        Charset charset = null;
        if (dataType != null && dataType.getMediaType() != null)
        {
            Optional<Charset> payloadEncoding = event.getMessage().getPayload().getDataType().getMediaType().getCharset();
            charset = payloadEncoding.orElse(SystemUtils.getDefaultEncoding(this.muleContext));
        }

        // TODO HACK that relies on using _outboundHeaders_ as the headers container
        String msgContentType = (String) ((Map) event.getVariable("_outboundHeaders_").getValue()).get("Content-Type");

        // user is in charge of setting content-type when using */*
        if ("*/*".equals(responseRepresentation))
        {
            if (msgContentType == null)
            {
                throw new ApikitRuntimeException("Content-Type must be set in the flow when declaring */* response type");
            }
            return event.getMessage();
        }

        if (event.getMessage().getPayload().getValue() == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Response transformation not required. Message payload type is NullPayload");
            }
            return event.getMessage();
        }

        Collection<String> conjunctionTypes = getBestMatchMediaTypes(responseMimeTypes, acceptedHeader);
        String msgAcceptedContentType = acceptedContentType(dataType.getMediaType().toString(), msgContentType, conjunctionTypes);
        if (msgAcceptedContentType != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Response transformation not required. Message payload type is " + msgAcceptedContentType);
            }
            return event.getMessage();
        }
        org.mule.runtime.api.metadata.DataTypeBuilder sourceDataTypeBuilder = DataType.builder();
        sourceDataTypeBuilder.type(event.getMessage().getPayload().getValue().getClass());
        sourceDataTypeBuilder.mediaType(dataType.getMediaType());
        sourceDataTypeBuilder.charset(charset);
        DataType sourceDataType = sourceDataTypeBuilder.build();//DataTypeFactory.create(event.getMessage().getPayload().getClass(), msgMimeType);

        org.mule.runtime.api.metadata.DataTypeBuilder resultDataTypeBuilder = DataType.builder();
        resultDataTypeBuilder.type(String.class);
        resultDataTypeBuilder.mediaType(responseRepresentation);
        DataType resultDataType = resultDataTypeBuilder.build();

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
            Object newPayload = transformer.transform(event.getMessage().getPayload().getValue());
            event = EventHelper.setPayload(event, newPayload, responseRepresentation);
            event = EventHelper.addOutboundProperty(event, "Content-Type", responseRepresentation);

            return event.getMessage();
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
