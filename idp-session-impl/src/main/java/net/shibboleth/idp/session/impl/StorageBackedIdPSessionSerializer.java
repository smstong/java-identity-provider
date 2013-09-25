/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.session.impl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.stream.JsonGenerator;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.session.BaseIdPSession;
import net.shibboleth.idp.session.ServiceSession;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link ServiceSession} serializers that handles data common to all such objects.
 */
@ThreadSafe
public class StorageBackedIdPSessionSerializer implements StorageSerializer<StorageBackedIdPSession> {

    /** Field name of creation instant. */
    private static final String CREATION_INSTANT_FIELD = "ts";

    /** Field name of principal name. */
    private static final String PRINCIPAL_NAME_FIELD = "nam";
    
    /** Field name of IPv4 address. */
    private static final String IPV4_ADDRESS_FIELD = "v4";

    /** Field name of IPv6 address. */
    private static final String IPV6_ADDRESS_FIELD = "v6";
    
    /** Field name of flow ID array. */
    private static final String FLOW_ID_ARRAY_FIELD = "flows";

    /** Field name of service ID array. */
    private static final String SERVICE_ID_ARRAY_FIELD = "svcs";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StorageBackedIdPSessionSerializer.class);
    
    /** Back-reference to parent instance. */
    @Nonnull private final StorageBackedSessionManager sessionManager;
    
    /** Object instance to overwrite with deserialization method. */
    @Nullable private StorageBackedIdPSession targetObject;
    
    /**
     * Constructor.
     * 
     * @param manager parent SessionManager instance
     * @param target object to overwrite when deseralizing instead of creating a new instance
     */
    public StorageBackedIdPSessionSerializer(@Nonnull final StorageBackedSessionManager manager,
            @Nullable final StorageBackedIdPSession target) {
        sessionManager = Constraint.isNotNull(manager, "SessionManager cannot be null");
        targetObject = target;
    }
    
    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String serialize(@Nonnull final StorageBackedIdPSession instance) throws IOException {
        
        try {
            final StringWriter sink = new StringWriter(128);
            final JsonGenerator gen = Json.createGenerator(sink);
            gen.writeStartObject()
                .write(CREATION_INSTANT_FIELD, instance.getCreationInstant())
                .write(PRINCIPAL_NAME_FIELD, instance.getPrincipalName());
            
            if (instance.getAddress(BaseIdPSession.AddressFamily.IPV4) != null) {
                gen.write(IPV4_ADDRESS_FIELD, instance.getAddress(BaseIdPSession.AddressFamily.IPV4));
            }

            if (instance.getAddress(BaseIdPSession.AddressFamily.IPV6) != null) {
                gen.write(IPV6_ADDRESS_FIELD, instance.getAddress(BaseIdPSession.AddressFamily.IPV6));
            }
            
            Set<AuthenticationResult> results = instance.getAuthenticationResults();
            if (!results.isEmpty()) {
                gen.writeStartArray(FLOW_ID_ARRAY_FIELD);
                for (AuthenticationResult result : results) {
                    gen.write(result.getAuthenticationFlowId());
                }
                gen.writeEnd();
            }
            
            if (sessionManager.isTrackServiceSessions()) {
                Set<ServiceSession> services = instance.getServiceSessions();
                if (!services.isEmpty()) {
                    gen.writeStartArray(SERVICE_ID_ARRAY_FIELD);
                    for (ServiceSession service : services) {
                        gen.write(service.getId());
                    }
                    gen.writeEnd();
                }
            }
            
            gen.writeEnd().close();
            
            return sink.toString();
        } catch (JsonException e) {
            log.error("Exception while serializing IdPSession", e);
            throw new IOException("Exception while serializing IdPSession", e);
        }
    }

    /** {@inheritDoc} */
    // Checkstyle: CyclomaticComplexity OFF
    @Nonnull public StorageBackedIdPSession deserialize(final int version, @Nonnull @NotEmpty final String context,
            @Nonnull @NotEmpty final String key, @Nonnull @NotEmpty final String value, @Nullable final Long expiration)
                    throws IOException {
        
        if (expiration == null) {
            throw new IOException("IdPSession objects must have an expiration");
        }

        try {
            final JsonReader reader = Json.createReader(new StringReader(value));
            final JsonStructure st = reader.read();
            if (!(st instanceof JsonObject)) {
                throw new IOException("Found invalid data structure while parsing IdPSession");
            }
            final JsonObject obj = (JsonObject) st;
            
            // Create new object if necessary.
            if (targetObject == null) {
                final long creation = obj.getJsonNumber(CREATION_INSTANT_FIELD).longValueExact();
                final String principalName = obj.getString(PRINCIPAL_NAME_FIELD);
                targetObject = new StorageBackedIdPSession(sessionManager, context, principalName, creation);
            }
            
            // Populate fields in-place, bypassing any storage interactions.
            targetObject.setVersion(version);
            targetObject.doSetLastActivityInstant(
                    expiration - sessionManager.getSessionTimeout() - sessionManager.getSessionSlop());
            if (obj.containsKey(IPV4_ADDRESS_FIELD)) {
                targetObject.doBindToAddress(obj.getString(IPV4_ADDRESS_FIELD));
            }
            if (obj.containsKey(IPV6_ADDRESS_FIELD)) {
                targetObject.doBindToAddress(obj.getString(IPV6_ADDRESS_FIELD));
            }
            
            if (obj.containsKey(FLOW_ID_ARRAY_FIELD)) {
                JsonArray flowIds = obj.getJsonArray(FLOW_ID_ARRAY_FIELD);
                if (flowIds != null) {
                    for (JsonString flowId : flowIds.getValuesAs(JsonString.class)) {
                        targetObject.getAuthenticationFlowIds().add(flowId.getString());
                    }
                }
            }

            if (obj.containsKey(SERVICE_ID_ARRAY_FIELD)) {
                JsonArray svcIds = obj.getJsonArray(SERVICE_ID_ARRAY_FIELD);
                if (svcIds != null) {
                    for (JsonString svcId : svcIds.getValuesAs(JsonString.class)) {
                        targetObject.getServiceIds().add(svcId.getString());
                    }
                }
            }
            
            return targetObject;
            
        } catch (NullPointerException | ClassCastException | ArithmeticException | JsonException e) {
            log.error("Exception while parsing IdPSession", e);
            throw new IOException("Found invalid data structure while parsing IdPSession", e);
        }
    }
    // Checkstyle: CyclomaticComplexity ON

}