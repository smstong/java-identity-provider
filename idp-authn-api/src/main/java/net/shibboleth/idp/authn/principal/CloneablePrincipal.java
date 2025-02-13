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

package net.shibboleth.idp.authn.principal;

import java.security.Principal;

import javax.annotation.Nonnull;

/** Principal that can be cloned without knowledge of the underlying type. */
public interface CloneablePrincipal extends Principal, Cloneable {

    /**
     * Creates and returns a copy of this object.
     * 
     * @return     a clone of this instance.
     * @exception  CloneNotSupportedException if the instance cannot
     *             be cloned.
     *
     * @see java.lang.Object#clone
     */
    @Nonnull Object clone() throws CloneNotSupportedException;
}