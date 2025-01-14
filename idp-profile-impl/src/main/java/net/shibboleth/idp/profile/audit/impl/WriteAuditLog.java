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

package net.shibboleth.idp.profile.audit.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPAuditFields;
import net.shibboleth.profile.context.AuditContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.shared.servlet.HttpServletSupport;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Action that produces audit log entries based on an {@link AuditContext} and one or more formatting strings. 
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 */
public class WriteAuditLog extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(WriteAuditLog.class);
    
    /** Strategy used to locate the {@link AuditContext} associated with a given {@link ProfileRequestContext}. */
    @Nonnull private Function<ProfileRequestContext,AuditContext> auditContextLookupStrategy;
    
    /** Map of log category to formatting tokens and literals to output. */
    @Nonnull @NotEmpty private Map<String,List<String>> formattingMap;

    /** Explicit categories to log from {@link #formattingMap}. */
    @Nonnull @NotEmpty private Collection<String> categoriesToLog;
    
    /** Formatter for date/time fields. */
    @Nonnull private DateTimeFormatter dateTimeFormatter;
    
    /** Convert date/time fields to default time zone. */
    private boolean useDefaultTimeZone;

    /** Include profile's logging ID in category. */
    private boolean includeProfileLoggingId;
    
    /** The Spring RequestContext to operate on. */
    @Nullable private RequestContext requestContext;

    /** The AuditContext to operate on. */
    @Nullable private AuditContext auditCtx;

    /** Constructor. */
    @SuppressWarnings("null")
    public WriteAuditLog() {
        auditContextLookupStrategy = new ChildContextLookup<>(AuditContext.class);
        formattingMap = CollectionSupport.emptyMap();
        categoriesToLog = CollectionSupport.emptyList();
        dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;
        includeProfileLoggingId = true;
    }

    /**
     * Set the strategy used to locate the {@link AuditContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setAuditContextLookupStrategy(@Nonnull final Function<ProfileRequestContext,AuditContext> strategy) {
        checkSetterPreconditions();
        auditContextLookupStrategy = Constraint.isNotNull(strategy, "AuditContext lookup strategy cannot be null");
    }
    
    /**
     * Get the map of logging category to formatting tokens for log entries.
     * 
     * @return map of formatting tokens
     */
    @Nonnull @NotLive @Unmodifiable public Map<String,List<String>> getFormattingMap() {
        return CollectionSupport.copyToMap(formattingMap);
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /**
     * Set the map of logging category to formatting strings for log entries.
     * 
     * <p>A formatting string consists of tokens prefixed by '%' separated by any non-alphanumeric or whitespace.
     * Tokens can contain any letter or number or a hyphen. Anything other than a token, including whitespace, is
     * a literal.</p>
     * 
     * @param map map of categories to formatting strings
     */
    public void setFormattingMap(@Nullable final Map<String,String> map) {
        checkSetterPreconditions();
        
        if (map == null) {
            formattingMap = CollectionSupport.emptyMap();
            return;
        }
        
        formattingMap = new HashMap<>(map.size());
        
        for (final Map.Entry<String,String> entry : map.entrySet()) {
            final String category = StringSupport.trimOrNull(entry.getKey());
            final String s = StringSupport.trimOrNull(entry.getValue());
            if (category == null || s == null) {
                continue;
            }
            
            final int len = s.length();
            boolean inToken = false;
            final List<String> format = new ArrayList<>();
            final StringBuilder field = new StringBuilder();
            for (int pos = 0; pos < len; ++pos) {
                final char ch = s.charAt(pos);
                if (inToken) {
                    if (!Character.isLetterOrDigit(ch) && ch != '-' && ch != '%') {
                        format.add(field.toString());
                        field.setLength(0);
                        inToken = false;
                    }
                } else if (ch == '%') {
                    if (field.length() > 0) {
                        format.add(field.toString());
                        field.setLength(0);
                    }
                    inToken = true;
                }
                
                field.append(ch);
            }
            
            if (field.length() > 0) {
                format.add(field.toString());
            }
            
            formattingMap.put(category, format);
        }
    }
// Checkstyle: CyclomaticComplexity ON

    /**
     * Set categories to log explicitly.
     * 
     * <p>In the absence of any, the original behavior that iterates over the whole formatting map and logs
     * each key is retained.</p>
     * 
     * @param categories categories to log
     * 
     * @since 4.3.0
     */
    public void setCategoriesToLog(@Nullable final Collection<String> categories) {
        checkSetterPreconditions();
        
        if (categories != null) {
            categoriesToLog = CollectionSupport.copyToList(categories);
        } else {
            categoriesToLog = CollectionSupport.emptyList();
        }
    }
    
    /**
     * Set the formatting string to apply when extracting date/time fields.
     * 
     * @param format formatting string
     */
    @SuppressWarnings("null")
    public void setDateTimeFormat(@Nullable @NotEmpty final String format) {
        checkSetterPreconditions();
        if (format != null) {
            dateTimeFormatter = DateTimeFormatter.ofPattern(StringSupport.trimOrNull(format));
        }
    }
    
    /**
     * Convert date/time fields to default time zone.
     * 
     * @param flag flag to set
     */
    public void setUseDefaultTimeZone(final boolean flag) {
        checkSetterPreconditions();
        useDefaultTimeZone = flag;
    }
    
    /**
     * Sets whether to suffix the profile's logging ID to the category to log against.
     * 
     * <p>Defaults to true.</p>
     * 
     * @param flag flag to set
     * 
     * @since 4.3.0
     */
    public void setIncludeProfileLoggingId(final boolean flag) {
        checkSetterPreconditions();
        includeProfileLoggingId = flag;
    }
    
    /** {@inheritDoc} */
    @SuppressWarnings("null")
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (useDefaultTimeZone) {
            dateTimeFormatter = dateTimeFormatter.withZone(ZoneId.systemDefault());
        } else {
            dateTimeFormatter = dateTimeFormatter.withZone(ZoneOffset.UTC);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) {
        requestContext = springRequestContext;
        return super.doExecute(springRequestContext, profileRequestContext);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        } else if (formattingMap.isEmpty()) {
            log.debug("No formatting for audit records supplied, nothing to do");
            return false;
        }
        
        auditCtx = auditContextLookupStrategy.apply(profileRequestContext);
        return true;
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        for (final Map.Entry<String,List<String>> entry : formattingMap.entrySet()) {
            
            if (!categoriesToLog.isEmpty() && !categoriesToLog.contains(entry.getKey())) {
                continue;
            }
        
            final StringBuilder record = new StringBuilder();
    
            for (final String token : entry.getValue()) {
                if (token.startsWith("%")) {
                    if (token.length() == 1 || token.charAt(1) == '%') {
                        record.append('%');
                    } else {
                        final String field = token.substring(1);
                        final HttpServletRequest httpRequest = getHttpServletRequest();
                        
                        if (IdPAuditFields.EVENT_TIME.equals(field)) {
                            record.append(dateTimeFormatter.format(Instant.now()));
                        } else if (IdPAuditFields.EVENT_TYPE.equals(field)) {
                            assert requestContext != null;
                            final Event event = requestContext.getCurrentEvent();
                            if (event != null && !event.getId().equals(EventIds.PROCEED_EVENT_ID)) {
                                record.append(event.getId());
                            }
                        } else if (IdPAuditFields.PROFILE.equals(field)) {
                            record.append(profileRequestContext.getProfileId());
                        } else if (IdPAuditFields.REMOTE_ADDR.equals(field) && httpRequest != null) {
                            record.append(HttpServletSupport.getRemoteAddr(httpRequest));
                        } else if (IdPAuditFields.URI.equals(field) && httpRequest != null) {
                            record.append(httpRequest.getRequestURI());
                        } else if (IdPAuditFields.URL.equals(field) && httpRequest != null) {
                            record.append(httpRequest.getRequestURL());
                        } else if (IdPAuditFields.USER_AGENT.equals(field) && httpRequest != null) {
                            record.append(httpRequest.getHeader("User-Agent"));
                        } else if (auditCtx != null) {
                            assert field != null;
                            final Iterator<String> iter = auditCtx.getFieldValues(field).iterator();
                            while (iter.hasNext()) {
                                record.append(iter.next());
                                if (iter.hasNext()) {
                                    record.append(',');
                                }
                            }
                        }
                    }
                } else {
                    record.append(token);
                }
            }
            
            filter(record);
            
            final String category;
            if (includeProfileLoggingId) {
                category = entry.getKey() + '.' + profileRequestContext.getLoggingId();
            } else {
                category = entry.getKey();
            }
            assert category != null;
            LoggerFactory.getLogger(category).info(record.toString());
        }
    }
// Checkstyle: CyclomaticComplexity ON
    
    /**
     * Optional override to filter the outgoing log message, does nothing by default.
     * 
     * @param entry log entry
     */
    protected void filter(@Nonnull final StringBuilder entry) {
        
    }
    
}