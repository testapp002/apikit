/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.functional.junit4.runners.ArtifactClassLoaderRunnerConfig;

import java.util.Collection;

import org.junit.Test;

@ArtifactClassLoaderRunnerConfig(plugins = {"org.mule.modules:mule-module-sockets", "org.mule.modules:mule-module-http-ext"},
        providedInclusions = "org.mule.modules:mule-module-sockets")
public class RouterlessConfigurationTestCase extends MuleArtifactFunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/module/apikit/config/routerless-config.xml";
    }

    @Test
    public void alive()
    {
        Collection<Configuration> configurations = muleContext.getRegistry().lookupObjects(Configuration.class);
        assertThat(configurations.size(), is(1));
        Configuration config = configurations.iterator().next();
        assertThat(config.getFlowMappings().size(), is(2));
        assertThat(config.getApi(), notNullValue());
        assertThat(config.getRawRestFlowMap().size(), is(3));
        assertThat(config.getRestFlowMap().size(), is(6));
    }
}
