/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit;

import static org.mule.runtime.module.http.api.HttpConstants.RequestProperties.HTTP_URI_PARAMS;

//import org.mule.runtime.core.DefaultMuleEvent;
//import org.mule.runtime.core.message.MuleEvent;
//import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.core.NonBlockingVoidMuleEvent;
//import org.mule.runtime.core.OptimizedRequestContext;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
//import org.mule.runtime.core.api.MessagingException;
//import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
//import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.StartException;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.construct.Flow;
import org.mule.module.apikit.exception.ApikitRuntimeException;
import org.mule.module.apikit.exception.InvalidUriParameterException;
import org.mule.module.apikit.exception.MethodNotAllowedException;
import org.mule.module.apikit.exception.MuleRestException;
import org.mule.module.apikit.uri.ResolvedVariables;
import org.mule.module.apikit.uri.URIPattern;
import org.mule.module.apikit.uri.URIResolver;
//import org.mule.runtime.core.message.DefaultMuleMessageBuilder;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.processor.AbstractRequestResponseMessageProcessor;
import org.mule.raml.interfaces.model.IResource;
import org.mule.raml.interfaces.model.parameter.IParameter;

import com.google.common.cache.LoadingCache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRouter extends AbstractInterceptingMessageProcessor implements ApiRouter
{

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected FlowConstruct flowConstruct;
    protected AbstractConfiguration config;
    protected RamlDescriptorHandler ramlHandler;

    @Override
    public void start() throws MuleException
    {
        startConfiguration();
        ramlHandler = new RamlDescriptorHandler(config);
        config.publishConsoleUrls(muleContext.getConfiguration().getWorkingDirectory());
    }

    protected abstract void startConfiguration() throws StartException;

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    protected Event processBlocking(Event event) throws MuleException
    {
        if (config.isExtensionEnabled() && config.getRouterExtension().isExecutable(event))
        {
            return config.getRouterExtension().processBlockingRequest(event, this);
        }
        else
        {
            return processBlockingRequest(event);
        }
    }

    @Override
    public final Event process(Event event) throws MuleException
    {
        //if (isNonBlocking(event))
        //{
        //    return processNonBlocking(event);
        //}
        //else
        //{
            return processBlocking(event);
//        }
    }

    public Event processBlockingRequest(Event event) throws MuleException
    {
        RouterRequest result = processRouterRequest(event);
        event = result.getEvent();
        if (result.getFlow() != null)
        {
            event = result.getFlow().process((Event)event);
        }
        return processRouterResponse(event, result.getSuccessStatus());
    }

    //protected MuleEvent processNonBlocking(MuleEvent event) throws MuleException
    //{
    //    final RouterRequest result = processRouterRequest(event);
    //
    //    event = result.getEvent();
    //
    //    final ReplyToHandler originalReplyToHandler = event.getReplyToHandler();
    //    event = new DefaultMuleEvent(event, new ReplyToHandler()
    //    {
    //        @Override
    //        public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException
    //        {
    //            MuleEvent response = processRouterResponse(new DefaultMuleEvent(event, originalReplyToHandler), result.getSuccessStatus());
    //            // Update RequestContext ThreadLocal for backwards compatibility
    //            OptimizedRequestContext.unsafeSetEvent(response);
    //            if (!NonBlockingVoidMuleEvent.getInstance().equals(response))
    //            {
    //                originalReplyToHandler.processReplyTo(response, null, null);
    //            }
    //        }
    //
    //        @Override
    //        public void processExceptionReplyTo(MessagingException exception, Object replyTo)
    //        {
    //            originalReplyToHandler.processExceptionReplyTo(exception, replyTo);
    //        }
    //    });
    //    // Update RequestContext ThreadLocal for backwards compatibility
    //    OptimizedRequestContext.unsafeSetEvent(event);
    //
    //    if (result.getFlow() != null)
    //    {
    //        event = result.getFlow().process(event);
    //    }
    //    if (!(event instanceof NonBlockingVoidMuleEvent))
    //    {
    //        return processRouterResponse(event, result.getSuccessStatus());
    //    }
    //    return event;
    //}

    protected RouterRequest processRouterRequest(Event event) throws MuleException
    {
        HttpRestRequest request = getHttpRestRequest(event);

        String path = request.getResourcePath();

        //check for raml descriptor request
        if (ramlHandler.handles(request))
        {
            return new RouterRequest(ramlHandler.processRouterRequest((Event)event));
        }

        Event handled = handleEvent(event, path);
        if (handled != null)
        {
            return new RouterRequest(handled);
        }

        URIPattern uriPattern;
        URIResolver uriResolver;
        path = path.isEmpty() ? "/" : path;
        try
        {
            uriPattern = getUriPatternCache().get(path);
            uriResolver = getUriResolverCache().get(path);
        }
        catch (ExecutionException e)
        {
            if (e.getCause() instanceof MuleRestException)
            {
                throw (MuleRestException) e.getCause();
            }
            throw new DefaultMuleException(e);
        }

        IResource resource = getRoutingTable().get(uriPattern);
        if (resource.getAction(request.getMethod()) == null)
        {
            throw new MethodNotAllowedException(resource.getUri(), request.getMethod());
        }

        ResolvedVariables resolvedVariables = uriResolver.resolve(uriPattern);

        processUriParameters(resolvedVariables, resource, event);

        Flow flow = getFlow(resource, request);
        if (flow == null)
        {
            throw new ApikitRuntimeException("Flow not found for resource: " + resource);
        }

        Event validatedEvent = (Event) request.validate(resource.getAction(request.getMethod()));

        return new RouterRequest(validatedEvent, flow, request.getSuccessStatus());
    }

    private Event processRouterResponse(Event event, Integer successStatus)
    {
        if (event == null || VoidMuleEvent.getInstance().equals(event))
        {
            return event;
        }
        return doProcessRouterResponse(event, successStatus);
    }

    protected abstract Event doProcessRouterResponse(Event event, Integer successStatus);

    @Override
    protected Event processNext(Event event) throws MuleException
    {
        throw new UnsupportedOperationException();
    }

    private Map<URIPattern, IResource> getRoutingTable()
    {
        return config.routingTable;
    }

    private LoadingCache<String, URIResolver> getUriResolverCache()
    {
        return config.uriResolverCache;
    }

    private LoadingCache<String, URIPattern> getUriPatternCache()
    {
        return config.uriPatternCache;
    }

    protected abstract Event handleEvent(Event event, String path) throws MuleException;

    private HttpRestRequest getHttpRestRequest(Event event)
    {
        return config.getHttpRestRequest(event);
    }

    private void processUriParameters(ResolvedVariables resolvedVariables, IResource resource, Event event) throws InvalidUriParameterException
    {
        if (logger.isDebugEnabled())
        {
            for (String name : resolvedVariables.names())
            {
                logger.debug("        uri parameter: " + name + "=" + resolvedVariables.get(name));
            }
        }

        if (!config.isDisableValidations())
        {
            for (Map.Entry<String, IParameter> entry : resource.getResolvedUriParameters().entrySet())
            {
                String value = (String) resolvedVariables.get(entry.getKey());
                IParameter uriParameter = entry.getValue();
                if (!uriParameter.validate(value))
                {
                    String msg = String.format("Invalid value '%s' for uri parameter %s. %s",
                                               value, entry.getKey(), uriParameter.message(value));
                    throw new InvalidUriParameterException(msg);
                }
            }
        }
        //TODO: We can use event.getMessage().getAttributes().getUriParams() to read the resolved params.
        //Map<String, String> uriParams = new HashMap<>();
        //for (String name : resolvedVariables.names())
        //{
        //    String value = String.valueOf(resolvedVariables.get(name));
        //    event.setFlowVariable(name, value);
        //    uriParams.put(name, value);
        //}
        //if (event.getMessage().getInboundProperty(HTTP_URI_PARAMS) != null)
        //{
        //    DefaultMuleMessageBuilder muleMessageBuilder = new DefaultMuleMessageBuilder(event.getMessage());
        //
        //    event.getMessage().<Map>getInboundProperty(HTTP_URI_PARAMS).putAll(uriParams);
        //    ((HttpRequestAttriutes)event.getMessage().getAttributes())(HTTP_URI_PARAMS)
        //}
    }

    protected abstract Flow getFlow(IResource resource, HttpRestRequest request);

    private static class RouterRequest
    {

        private Event event;
        private Flow flow;
        private Integer successStatus;

        public RouterRequest(Event event)
        {
            this(event, null, null);
        }

        public RouterRequest(Event event, Flow flow, Integer successStatus)
        {
            this.event = event;
            this.flow = flow;
            this.successStatus = successStatus;
        }

        public Event getEvent()
        {
            return event;
        }

        public Flow getFlow()
        {
            return flow;
        }

        public Integer getSuccessStatus()
        {
            return successStatus;
        }
    }
}
