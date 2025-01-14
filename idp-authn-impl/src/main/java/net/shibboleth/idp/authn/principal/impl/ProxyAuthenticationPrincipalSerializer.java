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

package net.shibboleth.idp.authn.principal.impl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Principal;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;
import jakarta.json.stream.JsonGenerator;

import net.shibboleth.idp.authn.principal.AbstractPrincipalSerializer;
import net.shibboleth.idp.authn.principal.ProxyAuthenticationPrincipal;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * Principal serializer for {@link ProxyAuthenticationPrincipal}.
 */
@ThreadSafe
public class ProxyAuthenticationPrincipalSerializer extends AbstractPrincipalSerializer<String> {

    /** Field name of authority content. */
    @Nonnull @NotEmpty private static final String PROXY_AUTH_FIELD = "AA";

    /** Field name of proxy count. */
    @Nonnull @NotEmpty private static final String PROXY_COUNT_FIELD = "PXC";

    /** Field name of proxy audiences. */
    @Nonnull @NotEmpty private static final String PROXY_AUD_FIELD = "AUD";

    /** Pattern used to determine if input is supported. */
    private static final Pattern JSON_PATTERN = Pattern.compile("^\\{\"AA\":.*\\}$");

    /** JSON object bulder factory. */
    @Nonnull private final JsonBuilderFactory objectBuilderFactory;

    /** Constructor. */
    public ProxyAuthenticationPrincipalSerializer() {
        final JsonBuilderFactory factory = Json.createBuilderFactory(null);
        assert factory != null;
        objectBuilderFactory = factory;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean supports(@Nonnull final Principal principal) {
        return principal instanceof ProxyAuthenticationPrincipal;
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String serialize(@Nonnull final Principal principal) throws IOException {
        
        final ProxyAuthenticationPrincipal proxyPrincipal = (ProxyAuthenticationPrincipal) principal;
        
        final JsonArrayBuilder arrayBuilder = getJsonArrayBuilder();
        proxyPrincipal.getAuthorities().forEach(arrayBuilder::add);

        final StringWriter sink = new StringWriter(32);
        
        try (final JsonGenerator gen = getJsonGenerator(sink)) {
            gen.writeStartObject().write(PROXY_AUTH_FIELD, arrayBuilder.build());
            
            final Integer proxyCount = proxyPrincipal.getProxyCount();
            if (proxyCount != null) {
                gen.write(PROXY_COUNT_FIELD, proxyCount );
            }
            
            if (!proxyPrincipal.getAudiences().isEmpty()) {
                final JsonArrayBuilder arrayBuilder2 = getJsonArrayBuilder();
                proxyPrincipal.getAudiences().forEach(arrayBuilder2::add);
                gen.write(PROXY_AUD_FIELD, arrayBuilder2.build());
            }
            
            gen.writeEnd();
        }
        final String result = sink.toString();
        assert result != null;
        return result;
    }
    
    /** {@inheritDoc} */
    public boolean supports(@Nonnull @NotEmpty final String value) {
        return JSON_PATTERN.matcher(value).matches();
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Nullable public ProxyAuthenticationPrincipal deserialize(@Nonnull @NotEmpty final String value)
            throws IOException {
        
        try (final JsonReader reader = getJsonReader(new StringReader(value))) {
            final JsonStructure st = reader.read();
            if (!(st instanceof JsonObject)) {
                throw new IOException("Found invalid data structure while parsing ProxyAuthenticationPrincipal");
            }
            
            final JsonValue jsonValue = ((JsonObject) st).get(PROXY_AUTH_FIELD);
            if (jsonValue != null && ValueType.ARRAY.equals(jsonValue.getValueType())) {
                final ProxyAuthenticationPrincipal ret = new ProxyAuthenticationPrincipal();
                for (final JsonValue e : (JsonArray) jsonValue) {
                    if (ValueType.STRING.equals(e.getValueType())) {
                        ret.getAuthorities().add(((JsonString) e).getString());
                    }
                }
                
                final JsonValue count = ((JsonObject) st).get(PROXY_COUNT_FIELD);
                if (count != null) {
                    if (ValueType.NUMBER.equals(count.getValueType())) {
                        ret.setProxyCount(((JsonNumber) count).intValueExact());
                    } else {
                        throw new IOException(
                                "Found invalid data structure while parsing ProxyAuthenticationPrincipal");
                    }
                }
                
                final JsonValue audiences = ((JsonObject) st).get(PROXY_AUD_FIELD);
                if (audiences != null) {
                    if (ValueType.ARRAY.equals(audiences.getValueType())) {
                        for (final JsonValue e : (JsonArray) audiences) {
                            if (ValueType.STRING.equals(e.getValueType())) {
                                ret.getAudiences().add(((JsonString) e).getString());
                            }
                        }
                    } else {
                        throw new IOException(
                                "Found invalid data structure while parsing ProxyAuthenticationPrincipal");
                    }
                }
                
                return ret;
            }
            throw new IOException("Serialized ProxyAuthenticationPrincipal missing primary array field");
        } catch (final JsonException e) {
            throw new IOException("Found invalid data structure while parsing ProxyAuthenticationPrincipal", e);
        }
    }
// Checkstyle: CyclomaticComplexity ON


     /**
      * Get a {@link JsonArrayBuilder} in a thread-safe manner.
      *
      * @return  an array builder
      */
     @Nonnull private synchronized JsonArrayBuilder getJsonArrayBuilder() {
        final JsonArrayBuilder result = objectBuilderFactory.createArrayBuilder();
        assert result != null;
        return result;
     }   
}

