/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit.transform;

import org.mule.module.apikit.EventHelper;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;

import java.nio.charset.Charset;

public class PayloadNormalizerTransformer extends AbstractMessageTransformer
{

    @Override
    public Object transformMessage(Event event, Charset encoding) throws TransformerException
    {
        DataType dataType = event.getMessage().getPayload().getDataType();
        Charset messageEncoding = EventHelper.getEncoding(event, this.muleContext);

        org.mule.runtime.api.metadata.DataTypeBuilder sourceDataTypeBuilder = DataType.builder();
        sourceDataTypeBuilder.type(event.getMessage().getPayload().getValue().getClass());
        sourceDataTypeBuilder.mediaType(dataType.getMediaType());
        sourceDataTypeBuilder.charset(messageEncoding);
        DataType sourceDataType = sourceDataTypeBuilder.build();
//        DataType sourceDataType = DataTypeFactory.create(event.getMessage().getPayload().getClass(), (String) ((HttpRequestAttributes)event.getMessage().getAttributes()).getHeaders().get("content-type"));
        DataType resultDataType = getReturnDataType();

        Transformer transformer;
        try
        {
            transformer = TransformerCache.getTransformerCache(muleContext).get(new DataTypePair(sourceDataType, resultDataType));
        }
        catch (Exception e)
        {
            throw new TransformerException(I18nMessageFactory.createStaticMessage(e.getMessage()), e);
        }

        return transformer.transform(event.getMessage().getPayload().getValue());
    }
}
