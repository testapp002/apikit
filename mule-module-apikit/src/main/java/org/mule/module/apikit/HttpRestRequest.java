/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit;

import static org.mule.module.apikit.CharsetUtils.getEncoding;
import static org.mule.module.apikit.CharsetUtils.trimBom;
import static org.mule.module.apikit.transform.ApikitResponseTransformer.ACCEPT_HEADER;
import static org.mule.module.apikit.transform.ApikitResponseTransformer.APIKIT_ROUTER_REQUEST;
import static org.mule.module.apikit.transform.ApikitResponseTransformer.BEST_MATCH_REPRESENTATION;
import static org.mule.module.apikit.transform.ApikitResponseTransformer.CONTRACT_MIME_TYPES;

//import org.mule.DefaultMuleMessage;
//import org.mule.runtime.core.api.MuleEvent;
import org.mule.common.metadata.datatype.DataTypeFactory;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.PropertyScope;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleException;
//import org.mule.runtime.core.api.MuleMessage;
//import org.mule.api.transformer.DataType;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.message.MessageAttachments;
import org.mule.runtime.core.api.transformer.TransformerException;
//import org.mule.api.transport.PropertyScope;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.message.ds.StringDataSource;
import org.mule.module.apikit.exception.BadRequestException;
import org.mule.module.apikit.exception.InvalidFormParameterException;
import org.mule.module.apikit.exception.InvalidHeaderException;
import org.mule.module.apikit.exception.InvalidQueryParameterException;
import org.mule.module.apikit.exception.MuleRestException;
import org.mule.module.apikit.exception.NotAcceptableException;
import org.mule.module.apikit.exception.UnsupportedMediaTypeException;
import org.mule.module.apikit.uri.URICoder;
import org.mule.module.apikit.validation.RestSchemaValidator;
import org.mule.module.apikit.validation.RestSchemaValidatorFactory;
import org.mule.module.apikit.validation.RestXmlSchemaValidator;
import org.mule.module.apikit.validation.SchemaType;
import org.mule.module.apikit.validation.cache.SchemaCacheUtils;
//import org.mule.runtime.module.http.internal.ParameterMap;
import org.mule.raml.interfaces.model.IAction;
import org.mule.raml.interfaces.model.IMimeType;
import org.mule.raml.interfaces.model.IResponse;
import org.mule.raml.interfaces.model.parameter.IParameter;
//import org.mule.transformer.types.DataTypeFactory;
import org.mule.runtime.core.model.ParameterMap;
import org.mule.runtime.core.util.CaseInsensitiveHashMap;
import org.mule.runtime.core.util.IOUtils;

import com.google.common.net.MediaType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.activation.DataHandler;

import org.raml.v2.api.model.common.ValidationResult;
import org.raml.v2.internal.utils.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRestRequest
{

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected MuleEvent requestEvent;
    protected AbstractConfiguration config;
    protected IAction action;
    protected HttpProtocolAdapter adapter;
    protected MuleContext muleContext;

    public HttpRestRequest(MuleEvent event, MuleContext muleContext, AbstractConfiguration config)
    {
        requestEvent = event;
        this.config = config;
        this.muleContext = muleContext;
        adapter = new HttpProtocolAdapter((Event) event);
    }

    public HttpProtocolAdapter getAdapter()
    {
        return adapter;
    }

    public String getResourcePath()
    {
        String path = adapter.getRelativePath();
        return URICoder.decode(path);
        //String basePath = adapter.getBasePath();
        //int start = basePath.endsWith("/") ? basePath.length() - 1 : basePath.length();
        //int end = path.endsWith("/") ? path.length() - 1 : path.length();
        //return URICoder.decode(path.substring(start, end));
    }

    public String getMethod()
    {
        return adapter.getMethod().toLowerCase();
    }

    public String getContentType()
    {
        return adapter.getRequestMediaType();
    }

    /**
     * Validates the request against the RAML and negotiates the response representation.
     * The resulting event is only updated when default values are applied.
     *
     * @param action Raml action to be invoked
     * @return the updated Mule Event
     * @throws MuleException
     */
    public MuleEvent validate(IAction action) throws MuleException
    {
        this.action = action;
        if (!config.isDisableValidations())
        {
            processQueryParameters();
            processHeaders();
        }
        negotiateInputRepresentation();
        List<String> responseMimeTypes = getResponseMimeTypes();
        String responseRepresentation = negotiateOutputRepresentation(responseMimeTypes);
        Event.Builder newRequestEventBuilder = Event.builder((Event)requestEvent);
        if (responseMimeTypes != null)
        {
            newRequestEventBuilder.addVariable(CONTRACT_MIME_TYPES, responseMimeTypes);
        }
        if (responseRepresentation != null)
        {
            newRequestEventBuilder.addVariable(BEST_MATCH_REPRESENTATION, responseRepresentation);
        }
        newRequestEventBuilder.addVariable(APIKIT_ROUTER_REQUEST, "yes");
        newRequestEventBuilder.addVariable(ACCEPT_HEADER, adapter.getAcceptableResponseMediaTypes());
        return newRequestEventBuilder.build();
    }

    private void processQueryParameters() throws InvalidQueryParameterException
    {
        for (String expectedKey : action.getQueryParameters().keySet())
        {
            IParameter expected = action.getQueryParameters().get(expectedKey);

            Object actual = ((HttpRequestAttributes)requestEvent.getMessage().getAttributes()).getQueryParams().get(expectedKey);
            if (actual == null && expected.isRequired())
            {
                throw new InvalidQueryParameterException("Required query parameter " + expectedKey + " not specified");
            }
            if (actual == null && expected.getDefaultValue() != null)
            {
                setQueryParameter(expectedKey, expected.getDefaultValue());
            }
            if (actual != null)
            {
                if (actual instanceof Collection)
                {
                    if (expected.isArray())
                    {
                        // raml 1.0 array validation
                        validateQueryParam(expectedKey, expected, (Collection<?>) actual);
                        return;
                    }
                    else if (!expected.isRepeat())
                    {
                        throw new InvalidQueryParameterException("Query parameter " + expectedKey + " is not repeatable");
                    }
                }
                if (!(actual instanceof Collection))
                {
                    actual = Collections.singletonList(actual);
                }
                //noinspection unchecked
                for (String param : (Collection<String>) actual)
                {
                    validateQueryParam(expectedKey, expected, param);
                }
            }
        }
    }

    private void validateQueryParam(String paramKey, IParameter expected, Collection<?> paramValue) throws InvalidQueryParameterException
    {
        StringBuilder builder = new StringBuilder();
        for (Object item : paramValue)
        {
            builder.append("- ").append(String.valueOf(item)).append("\n");
        }
        validateQueryParam(paramKey, expected, builder.toString());
    }

    private void validateQueryParam(String paramKey, IParameter expected, String paramValue) throws InvalidQueryParameterException
    {
        if (!expected.validate(paramValue))
        {
            String msg = String.format("Invalid value '%s' for query parameter %s. %s",
                                       paramValue, paramKey, expected.message(paramValue));
            throw new InvalidQueryParameterException(msg);
        }
    }

    private void setQueryParameter(String key, String value)
    {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(key,value);
        requestEvent = EventHelper.addQueryParameters((Event)requestEvent, queryParams);
    }


    @SuppressWarnings("unchecked")
    private void processHeaders() throws InvalidHeaderException
    {
        for (String expectedKey : action.getHeaders().keySet())
        {
            IParameter expected = action.getHeaders().get(expectedKey);
            Map<String, String> incomingHeaders = getIncomingHeaders(requestEvent.getMessage());

            if (expectedKey.contains("{?}"))
            {
                String regex = expectedKey.replace("{?}", ".*");
                for (String incoming : incomingHeaders.keySet())
                {
                    String incomingValue = incomingHeaders.get(incoming);
                    if (incoming.matches(regex) && !expected.validate(incomingValue))
                    {
                        String msg = String.format("Invalid value '%s' for header %s. %s",
                                                   incomingValue, expectedKey, expected.message(incomingValue));
                        throw new InvalidHeaderException(msg);
                    }
                }
            }
            else
            {
                String actual = incomingHeaders.get(expectedKey);
                if (actual == null && expected.isRequired())
                {
                    throw new InvalidHeaderException("Required header " + expectedKey + " not specified");
                }
                if (actual == null && expected.getDefaultValue() != null)
                {
                    setHeader(expectedKey, expected.getDefaultValue());
                }
                if (actual != null)
                {
                    if (!expected.validate(actual))
                    {
                        String msg = String.format("Invalid value '%s' for header %s. %s",
                                                   actual, expectedKey, expected.message(actual));
                        throw new InvalidHeaderException(msg);
                    }
                }
            }
        }
    }

    private Map<String,String> getIncomingHeaders(Message message)
    {

        Map<String, String> incomingHeaders = ((HttpRequestAttributes)message.getAttributes()).getHeaders();//((HttpRestRequest)message.getAttributes()).getIncomingHeaders(message);
        //if (incomingHeaders != null && incomingHeaders.size() > 0)
        //{
        //    incomingHeaders = new CaseInsensitiveHashMap(message.<Map>getInboundProperty("http.headers"));
        //}
        //else
        //{
        //    for (String key : message.getInboundPropertyNames())
        //    {
        //        if (!key.startsWith("http.")) //TODO MULE-8131
        //        {
        //            incomingHeaders.put(key, String.valueOf(message.getInboundProperty(key)));
        //        }
        //    }
        //}
        return incomingHeaders;
    }

    private void setHeader(String key, String value)
    {
        Event.Builder builder = Event.builder((Event)requestEvent);
        InternalMessage.Builder messageBuilder = InternalMessage.builder(requestEvent.getMessage());

        HttpRequestAttributes oldAttributes = ((HttpRequestAttributes)requestEvent.getMessage().getAttributes());
        ParameterMap headers = oldAttributes.getHeaders();
        headers.put(key, value);
        HttpRequestAttributes newAttributes = new HttpRequestAttributes(headers, oldAttributes.getListenerPath(), oldAttributes.getRelativePath(), oldAttributes.getVersion(), oldAttributes.getScheme(), oldAttributes.getMethod(), oldAttributes.getRequestPath(), oldAttributes.getRequestUri(), oldAttributes.getQueryString(), oldAttributes.getQueryParams(), oldAttributes.getUriParams(), oldAttributes.getRemoteAddress(), oldAttributes.getClientCertificate());


        messageBuilder.attributes(newAttributes);
        requestEvent = builder.message(messageBuilder.build()).build();

        //requestEvent.getMessage().setProperty(key, value, PropertyScope.INBOUND);
        //if (requestEvent.getMessage().getInboundProperty("http.headers") != null)
        //{
        //    //TODO MULE-8131
        //    requestEvent.getMessage().<Map<String, String>>getInboundProperty("http.headers").put(key, value);
        //}
    }

    private void negotiateInputRepresentation() throws MuleRestException
    {
        if (action == null || !action.hasBody())
        {
            logger.debug("=== no body types defined: accepting any request content-type");
            return;
        }
        String requestMimeTypeName = null;
        boolean found = false;
        if (adapter.getRequestMediaType() != null)
        {
            requestMimeTypeName = adapter.getRequestMediaType();
        }
        for (String mimeTypeName : action.getBody().keySet())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("comparing request media type %s with expected %s\n",
                                           requestMimeTypeName, mimeTypeName));
            }
            if (mimeTypeName.equals(requestMimeTypeName))
            {
                found = true;
                if (!config.isDisableValidations())
                {
                    validateBody(mimeTypeName);
                }
                break;
            }
        }
        if (!found)
        {
            handleUnsupportedMediaType();
        }
    }

    protected void handleUnsupportedMediaType() throws UnsupportedMediaTypeException
    {
        throw new UnsupportedMediaTypeException();
    }

    private void validateBody(String mimeTypeName) throws MuleRestException
    {
        IMimeType actionMimeType = action.getBody().get(mimeTypeName);
        boolean isJson = mimeTypeName.contains("json");
        boolean isXml = mimeTypeName.contains("xml");
        if (actionMimeType.getSchema() != null && (isXml || isJson))
        {
            if (config.isParserV2())
            {
                validateSchemaV2(actionMimeType, isJson);
            }
            else
            {
                validateSchema(mimeTypeName);
            }
        }
        else if (actionMimeType.getFormParameters() != null &&
                 mimeTypeName.contains("multipart/form-data"))
        {
            validateMultipartForm(actionMimeType.getFormParameters());
        }
        else if (actionMimeType.getFormParameters() != null &&
                 mimeTypeName.contains("application/x-www-form-urlencoded"))
        {
            validateUrlencodedForm(actionMimeType.getFormParameters());
        }
    }

    //TODO FIX METHOD. ENCODING CANNOT BE GET FROM THE EVENT. WE HAVE TO USE THE ENCODING OF THE PAYLOAD OR THE SYSTEM.UTILS ONE.
    @SuppressWarnings("unchecked")
    private void validateUrlencodedForm(Map<String, List<IParameter>> formParameters) throws BadRequestException
    {
        Map<String, String> paramMap;
        //try
        //{
            //if (requestEvent.getMessage().getPayload() instanceof Map)
            //{
                paramMap = (Map<String, String>) requestEvent.getMessage().getPayload().getValue();
            //}
            //else
            //{
                //String encoding = requestEvent.getMessage().getPayload().getDataType().toString();
                //paramMap = (Map) new FormTransformer().transformMessage(requestEvent.getMessage(), requestEvent.getEncoding());
            //}
        //}
        //catch (TransformerException e)
        //{
        //    logger.warn("Cannot validate url-encoded form", e);
        //    return;
        //}

        for (String expectedKey : formParameters.keySet())
        {
            if (formParameters.get(expectedKey).size() != 1)
            {
                //do not perform validation when multi-type parameters are used
                continue;
            }

            IParameter expected = formParameters.get(expectedKey).get(0);
            Object actual = paramMap.get(expectedKey);
            if (actual == null && expected.isRequired())
            {
                throw new InvalidFormParameterException("Required form parameter " + expectedKey + " not specified");
            }
            if (actual == null && expected.getDefaultValue() != null)
            {
                paramMap.put(expectedKey, expected.getDefaultValue());
            }
            if (actual != null && actual instanceof String)
            {
                if (!expected.validate((String) actual))
                {
                    String msg = String.format("Invalid value '%s' for form parameter %s. %s",
                                               actual, expectedKey, expected.message((String) actual));
                    throw new InvalidQueryParameterException(msg);
                }
            }
        }
        //TODO SETPAYLOAD SHOULD USE A MIMETYPE
        requestEvent = EventHelper.setPayload((Event) requestEvent, paramMap);
    }


    //TODO FIX THIS METHOD
    private void validateMultipartForm(Map<String, List<IParameter>> formParameters) throws BadRequestException
    {
        for (String expectedKey : formParameters.keySet())
        {
            if (formParameters.get(expectedKey).size() != 1)
            {
                //do not perform validation when multi-type parameters are used
                continue;
            }
            IParameter expected = formParameters.get(expectedKey).get(0);
            Message data;
            try
            {
                data = ((MultiPartPayload) requestEvent.getMessage().getPayload().getValue()).getPart(expectedKey);
            }
            catch (NoSuchElementException e)
            {
                data = null;
            }
            if (data == null && expected.isRequired())
            {
                //perform only 'required' validation to avoid consuming the stream
                throw new InvalidFormParameterException("Required form parameter " + expectedKey + " not specified");
            }
            if (data == null && expected.getDefaultValue() != null)
            {
                //TODO create message for default values

//                DataHandler defaultDataHandler = new DataHandler(new StringDataSource(expected.getDefaultValue(), expectedKey));
                PartAttributes part1Attributes = new PartAttributes(expectedKey,
                                                            null,
                                                            expected.getDefaultValue().length(),
                                                            Collections.emptyMap());
                Message part1 = Message.builder().payload(expected.getDefaultValue()).attributes(part1Attributes).build();


                try
                {
                    ((MultiPartPayload)requestEvent.getMessage().getPayload().getValue()).getParts().add(part1);
                    //((DefaultMuleMessage) requestEvent.getMessage()).addInboundAttachment(expectedKey, defaultDataHandler);
                }
                catch (Exception e)
                {
                    logger.warn("Cannot set default part " + expectedKey, e);
                }
            }
        }
    }

    //public static class CreatePartsMessageProcessor implements Processor {
    //
    //    @Override
    //    public Event process(Event event) throws MuleException {
    //        PartAttributes part1Attributes = new PartAttributes(TEXT_BODY_FIELD_NAME);
    //        Message part1 = builder().payload(TEXT_BODY_FIELD_VALUE).attributes(part1Attributes).mediaType(TEXT_PLAIN_LATIN).build();
    //        PartAttributes part2Attributes = new PartAttributes(FILE_BODY_FIELD_NAME,
    //                                                            FILE_BODY_FIELD_FILENAME,
    //                                                            FILE_BODY_FIELD_VALUE.length(),
    //                                                            emptyMap());
    //        Message part2 = builder().payload(FILE_BODY_FIELD_VALUE).attributes(part2Attributes).mediaType(BINARY).build();
    //        return Event.builder(event).message(InternalMessage.of(new DefaultMultiPartPayload(part1, part2))).build();
    //    }
    //}


    private void validateSchemaV2(IMimeType mimeType, boolean trimBom) throws BadRequestException
    {
        String payload = getPayloadAsString(trimBom);
        List<ValidationResult> validationResults;
        if (mimeType instanceof org.mule.raml.implv2.v10.model.MimeTypeImpl)
        {
            validationResults = ((org.mule.raml.implv2.v10.model.MimeTypeImpl) mimeType).validate(payload);
        }
        else
        {
            // TODO implement for 08
            // List<ValidationResult> validationResults = ((org.mule.raml.implv2.v08.model.MimeTypeImpl) mimeType).validate(payload);
            throw new RuntimeException("not supported");

        }
        if (!validationResults.isEmpty())
        {
            String message = validationResults.get(0).getMessage();
            logger.info("Schema validation failed: " + message);
            throw new BadRequestException(message);
        }
    }

    private String getPayloadAsString( boolean trimBom) throws BadRequestException
    {
        Object input = requestEvent.getMessage().getPayload().getValue();
        if (input instanceof InputStream)
        {
            logger.debug("transforming payload to perform Schema validation");
            try
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copyLarge((InputStream) input, baos);
                byte[] bytes = baos.toByteArray();

                String charset = getEncoding((Event)requestEvent, muleContext, bytes, logger);
                requestEvent = EventHelper.setPayload((Event)requestEvent, new ByteArrayInputStream(baos.toByteArray()), requestEvent.getMessage().getPayload().getDataType().getMediaType());
                input = byteArrayToString(bytes, charset, trimBom);
            }
            catch (IOException e)
            {
                throw new BadRequestException("Error processing request: " + e.getMessage());
            }
        }
        else if (input instanceof byte[])
        {
            try
            {
                String encoding = getEncoding((Event)requestEvent, muleContext, (byte[]) input, logger);

                input = byteArrayToString((byte[]) input, encoding, trimBom);
            }
            catch (IOException e)
            {
                throw new BadRequestException("Error processing request: " + e.getMessage());
            }
        }
        else if (input instanceof String)
        {
            // already in the right format
        }
        else
        {
            throw new BadRequestException("Don't know how to parse " + input.getClass().getName());
        }
        return (String) input;
    }

    private String byteArrayToString(byte[] bytes, String charset, boolean trimBom) throws IOException
    {
        String result;
        if (trimBom)
        {
            result = IOUtils.toString(new ByteArrayInputStream(trimBom(bytes)), charset);
        }
        else
        {
            result = IOUtils.toString(bytes, charset);
        }
        return result;
    }

    private void validateSchema(String mimeTypeName) throws MuleRestException
    {
        SchemaType schemaType = mimeTypeName.contains("json") ? SchemaType.JSONSchema : SchemaType.XMLSchema;
        RestSchemaValidator validator = RestSchemaValidatorFactory.getInstance().createValidator(schemaType, config.getMuleContext());
        requestEvent = validator.validate(config.getName(), SchemaCacheUtils.getSchemaCacheKey(action, mimeTypeName), (Event)requestEvent, config.getApi());
    }

    private String negotiateOutputRepresentation(List<String> mimeTypes) throws MuleRestException
    {
        if (action == null || action.getResponses() == null || mimeTypes.isEmpty())
        {
            //no response media-types defined, return no body
            return null;
        }
        MediaType bestMatch = RestContentTypeParser.bestMatch(mimeTypes, adapter.getAcceptableResponseMediaTypes());
        if (bestMatch == null)
        {
            return handleNotAcceptable();
        }
        logger.debug("=== negotiated response content-type: " + bestMatch.toString());
        for (String representation : mimeTypes)
        {
            if (representation.equals(bestMatch.withoutParameters().toString()))
            {
                return representation;
            }
        }
        return handleNotAcceptable();
    }

    protected String handleNotAcceptable() throws NotAcceptableException
    {
        throw new NotAcceptableException();
    }

    private List<String> getResponseMimeTypes()
    {
        List<String> mimeTypes = new ArrayList<>();
        int status = getSuccessStatus();
        if (status != -1)
        {
            IResponse response = action.getResponses().get(String.valueOf(status));
            if (response != null && response.hasBody())
            {
                Map<String, IMimeType> interfacesOfTypes = response.getBody();
                for (Map.Entry<String, IMimeType> entry : interfacesOfTypes.entrySet())
                {
                    mimeTypes.add(entry.getValue().getType());
                }
                logger.debug(String.format("=== adding response mimeTypes for status %d : %s", status, mimeTypes));
            }
        }
        return mimeTypes;
    }

    protected int getSuccessStatus()
    {
        for (String status : action.getResponses().keySet())
        {
            int code = Integer.parseInt(status);
            if (code >= 200 && code < 300)
            {
                return code;
            }
        }
        //default success status
        return 200;
    }

    public void updateEvent(Event event)
    {
        requestEvent = event;
    }

}
