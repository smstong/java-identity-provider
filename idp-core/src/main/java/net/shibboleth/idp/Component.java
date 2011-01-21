/*
 * Copyright 2011 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp;

/**
 * A discrete component that provides a specific function to the system. Components carry a unique identifier and may be
 * validated to ensure they are operating properly.
 */
public interface Component {

    /**
     * Gets the ID of this component.
     * 
     * @return ID of this component, never null
     */
    public String getId();

    /**
     * Validates that this component is operational and function properly (with the limits that such things can be
     * checked).
     * 
     * @throws ComponentValidationException thrown if there is a problem with the component
     */
    public void validate() throws ComponentValidationException;
}