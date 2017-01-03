/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit.validation;

import org.mule.module.apikit.exception.BadRequestException;
import org.mule.raml.interfaces.model.IRaml;
import org.mule.runtime.core.api.Event;

public interface RestSchemaValidator
{

    Event validate(String configId, String schemaPath, Event muleEvent, IRaml api) throws BadRequestException;
}
