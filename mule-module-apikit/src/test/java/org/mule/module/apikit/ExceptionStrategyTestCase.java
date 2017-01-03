/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import com.jayway.restassured.RestAssured;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@ArtifactClassLoaderRunnerConfig(plugins = {"org.mule.modules:mule-module-sockets", "org.mule.modules:mule-module-http-ext"},
        providedInclusions = "org.mule.modules:mule-module-sockets")
public class ExceptionStrategyTestCase extends MuleArtifactFunctionalTestCase
{

    @Rule
    public DynamicPort serverPort = new DynamicPort("serverPort");

    @Override
    public int getTestTimeoutSecs()
    {
        return 6000;
    }

    @Override
    protected void doSetUp() throws Exception
    {
        RestAssured.port = serverPort.getNumber();
        super.doSetUp();
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/module/apikit/exception/exception-strategy-config.xml";
    }

    @Ignore
    @Test
    public void userDefinedStatusOnException() throws Exception
    {
        given().header("Accept", "application/json")
                .expect()
                .response().body(containsString("exception"))
                .header("Content-type", "text/plain").statusCode(410)
                .when().get("/api/resources");
    }

    @Ignore
    @Test
    public void muleMappedStatusOnException() throws Exception
    {
        given().header("Accept", "application/json")
                .expect()
                .response().body(containsString("Authentication denied"))
                .header("Content-type", "text/plain").statusCode(401)
                .when().get("/mule/resources");
    }

}
