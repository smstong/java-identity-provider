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

package net.shibboleth.idp.profile.context.navigate;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.primitive.StringSupport;

import org.springframework.webflow.execution.Event;

/**
 * A function that returns a view name to render based on a Spring Web Flow {@link Event}.
 */
public class SpringEventToViewLookupFunction implements Function<Event,String> {

    /** Default view name to return. */
    @Nullable private String defaultView;
    
    /** Map of event ID to view name. */
    @Nonnull private Map<String,String> eventMap;
    
    /** Constructor. */
    public SpringEventToViewLookupFunction() {
        eventMap = CollectionSupport.emptyMap();
    }
    
    /**
     * Set the default view name.
     * 
     * @param view default view name
     */
    public void setDefaultView(@Nullable final String view) {
        defaultView = StringSupport.trimOrNull(view);
    }
    
    /**
     * Set the map of event IDs to view names.
     * 
     * @param map map to use
     */
    public void setEventMap(@Nullable final Map<String,String> map) {
        if (map == null) {
            eventMap = CollectionSupport.emptyMap();
        } else {
            eventMap = new HashMap<>(map.size());
            for (final Map.Entry<String,String> entry : map.entrySet()) {
                final String eventId = StringSupport.trimOrNull(entry.getKey());
                final String viewName = StringSupport.trimOrNull(entry.getValue());
                if (eventId != null && viewName != null) {
                    eventMap.put(eventId, viewName);
                }
            }
        }
    }
    
    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final Event input) {
        
        if (input != null) {
            final String view = eventMap.get(input.getId());
            return view != null ? view : defaultView;
        }
        return null;
    }

}