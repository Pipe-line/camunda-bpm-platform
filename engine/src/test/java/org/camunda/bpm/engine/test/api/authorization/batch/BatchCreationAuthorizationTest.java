/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.test.api.authorization.batch;

import java.util.ArrayList;
import java.util.Collections;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.batch.Batch;
import org.camunda.bpm.engine.batch.history.HistoricBatch;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.RequiredHistoryLevel;
import org.camunda.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.camunda.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.camunda.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.camunda.bpm.engine.test.util.ProcessEngineTestRule;
import org.camunda.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.List;

import static org.camunda.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.camunda.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;

/**
 * @author Askar Akhmerov
 */
@RunWith(Parameterized.class)
public class BatchCreationAuthorizationTest {
  protected static final String TEST_REASON = "test reason";

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected AuthorizationTestRule authRule = new AuthorizationTestRule(engineRule);
  protected ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  protected ProcessInstance processInstance;
  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected HistoryService historyService;

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(authRule).around(testHelper);

  @Parameterized.Parameter
  public AuthorizationScenario scenario;

  @Parameterized.Parameters(name = "Scenario {index}")
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestRule.asParameters(
        scenario()
            .withoutAuthorizations()
            .failsDueToRequired(
                grant(Resources.BATCH, "batchId", "userId", Permissions.CREATE)
            ),
        scenario()
            .withAuthorizations(
                grant(Resources.BATCH, "batchId", "userId", Permissions.CREATE)
            ).succeeds()
    );
  }

  @Before
  public void setUp() {
    authRule.createUserAndGroup("userId", "groupId");
    runtimeService = engineRule.getRuntimeService();
    managementService = engineRule.getManagementService();
    historyService = engineRule.getHistoryService();
  }

  @Before
  public void deployProcessesAndCreateMigrationPlan() {
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());
  }

  @After
  public void tearDown() {
    authRule.deleteUsersAndGroups();
  }

  @After
  public void cleanBatch() {
    Batch batch = engineRule.getManagementService().createBatchQuery().singleResult();
    if (batch != null) {
      engineRule.getManagementService().deleteBatch(
          batch.getId(), true);
    }

    HistoricBatch historicBatch = engineRule.getHistoryService().createHistoricBatchQuery().singleResult();
    if (historicBatch != null) {
      engineRule.getHistoryService().deleteHistoricBatch(
          historicBatch.getId());
    }
  }

  @Test
  public void testBatchProcessInstanceDeletion() {
    //given
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("batchId", "*")
        .start();

    // when
    List<String> processInstanceIds = Collections.singletonList(processInstance.getId());
    runtimeService.deleteProcessInstancesAsync(
        processInstanceIds, null, TEST_REASON);

    // then
    authRule.assertScenario(scenario);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  public void testBatchHistoricProcessInstanceDeletion() {
    List<String> historicProcessInstances = setupHistory();

    //given
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("batchId", "*")
        .start();

    // when
    historyService.deleteHistoricProcessInstancesAsync(historicProcessInstances, TEST_REASON);

    // then
    authRule.assertScenario(scenario);
  }

  protected List<String> setupHistory() {
    runtimeService.deleteProcessInstance(processInstance.getId(), null);
    List<String> historicProcessInstances = new ArrayList<String>();

    for (HistoricProcessInstance hpi : historyService.createHistoricProcessInstanceQuery().list()) {
      historicProcessInstances.add(hpi.getId());
    }
    return historicProcessInstances;
  }


}
