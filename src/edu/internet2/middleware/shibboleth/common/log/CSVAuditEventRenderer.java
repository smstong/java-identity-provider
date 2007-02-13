/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.log;

import org.apache.log4j.or.ObjectRenderer;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.opensaml.xml.util.DatatypeHelper;

/**
 * Renders a {@link AuditLogEntry} as a character seperated string. The format of the string is, which makes up the
 * message component of a Lo4J entry, is:
 * 
 * <code>timestamp|relyingParty|requestBinding|responseBinding|messageProfile|requestID|principalID|authNMethod1,authNMethod2,etc,|attributeID1,attributeID2,|</code>
 */
public class CSVAuditEventRenderer implements ObjectRenderer {

    /** Formatter used to convert timestamps to strings. */
    private static DateTimeFormatter dateFormatter = ISODateTimeFormat.basicDateTimeNoMillis();
    
    /** {@inheritDoc} */
    public String doRender(Object obj) {
        if(!(obj instanceof AuditLogEntry)){
            return null;
        }
        
        AuditLogEntry entry = (AuditLogEntry) obj;
        StringBuilder entryString = new StringBuilder();
        
        entryString.append(entry.getAuditEventTime().toString(dateFormatter.withZone(DateTimeZone.UTC)));
        entryString.append("|");
        
        entryString.append(entry.getRelyingParty());
        entryString.append("|");
        
        entryString.append(entry.getRequestBinding());
        entryString.append("|");
        
        entryString.append(entry.getResponseBinding());
        entryString.append("|");
        
        entryString.append(entry.getMessageProfile());        
        entryString.append("|");
        
        if(!DatatypeHelper.isEmpty(entry.getRequestId())){
            entryString.append(entry.getRequestId());
        }
        entryString.append("|");
        
        entryString.append(entry.getPrincipalId());
        entryString.append("|");
        
        for(String method : entry.getPrincipalAuthenticationMethods()){
            entryString.append(method);
            entryString.append(",");
        }
        entryString.append("|");
        
        for(String attribute : entry.getReleasedAttributes()){
            entryString.append(attribute);
            entryString.append(",");
        }
        entryString.append("|");
        
        return entryString.toString();
    }
}