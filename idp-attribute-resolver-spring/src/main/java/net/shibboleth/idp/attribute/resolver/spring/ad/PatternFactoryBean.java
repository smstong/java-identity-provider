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

package net.shibboleth.idp.attribute.resolver.spring.ad;

import java.util.regex.Pattern;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Factory bean for {@link Pattern}. Allows us to inject property based case sensitivity.
 */
public class PatternFactoryBean extends AbstractFactoryBean<Pattern> {

    /** Whether the we are case sensitive or not. */
    private Boolean caseSensitive;

    /** The regular expressions. */
    private String regexp;

    /** {@inheritDoc} */
    @Override public Class<?> getObjectType() {
        return Pattern.class;
    }

    /**
     * set case sensitivity.
     * 
     * @return Returns the caseSensitive.
     */
    public Boolean getCaseSensitive() {
        return caseSensitive;
    }

    /**
     * get case sensitivity.
     * 
     * @param what The value to set.
     */
    public void setCaseSensitive(Boolean what) {
        caseSensitive = what;
    }

    /**
     * Get the regular expression.
     * 
     * @return Returns the regexp.
     */
    public String getRegexp() {
        return regexp;
    }

    /**
     * Set the regular expression.
     * 
     * @param what what to set.
     */
    public void setRegexp(String what) {
        regexp = what;
    }

    /** {@inheritDoc} */
    @Override protected Pattern createInstance() throws Exception {
        if (null == getCaseSensitive() || getCaseSensitive()) {
            return Pattern.compile(regexp, 0);
        } else {
            return Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
        }
    }

}
