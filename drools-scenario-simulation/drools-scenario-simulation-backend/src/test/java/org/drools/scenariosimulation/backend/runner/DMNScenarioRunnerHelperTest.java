/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.scenariosimulation.backend.runner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.drools.scenariosimulation.api.model.ExpressionIdentifier;
import org.drools.scenariosimulation.api.model.FactIdentifier;
import org.drools.scenariosimulation.api.model.FactMapping;
import org.drools.scenariosimulation.api.model.FactMappingType;
import org.drools.scenariosimulation.api.model.FactMappingValue;
import org.drools.scenariosimulation.api.model.FactMappingValueStatus;
import org.drools.scenariosimulation.api.model.Scenario;
import org.drools.scenariosimulation.api.model.ScenarioWithIndex;
import org.drools.scenariosimulation.api.model.Simulation;
import org.drools.scenariosimulation.backend.expression.DMNFeelExpressionEvaluator;
import org.drools.scenariosimulation.backend.expression.ExpressionEvaluator;
import org.drools.scenariosimulation.backend.fluent.DMNScenarioExecutableBuilder;
import org.drools.scenariosimulation.backend.model.Dispute;
import org.drools.scenariosimulation.backend.model.Person;
import org.drools.scenariosimulation.backend.runner.model.ScenarioExpect;
import org.drools.scenariosimulation.backend.runner.model.ScenarioResultMetadata;
import org.drools.scenariosimulation.backend.runner.model.ScenarioRunnerData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.RequestContext;
import org.kie.dmn.api.core.DMNDecisionResult;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.ast.DecisionNode;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.kie.dmn.api.core.DMNDecisionResult.DecisionEvaluationStatus;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DMNScenarioRunnerHelperTest {

    @Mock
    protected RequestContext requestContextMock;

    @Mock
    protected DMNResult dmnResultMock;

    @Mock
    protected DMNDecisionResult dmnDecisionResultMock;

    @Mock
    protected DMNModel dmnModelMock;

    private static final String NAME = "NAME";
    private static final String FEEL_EXPRESSION_NAME = "\"" + NAME + "\"";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(10);
    private static final String TEST_DESCRIPTION = "Test description";
    private static final ClassLoader classLoader = RuleScenarioRunnerHelperTest.class.getClassLoader();
    private static final ExpressionEvaluator expressionEvaluator = new DMNFeelExpressionEvaluator(classLoader);
    private static final DMNScenarioRunnerHelper runnerHelper = new DMNScenarioRunnerHelper();

    private Simulation simulation;
    private FactIdentifier personFactIdentifier;
    private ExpressionIdentifier firstNameGivenExpressionIdentifier;
    private FactMapping firstNameGivenFactMapping;
    private Scenario scenario1;
    private Scenario scenario2;
    private ExpressionIdentifier firstNameExpectedExpressionIdentifier;
    private FactMapping firstNameExpectedFactMapping;
    private FactIdentifier disputeFactIdentifier;
    private ExpressionIdentifier amountGivenExpressionIdentifier;
    private FactMapping amountNameGivenFactMapping;
    private ExpressionIdentifier amountExpectedExpressionIdentifier;
    private FactMapping amountNameExpectedFactMapping;
    private FactMappingValue amountNameExpectedFactMappingValue;
    private FactMappingValue firstNameExpectedValue;

    @Before
    public void init() {
        simulation = new Simulation();
        personFactIdentifier = FactIdentifier.create("Fact 1", Person.class.getCanonicalName());
        firstNameGivenExpressionIdentifier = ExpressionIdentifier.create("First Name Given", FactMappingType.GIVEN);
        firstNameGivenFactMapping = simulation.getSimulationDescriptor().addFactMapping(personFactIdentifier, firstNameGivenExpressionIdentifier);
        firstNameGivenFactMapping.addExpressionElement("Fact 1", String.class.getCanonicalName());
        firstNameGivenFactMapping.addExpressionElement("firstName", String.class.getCanonicalName());

        disputeFactIdentifier = FactIdentifier.create("Fact 2", Dispute.class.getCanonicalName());
        amountGivenExpressionIdentifier = ExpressionIdentifier.create("Amount Given", FactMappingType.GIVEN);
        amountNameGivenFactMapping = simulation.getSimulationDescriptor().addFactMapping(disputeFactIdentifier, amountGivenExpressionIdentifier);
        amountNameGivenFactMapping.addExpressionElement("Fact 2", BigDecimal.class.getCanonicalName());
        amountNameGivenFactMapping.addExpressionElement("amount", BigDecimal.class.getCanonicalName());

        firstNameExpectedExpressionIdentifier = ExpressionIdentifier.create("First Name Expected", FactMappingType.EXPECT);
        firstNameExpectedFactMapping = simulation.getSimulationDescriptor().addFactMapping(personFactIdentifier, firstNameExpectedExpressionIdentifier);
        firstNameExpectedFactMapping.addExpressionElement("Fact 1", String.class.getCanonicalName());
        firstNameExpectedFactMapping.addExpressionElement("firstName", String.class.getCanonicalName());

        amountExpectedExpressionIdentifier = ExpressionIdentifier.create("Amount Expected", FactMappingType.EXPECT);
        amountNameExpectedFactMapping = simulation.getSimulationDescriptor().addFactMapping(disputeFactIdentifier, amountExpectedExpressionIdentifier);
        amountNameExpectedFactMapping.addExpressionElement("Fact 2", Double.class.getCanonicalName());
        amountNameExpectedFactMapping.addExpressionElement("amount", Double.class.getCanonicalName());

        scenario1 = simulation.addScenario();
        scenario1.setDescription(TEST_DESCRIPTION);
        scenario1.addMappingValue(personFactIdentifier, firstNameGivenExpressionIdentifier, FEEL_EXPRESSION_NAME);
        firstNameExpectedValue = scenario1.addMappingValue(personFactIdentifier, firstNameExpectedExpressionIdentifier, FEEL_EXPRESSION_NAME);

        scenario2 = simulation.addScenario();
        scenario2.setDescription(TEST_DESCRIPTION);
        scenario2.addMappingValue(personFactIdentifier, firstNameGivenExpressionIdentifier, FEEL_EXPRESSION_NAME);
        scenario2.addMappingValue(personFactIdentifier, firstNameExpectedExpressionIdentifier, FEEL_EXPRESSION_NAME);
        scenario2.addMappingValue(disputeFactIdentifier, amountGivenExpressionIdentifier, AMOUNT);
        amountNameExpectedFactMappingValue = scenario2.addMappingValue(disputeFactIdentifier, amountExpectedExpressionIdentifier, AMOUNT);

        when(requestContextMock.getOutput(DMNScenarioExecutableBuilder.DMN_RESULT)).thenReturn(dmnResultMock);
        when(requestContextMock.getOutput(DMNScenarioExecutableBuilder.DMN_MODEL)).thenReturn(dmnModelMock);
    }

    @Test
    public void verifyConditions() {
        ScenarioRunnerData scenarioRunnerData = new ScenarioRunnerData();
        scenarioRunnerData.addExpect(new ScenarioExpect(personFactIdentifier, Collections.singletonList(firstNameExpectedValue)));

        Assertions.assertThatThrownBy(() -> runnerHelper.verifyConditions(simulation.getSimulationDescriptor(), scenarioRunnerData, expressionEvaluator, requestContextMock))
                .isInstanceOf(ScenarioException.class)
                .hasMessage("DMN execution has not generated a decision result with name Fact 1");

        when(dmnResultMock.getDecisionResultByName(anyString())).thenReturn(dmnDecisionResultMock);
        when(dmnDecisionResultMock.getEvaluationStatus()).thenReturn(DecisionEvaluationStatus.SUCCEEDED);

        Assertions.assertThatThrownBy(() -> runnerHelper.verifyConditions(simulation.getSimulationDescriptor(), scenarioRunnerData, expressionEvaluator, requestContextMock))
                .isInstanceOf(ScenarioException.class)
                .hasMessage("Wrong resultRaw structure because it is not a complex type as expected");

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("firstName", "WrongValue");

        when(dmnDecisionResultMock.getResult()).thenReturn(resultMap);

        runnerHelper.verifyConditions(simulation.getSimulationDescriptor(), scenarioRunnerData, expressionEvaluator, requestContextMock);

        assertEquals(1, scenarioRunnerData.getResults().size());
        assertFalse(scenarioRunnerData.getResults().get(0).getResult());

        ScenarioRunnerData newScenarioRunnerData = new ScenarioRunnerData();
        newScenarioRunnerData.addExpect(new ScenarioExpect(personFactIdentifier, Collections.singletonList(firstNameExpectedValue)));
        resultMap.put("firstName", NAME);

        runnerHelper.verifyConditions(simulation.getSimulationDescriptor(), newScenarioRunnerData, expressionEvaluator, requestContextMock);

        assertEquals(1, newScenarioRunnerData.getResults().size());
        assertTrue(newScenarioRunnerData.getResults().get(0).getResult());

        // verify that when expression evaluation fails the corresponding expression is marked as error
        runnerHelper.verifyConditions(simulation.getSimulationDescriptor(),
                                      newScenarioRunnerData,
                                      mock(ExpressionEvaluator.class),
                                      requestContextMock);
        assertEquals(newScenarioRunnerData.getResults().get(0).getFactMappingValue().getStatus(), FactMappingValueStatus.FAILED_WITH_ERROR);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createObject() {
        Map<List<String>, Object> params = new HashMap<>();
        params.put(asList("creator", "name"), "TestName");
        params.put(asList("creator", "surname"), "TestSurname");
        params.put(singletonList("age"), BigDecimal.valueOf(10));

        Object objectRaw = runnerHelper.createObject(String.class.getCanonicalName(), params, this.getClass().getClassLoader());
        assertTrue(objectRaw instanceof Map);

        Map<String, Object> object = (Map<String, Object>) objectRaw;
        assertEquals(BigDecimal.valueOf(10), object.get("age"));
        assertTrue(object.get("creator") instanceof Map);

        Map<String, Object> creator = (Map<String, Object>) object.get("creator");
        assertEquals("TestName", creator.get("name"));
        assertEquals("TestSurname", creator.get("surname"));
    }

    @Test
    public void extractResultMetadata() {
        Set<DecisionNode> decisions = new HashSet<>();
        IntStream.range(0, 5).forEach(index -> decisions.add(createDecisionMock("decision" + index)));
        when(dmnModelMock.getDecisions()).thenReturn(decisions);

        List<DMNDecisionResult> decisionResults = new ArrayList<>();
        decisionResults.add(createDecisionResultMock("decision2", true));
        decisionResults.add(createDecisionResultMock("decision3", false));
        when(dmnResultMock.getDecisionResults()).thenReturn(decisionResults);

        ScenarioWithIndex scenarioWithIndex = new ScenarioWithIndex(1, scenario1);
        ScenarioResultMetadata scenarioResultMetadata = runnerHelper.extractResultMetadata(requestContextMock, scenarioWithIndex);

        assertEquals(scenarioWithIndex, scenarioResultMetadata.getScenarioWithIndex());
        assertEquals(5, scenarioResultMetadata.getAvailable().size());
        assertTrue(scenarioResultMetadata.getAvailable().contains("decision1"));
        assertEquals(1, scenarioResultMetadata.getExecuted().size());
        assertTrue(scenarioResultMetadata.getExecuted().contains("decision2"));
        assertFalse(scenarioResultMetadata.getExecuted().contains("decision3"));
    }

    private DecisionNode createDecisionMock(String decisionName) {
        DecisionNode decisionMock = mock(DecisionNode.class);
        when(decisionMock.getName()).thenReturn(decisionName);
        return decisionMock;
    }

    private DMNDecisionResult createDecisionResultMock(String decisionName, boolean success) {
        DMNDecisionResult decisionResultMock = mock(DMNDecisionResult.class);
        when(decisionResultMock.getDecisionName()).thenReturn(decisionName);
        when(decisionResultMock.getEvaluationStatus())
                .thenReturn(success ? DecisionEvaluationStatus.SUCCEEDED :
                                    DecisionEvaluationStatus.FAILED);
        return decisionResultMock;
    }
}