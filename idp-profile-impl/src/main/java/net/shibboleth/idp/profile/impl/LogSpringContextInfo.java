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

package net.shibboleth.idp.profile.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

//Checkstyle: JavadocStyle OFF -- ignore extra HTML tag error
/**
 * Spring Web Flow utility action for logging on DEBUG details about the current hierarchy of
 * Spring {@link ApplicationContext} and the beans contained within each.
 * 
 * <p>
 * You can contextualize the logging of the info either by setting {@link #setDescription(String)},
 * or more usefully by using an attribute on the specific action expression as below.  This allows using
 * just one declaration of the action bean, but parameterized differently depending on where it is placed.
 * </p>
 * 
 * <pre>
 * {@code
 * <evaluate expression="LogSpringContextInfo">
 *    <attribute name="springInfoDescription" value="My Description" />
 * </evaluate>
 *  }
 * </pre>
 */
//Checkstyle: JavadocStyle ON
public class LogSpringContextInfo extends AbstractProfileAction implements ApplicationContextAware {
    
    /** Name of Spring web flow attribute holding the description of the tree to log. */
    @Nonnull @NotEmpty public static final String ATTRIB_DESC = "springInfoDescription";
    
    /** Logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger("SPRING_CONTEXT_INFO");
    
    /** The owning Spring ApplicationContext in which this action is defined. */
    @Nullable private ApplicationContext applicationContext;
    
    /** Contextual description to output at the start of the action. */
    @Nullable private String description;
    
    /**
     * Set the contextual description to output at the start of the action.
     * 
     * @param value the description value
     */
    public void setDescription(@Nullable final String value) {
        description = StringSupport.trimOrNull(value);
    }
    
    /** {@inheritDoc} */
    public void setApplicationContext(@Nonnull final ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!log.isDebugEnabled()) {
            // short-circuit if not logging at debug
            return;
        }
        
        String contextualDescription = null;
        
        final SpringRequestContext springRequestContext =
                profileRequestContext.getSubcontext(SpringRequestContext.class);
        if (springRequestContext != null && springRequestContext.getRequestContext() != null) {
            final RequestContext requestContext = springRequestContext.getRequestContext();
            assert requestContext!= null;
            contextualDescription = requestContext.getAttributes().getString(ATTRIB_DESC);
        }
        
        if (contextualDescription == null) {
            contextualDescription = description;
        }
        
        if (contextualDescription != null) {
            log.debug("Spring ApplicationContext hierarchy contextual description: {}", contextualDescription) ;
        }
        
        log.debug("**********************************************************************************************");
        
        ApplicationContext current = applicationContext;
        while (current != null) {
            log.debug("Spring Context: {}", current.toString());
            log.debug("Spring Context Name: {}", current.getApplicationName());
            log.debug("Spring Context Parent: {}", current.getParent());
            log.debug("");
            log.debug("Spring Context Bean Definition Count: {}", current.getBeanDefinitionCount());
            log.debug("");
            log.debug("Spring Context Bean Details:");
            log.debug("");
            
            for (final String beanName : current.getBeanDefinitionNames()) {
                assert beanName != null;
                final Class<?> type = current.getType(beanName);
                assert type != null;
                log.debug(String.format("Spring Bean id: %s, singleton?: %s, prototype?: %s, type: %s",
                        beanName, current.isSingleton(beanName), current.isPrototype(beanName), 
                        type.getName()));
            }
            
            log.debug("**********************************************************************************************");
            current = current.getParent();
        }
        
    }
    
}