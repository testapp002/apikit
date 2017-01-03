/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.apikit.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

public class APIFactory
{
    private Map<File, API> apis = new HashMap<File, API>();
    private Map<String, HttpListener3xConfig> domainHttpListenerConfigs = new HashMap<>();
    public APIFactory (Map<String, HttpListener3xConfig> domainHttpListenerConfigs)
    {
        this.domainHttpListenerConfigs.putAll(domainHttpListenerConfigs);
    }

    public APIFactory ()
    {
    }

    public API createAPIBindingInboundEndpoint(File ramlFile, File xmlFile, String baseUri, String path, APIKitConfig config)
    {
        return createAPIBinding(ramlFile, xmlFile, baseUri, path, config, null, "3.5.0");
    }

    public API createAPIBindingListenerMule3(File ramlFile, File xmlFile, String path, APIKitConfig config, HttpListener3xConfig httpListener3xConfig)
    {
        return createAPIBinding(ramlFile, xmlFile, null, path, config, httpListener3xConfig, "3.7.0");
    }

    public API createAPIBinding(File ramlFile, File xmlFile, String baseUri, String path, APIKitConfig config, HttpListener3xConfig httpListener3xConfig, String muleVersion)
    {
        Validate.notNull(ramlFile);
        if(apis.containsKey(ramlFile))
        {
            API api = apis.get(ramlFile);
            if(api.getXmlFile() == null && xmlFile != null)
            {
                api.setXmlFile(xmlFile);
            }
            return api;
        }
        API api = new API(ramlFile, xmlFile, baseUri, path, config, muleVersion);
        if (!org.mule.tools.apikit.misc.APIKitTools.defaultIsInboundEndpoint(muleVersion))
        {
            if (httpListener3xConfig == null)
            {
                if (domainHttpListenerConfigs.size() >0)
                {
                    api.setHttpListenerConfig(getFirstLC());
                }
                else
                {
                    api.setDefaultHttpListenerConfig();
                }
            }
            else
            {
                api.setHttpListenerConfig(httpListener3xConfig);
            }
        }
        api.setConfig(config);
        apis.put(ramlFile, api);
        return api;
    }

    public Map<String, HttpListener3xConfig> getDomainHttpListenerConfigs() {
        return domainHttpListenerConfigs;
    }

    private HttpListener3xConfig getFirstLC()
    {
        List<Map.Entry<String,HttpListener3xConfig>> list = new ArrayList<>(domainHttpListenerConfigs.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, HttpListener3xConfig>>(){
            @Override
            public int compare(Map.Entry<String, HttpListener3xConfig> o1, Map.Entry<String, HttpListener3xConfig> o2)
            {
                Integer i1 = Integer.parseInt(o1.getValue().getPort());
                Integer i2 = Integer.parseInt(o2.getValue().getPort());
                return i1.compareTo(i2);
            }
        });
        return list.get(0).getValue();
    }
}
