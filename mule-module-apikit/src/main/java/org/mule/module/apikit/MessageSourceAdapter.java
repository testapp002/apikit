/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit;

//import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;

import org.mule.module.apikit.exception.ApikitRuntimeException;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;
import org.mule.runtime.module.http.internal.listener.DefaultHttpListener;

public class MessageSourceAdapter
{

    private MessageSourceListenerAdapter delegate;

    public MessageSourceAdapter(MessageSource messageSource)
    {
        //if (messageSource instanceof ImmutableEndpoint)
        //{
        //    delegate = new MessageSourceEndpointAdapter((ImmutableEndpoint) messageSource);
        //}
        //else
        if (messageSource instanceof DefaultHttpListener)
        {
            delegate = new MessageSourceListenerAdapter((DefaultHttpListener) messageSource);
        }
        else if (messageSource instanceof ExtensionMessageSource)
        {
            //delegate = new MessageSourceExtensionAdapter((ExtensionMessageSource) messageSource);
        }
        else if (messageSource == null)
        {
            throw new ApikitRuntimeException("Flow endpoint is null, APIKIT requires a listener ref in each of it's flows");
        }
        else
        {
            throw new ApikitRuntimeException("Message Source Type NOT SUPPORTED: " + messageSource.getClass());
        }

    }

//    @Override
    public String getAddress()
    {
        return delegate.getAddress();
    }

//    @Override
    public String getPath()
    {
        return delegate.getPath();
    }

//    @Override
    public String getScheme()
    {
        return delegate.getScheme();
    }
    //
    //private interface IMessageSource
    //{
    //    String getAddress();
    //    String getPath();
    //    String getScheme();
    //}

    //private class MessageSourceEndpointAdapter implements IMessageSource
    //{

        //private ImmutableEndpoint endpoint;

        //public MessageSourceEndpointAdapter(ImmutableEndpoint messageSource)
        //{
        //    endpoint = messageSource;
        //}

        //@Override
        //public String getAddress()
        //{
        //    return endpoint.getAddress();
        //}
        //
        //@Override
        //public String getPath()
        //{
        //    return endpoint.getEndpointURI().getPath();
        //}
        //
        //@Override
        //public String getScheme()
        //{
        //    return endpoint.getEndpointURI().getScheme();
        //}

}
