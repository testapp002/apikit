package org.mule.module.apikit;

import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;
import org.mule.runtime.module.http.internal.listener.DefaultHttpListener;
import org.mule.runtime.module.http.internal.listener.DefaultHttpListenerConfig;

public class MessageSourceExtensionAdapter //implements IMessageSource
{

    private ExtensionMessageSource listener;
    private DefaultHttpListenerConfig config;

    public MessageSourceExtensionAdapter(ExtensionMessageSource messageSource)
    {
        listener = messageSource;
 //       config = (DefaultHttpListenerConfig) messageSource.getConfig();
    }

    //@Override
    public String getAddress()
    {
        return String.format("%s://%s:%s%s", getScheme(), config.getHost(), config.getPort(), getPath());
    }

    //@Override
    public String getPath()
    {
        return "";
        //String path = listener.getPath();
        //return path.endsWith("/*") ? path.substring(0, path.length() - 2) : path;
    }

    //@Override
    public String getScheme()
    {
        return config.getTlsContext() != null ? "https" : "http";
    }
}
