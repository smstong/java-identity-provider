/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.saml.nameid.impl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.stream.JsonGenerator;

import org.slf4j.Logger;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * The Parameters we need to store in, and get out of a transient ID, namely the attribute recipient (aka the SP) and
 * the principal. Having this as a separate class allows streamlining of the encoding/decoding.
 */
public class TransientIdParameters {
    /** Context label for storage of IDs. */
    @Nonnull @NotEmpty public static final String CONTEXT = "TransientId";
    
    /** Field name of creation instant. */
    @Nonnull @NotEmpty private static final String ATTRIBUTE_RECIPIENT_FIELD = "sp";

    /** Field name of principal name. */
    @Nonnull @NotEmpty private static final String PRINCIPAL_FIELD = "princ";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(TransientIdParameters.class);

    /** The Attribute Recipient. */
    @Nullable private final String attributeRecipient;

    /** The principal. */
    @Nullable private final String principal;

    /**
     * Constructor for the attribute definition.
     * 
     * @param recipient the SP
     * @param thePrincipal the user
     */
    public TransientIdParameters(@Nullable final String recipient, @Nullable final String thePrincipal) {
        attributeRecipient = recipient;
        principal = thePrincipal;
    }

    /**
     * Constructor for the decoding definitions.
     * 
     * @param encoded the JSON encoded data
     * @throws IOException if decoding failed
     */
    public TransientIdParameters(@Nonnull @NotEmpty final String encoded) throws IOException {
        Constraint.isNotNull(StringSupport.trimOrNull(encoded), "encoded data must not be null or empty");

        final JsonReader reader = Json.createReader(new StringReader(encoded));
        final JsonStructure st = reader.read();

        if (!(st instanceof JsonObject)) {
            throw new IOException("Found invalid data structure while parsing IdPSession");
        }
        final JsonObject jsonObj = (JsonObject) st;

        principal = jsonObj.getString(PRINCIPAL_FIELD);
        attributeRecipient = jsonObj.getString(ATTRIBUTE_RECIPIENT_FIELD);
    }

    /**
     * Get the SP.
     * 
     * @return the sp.
     */
    @Nullable public String getAttributeRecipient() {
        return attributeRecipient;
    }

    /**
     * Get the Principal.
     * 
     * @return the principal
     */
    @Nullable public String getPrincipal() {
        return principal;
    }

    /**
     * Encode up for storing.
     * 
     * @return the encoded string.
     * @throws IOException if encoding failed
     */
    @Nonnull public String encode() throws IOException {
        try {
            final StringWriter sink = new StringWriter(128);
            final JsonGenerator gen = Json.createGenerator(sink);
            gen.writeStartObject().write(ATTRIBUTE_RECIPIENT_FIELD, getAttributeRecipient())
                    .write(PRINCIPAL_FIELD, getPrincipal());
            gen.writeEnd().close();
            final String result = sink.toString();
            assert result != null;
            return result;
        } catch (final JsonException e) {
            log.error("Exception while serializing TransientID: {}", e.getMessage());
            throw new IOException("Exception while serializing TransientID", e);
        }
    }

}