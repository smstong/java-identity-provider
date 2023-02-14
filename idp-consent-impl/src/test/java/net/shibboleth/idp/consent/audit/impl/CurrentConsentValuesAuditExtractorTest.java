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

package net.shibboleth.idp.consent.audit.impl;

import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.consent.context.ConsentContext;

/** {@link CurrentConsentValuesAuditExtractor} unit test. */
@SuppressWarnings("javadoc")
public class CurrentConsentValuesAuditExtractorTest extends AbstractConsentAuditExtractorTest {

    private CurrentConsentValuesAuditExtractor extractor;

    @BeforeMethod public void setUpExtractor() {
        extractor = new CurrentConsentValuesAuditExtractor();
    }

    @Test public void testNoCurrentConsents() {
        prc.getSubcontext(ConsentContext.class).getCurrentConsents().clear();
        Assert.assertEquals(extractor.apply(prc), Collections.emptyList());
    }

    @Test public void testExtraction() {
        Assert.assertEquals(extractor.apply(prc), List.of("value1", "value2"));
    }
}
