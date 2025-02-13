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

package net.shibboleth.idp.test.flows.mapper;

import net.shibboleth.idp.test.flows.AbstractFlowTest;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
@ContextConfiguration({"classpath:/mapping/configs/override.xml", })
public class MappingTest extends AbstractFlowTest {
    @Test(enabled=true) public void testResolveAndFilter() {

        final FlowExecutionResult result = flowExecutor.launchExecution("resolveAndFilter", null, externalContext);

        assertFlowExecutionResult(result, "resolveAndFilter");
        assertFlowExecutionOutcome(result.getOutcome());
    }

}
