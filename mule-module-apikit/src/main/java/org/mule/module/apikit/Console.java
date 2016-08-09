/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.MessageFactory;

import java.util.Collection;

public class Console implements MessageProcessor, Initialisable, Startable, MuleContextAware, FlowConstructAware
{

    private AbstractConfiguration config;
    private MuleContext muleContext;
    private ConsoleHandler consoleHandler;
    private FlowConstruct flowConstruct;
    protected RamlDescriptorHandler ramlHandler;

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    public void setConfig(AbstractConfiguration config)
    {
        this.config = config;
    }

    public AbstractConfiguration getConfig()
    {
        return config;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        //avoid spring initialization
        if (flowConstruct == null)
        {
            return;
        }
        if (config == null)
        {
            Collection<AbstractConfiguration> configurations = AbstractConfiguration.getAllConfigurations(muleContext);
            if (configurations.size() != 1)
            {
                throw new InitialisationException(MessageFactory.createStaticMessage("APIKit configuration not Found"), this);
            }
            config = configurations.iterator().next();
        }
        consoleHandler = new ConsoleHandler(getConfig().getEndpointAddress(flowConstruct), config);
        config.addConsoleUrl(consoleHandler.getConsoleUrl());
        ramlHandler = new RamlDescriptorHandler(config);
    }

    @Override
    public void start() throws MuleException
    {
        consoleHandler.updateRamlUri();
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        HttpRestRequest request = new HttpRestRequest(event, getConfig());

        //check for raml descriptor request
        if (ramlHandler.handles(request))
        {
            return ramlHandler.processConsoleRequest(event);
        }

        return consoleHandler.process(event);
    }
}
