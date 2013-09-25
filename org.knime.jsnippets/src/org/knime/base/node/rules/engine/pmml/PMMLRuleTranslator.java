/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created on 2013.08.13. by Gabor Bakos
 */
package org.knime.base.node.rules.engine.pmml;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.dmg.pmml.CompoundPredicateDocument.CompoundPredicate;
import org.dmg.pmml.CompoundRuleDocument;
import org.dmg.pmml.CompoundRuleDocument.CompoundRule;
import org.dmg.pmml.DataDictionaryDocument.DataDictionary;
import org.dmg.pmml.DataFieldDocument.DataField;
import org.dmg.pmml.MININGFUNCTION;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.dmg.pmml.RuleSelectionMethodDocument.RuleSelectionMethod;
import org.dmg.pmml.RuleSelectionMethodDocument.RuleSelectionMethod.Criterion;
import org.dmg.pmml.RuleSetDocument.RuleSet;
import org.dmg.pmml.RuleSetModelDocument.RuleSetModel;
import org.dmg.pmml.SimplePredicateDocument.SimplePredicate;
import org.dmg.pmml.SimplePredicateDocument.SimplePredicate.Operator.Enum;
import org.dmg.pmml.SimpleRuleDocument;
import org.dmg.pmml.SimpleRuleDocument.SimpleRule;
import org.dmg.pmml.SimpleSetPredicateDocument.SimpleSetPredicate;
import org.dmg.pmml.ValueDocument.Value;
import org.knime.base.node.mine.decisiontree2.PMMLBooleanOperator;
import org.knime.base.node.mine.decisiontree2.PMMLCompoundPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLConditionTranslator;
import org.knime.base.node.mine.decisiontree2.PMMLFalsePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLOperator;
import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLPredicateTranslator;
import org.knime.base.node.mine.decisiontree2.PMMLSimplePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSimpleSetPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLTruePredicate;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLTranslator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * This class converts PMML RuleSets to and from {@link Rule}s. <br>
 * The compound rules are collapsed to a simple form. The id, nbCorrect, recordCount, scoreDistribution parts are not
 * modeled. <br>
 * It prefers and uses the rule versions of {@link PMMLSimplePredicate} and {@link PMMLCompoundPredicate}:
 * {@link PMMLRuleSimplePredicate} and {@link PMMLRuleCompoundPredicate}.
 *
 * @author Gabor Bakos
 * @since 2.9
 */
public class PMMLRuleTranslator extends PMMLConditionTranslator implements PMMLTranslator {
    /**
     * A simple container class for the simple PMML rules created.
     */
    public static class Rule {
        private final PMMLPredicate m_condition;

        private final String m_outcome;

        private final Double m_weight;

        private final Double m_confidence;

        /**
         * Constructs {@link Rule} with its content. The nbCorrect, recordCount and id properties are not available in
         * this simplified model.
         *
         * @param condition The {@link PMMLPredicate} (preferably the rule speicific subtypes,
         *            {@link PMMLRuleSimplePredicate} and {@link PMMLRuleCompoundPredicate}).
         * @param outcome The outcome when the condition matches.
         * @param weight The weight of rule.
         * @param confidence The confidence of the rule.
         */
        protected Rule(final PMMLPredicate condition, final String outcome, final Double weight, final Double confidence) {
            super();
            this.m_condition = condition;
            this.m_outcome = outcome;
            this.m_weight = weight;
            this.m_confidence = confidence;
        }

        /**
         * @return the condition
         */
        public PMMLPredicate getCondition() {
            return m_condition;
        }

        /**
         * @return the outcome
         */
        public String getOutcome() {
            return m_outcome;
        }

        /**
         * @return the weight
         */
        public Double getWeight() {
            return m_weight;
        }

        /**
         * @return the confidence
         */
        public Double getConfidence() {
            return m_confidence;
        }

        /**
         * {@inheritDoc} Auto-generated {@link #toString()} method.
         */
        @Override
        public String toString() {
            return "Rule [m_condition=" + m_condition + ", m_outcome=" + m_outcome + ", m_weight=" + m_weight
                + ", m_confidence=" + m_confidence + "]";
        }
    }

    private List<Rule> m_rules = new ArrayList<Rule>();

    private boolean m_isScorable;

    private List<RuleSelectionMethod> m_selectionMethodList;

    private Map<String, List<String>> m_dataDictionary;

    private String m_defaultScore;

    private double m_defaultConfidence;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        m_nameMapper = new DerivedFieldMapper(pmmlDoc);
        List<RuleSetModel> models = pmmlDoc.getPMML().getRuleSetModelList();
        if (models.size() == 0) {
            throw new IllegalArgumentException("No treemodel provided.");
        }
        RuleSetModel ruleModel = models.get(0);
        initDataDictionary(pmmlDoc);
        m_rules = parseRulesFromModel(ruleModel);
        MININGFUNCTION.Enum functionName = ruleModel.getFunctionName();
        assert functionName == MININGFUNCTION.CLASSIFICATION : functionName;
        m_isScorable = ruleModel.getIsScorable();
        m_selectionMethodList = ruleModel.getRuleSet().getRuleSelectionMethodList();
        m_defaultScore = ruleModel.getRuleSet().getDefaultScore();
        m_defaultConfidence = ruleModel.getRuleSet().getDefaultConfidence();
    }

    /**
     * Inits {@link #m_dataDictionary} based on the {@code pmmlDoc} document.
     *
     * @param pmmlDoc A {@link PMMLDocument}.
     */
    private void initDataDictionary(final PMMLDocument pmmlDoc) {
        DataDictionary dd = pmmlDoc.getPMML().getDataDictionary();
        if (dd == null) {
            m_dataDictionary = Collections.emptyMap();
            return;
        }
        Map<String, List<String>> dataDictionary =
            new LinkedHashMap<String, List<String>>(dd.sizeOfDataFieldArray() * 2);
        for (DataField df : dd.getDataFieldList()) {
            List<String> list = new ArrayList<String>(df.sizeOfValueArray());
            for (Value val : df.getValueList()) {
                list.add(val.getValue());
            }
            dataDictionary.put(df.getName(), Collections.unmodifiableList(list));
        }
        m_dataDictionary = Collections.unmodifiableMap(dataDictionary);
    }

    /**
     * From an xml RuleSetModel it parses the {@link Rule}s.
     *
     * @param ruleModel A {@link RuleSetModel}.
     * @return {@link List} of {@link Rule}s.
     */
    private List<Rule> parseRulesFromModel(final RuleSetModel ruleModel) {
        RuleSet ruleSet = ruleModel.getRuleSet();
        List<Rule> ret = parseRuleSet(ruleSet);
        return ret;
    }

    /**
     * From an xml {@link RuleSet} it parses the {@link Rule}s.
     *
     * @param ruleSet A {@link RuleSet}.
     * @return The parsed {@link Rule}s.
     */
    private List<Rule> parseRuleSet(final RuleSet ruleSet) {
        List<Rule> ret = new ArrayList<Rule>();
        final BitSet isCompound = new BitSet(ruleSet.sizeOfCompoundRuleArray() + ruleSet.sizeOfSimpleRuleArray());
        XmlCursor xmlCursor = ruleSet.newCursor();

        if (xmlCursor.toFirstChild()) {
            int bitIndex = 0;
            do {
                final XmlObject xmlElement = xmlCursor.getObject();
                if (xmlElement instanceof CompoundRuleDocument.CompoundRule) {
                    isCompound.set(bitIndex++);
                } else if (xmlElement instanceof SimpleRuleDocument.SimpleRule) {
                    bitIndex++;
                }

            } while (xmlCursor.toNextSibling());
        }
        int compoundIndex = 0, simpleIndex = 0;
        for (; compoundIndex < ruleSet.sizeOfCompoundRuleArray() || simpleIndex < ruleSet.sizeOfSimpleRuleArray();) {
            if (isCompound.get(compoundIndex + simpleIndex)) {
                ret.add(createRule(ruleSet.getCompoundRuleArray(compoundIndex++)));
            } else {
                ret.add(createRule(ruleSet.getSimpleRuleArray(simpleIndex++)));
            }
        }
        return ret;
    }

    /**
     * The compound rules are tricky... We have to pull each simple rule out of them in order and find the first simple
     * rule to get the outcome. The result is a simple {@link Rule}.
     *
     * @param compoundRule An xml {@link CompoundRule}.
     * @return The corresponding {@link Rule}.
     */
    private Rule createRule(final CompoundRule compoundRule) {
        final LinkedList<PMMLPredicate> predicates = new LinkedList<PMMLPredicate>();
        predicates.addAll(collectPredicates(compoundRule));

        final PMMLCompoundPredicate condition = newCompoundPredicate(PMMLBooleanOperator.AND.toString());
        condition.setPredicates(predicates);
        //This is suspicious, as the later outcomes are discarded, but this is the right thing
        //according to the spec 4.1 (http://www.dmg.org/v4-1/RuleSet.html)
        final SimpleRule firstRule = findFirst(compoundRule);
        if (firstRule == null) {
            throw new IllegalStateException("No SimpleRule was found in " + compoundRule);
        }
        return new Rule(condition, firstRule.getScore(), firstRule.isSetWeight() ? firstRule.getWeight() : null,
            firstRule.isSetConfidence() ? firstRule.getConfidence() : null);
    }

    /**
     * The predicates of a {@link CompoundRule} in the order they appear.
     *
     * @param compoundRule An xml {@link CompoundRule}.
     * @return The flat list of {@link PMMLPredicate}s.
     */
    private List<PMMLPredicate> collectPredicates(final CompoundRule compoundRule) {
        List<PMMLPredicate> ret = new ArrayList<PMMLPredicate>();
        XmlCursor cursor = compoundRule.newCursor();
        if (cursor.toFirstChild()) {
            do {
                XmlObject object = cursor.getObject();
                if (object instanceof CompoundRuleDocument.CompoundRule) {
                    CompoundRuleDocument.CompoundRule cr = (CompoundRuleDocument.CompoundRule)object;
                    ret.addAll(collectPredicates(cr));
                } else if (object instanceof SimpleRule) {
                    SimpleRule sr = (SimpleRule)object;
                    ret.add(createRule(sr).getCondition());
                } else if (object instanceof SimplePredicate) {
                    SimplePredicate sp = (SimplePredicate)object;
                    ret.add(parseSimplePredicate(sp));
                } else if (object instanceof CompoundPredicate) {
                    CompoundPredicate cp = (CompoundPredicate)object;
                    ret.add(parseCompoundPredicate(cp));
                }
            } while (cursor.toNextSibling());
        }
        return ret;
    }

    /**
     * Finds the first xml {@link SimpleRule} within the {@code rule} {@link CompoundRule}.
     *
     * @param rule A {@link CompoundRule}.
     * @return The first {@link SimpleRule} the should provide the outcome.
     */
    private SimpleRule findFirst(final CompoundRule rule) {
        XmlCursor newCursor = rule.newCursor();
        if (newCursor.toFirstChild()) {
            do {
                XmlObject object = newCursor.getObject();
                if (object instanceof SimpleRuleDocument.SimpleRule) {
                    SimpleRuleDocument.SimpleRule sr = (SimpleRuleDocument.SimpleRule)object;
                    return sr;
                }
                if (object instanceof CompoundRule) {
                    CompoundRule cp = (CompoundRule)object;
                    SimpleRule first = findFirst(cp);
                    if (first != null) {
                        return first;
                    }
                }
            } while (newCursor.toNextSibling());
        }
        assert false : rule;
        return null;
    }

    /**
     * Converts an xml {@link SimpleRule} to {@link Rule}.
     *
     * @param r An xml {@link SimpleRule}.
     * @return The corresponding {@link Rule} object.
     */
    private Rule createRule(final SimpleRule r) {
        PMMLPredicate pred;
        if (r.getTrue() != null) {
            pred = new PMMLTruePredicate();
        } else if (r.getFalse() != null) {
            pred = new PMMLFalsePredicate();
        } else if (r.getCompoundPredicate() != null) {
            CompoundPredicate c = r.getCompoundPredicate();
            pred = parseCompoundPredicate(c);
        } else if (r.getSimplePredicate() != null) {
            pred = parseSimplePredicate(r.getSimplePredicate());
        } else if (r.getSimpleSetPredicate() != null) {
            pred = parseSimpleSetPredicate(r.getSimpleSetPredicate());
        } else {
            throw new UnsupportedOperationException(r.toString());
        }
        return new Rule(pred, r.getScore(), r.isSetWeight() ? r.getWeight() : null, r.isSetConfidence()
            ? r.getConfidence() : null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SchemaType exportTo(final PMMLDocument pmmlDoc, final PMMLPortObjectSpec spec) {
        m_nameMapper = new DerivedFieldMapper(pmmlDoc);
        PMML pmml = pmmlDoc.getPMML();
        RuleSetModel ruleSetModel = pmml.addNewRuleSetModel();

        PMMLMiningSchemaTranslator.writeMiningSchema(spec, ruleSetModel);
        ruleSetModel.setModelName("RuleSet");
        ruleSetModel.setFunctionName(MININGFUNCTION.CLASSIFICATION);

        RuleSet ruleSet = ruleSetModel.addNewRuleSet();
        RuleSelectionMethod ruleSelectionMethod = ruleSet.addNewRuleSelectionMethod();
        ruleSelectionMethod.setCriterion(Criterion.FIRST_HIT);
        new DerivedFieldMapper(pmmlDoc);
        addRules(ruleSet, m_rules);
        return RuleSetModel.type;
    }

    /**
     * Adds the {@code rules} as {@link SimpleRule}s to {@code ruleSet}.
     *
     * @param ruleSet An xml {@link RuleSet}.
     * @param rules The simplified {@link Rule}s to add.
     */
    private void addRules(final RuleSet ruleSet, final List<Rule> rules) {
        for (Rule rule : rules) {
            SimpleRule simpleRule = ruleSet.addNewSimpleRule();
            simpleRule.setScore(rule.getOutcome());
            setPredicate(simpleRule, rule.getCondition());
            simpleRule.setWeight(rule.getWeight());
        }
    }

    /**
     * As the predicates can be of different subclasses of {@link PMMLPredicate}, creating them adding their properties
     * to the {@code simpleRule} is done with this method.
     *
     * @param simpleRule An xml {@link SimpleRule} (recently created).
     * @param predicate A {@link PMMLPredicate} with preferably from the Rule versions of
     *            {@link PMMLRuleSimplePredicate} and {@link PMMLRuleCompoundPredicate}.
     */
    void setPredicate(final SimpleRule simpleRule, final PMMLPredicate predicate) {
        if (predicate instanceof PMMLFalsePredicate) {
            simpleRule.addNewFalse();
        } else if (predicate instanceof PMMLTruePredicate) {
            simpleRule.addNewTrue();
        } else if (predicate instanceof PMMLRuleSimplePredicate) {
            PMMLRuleSimplePredicate simple = (PMMLRuleSimplePredicate)predicate;
            SimplePredicate pred = simpleRule.addNewSimplePredicate();
            pred.setField(simple.getSplitAttribute());
            setOperator(pred, simple);
            if (simple.getThreshold() != null) {
                pred.setValue(simple.getThreshold());
            }
        } else if (predicate instanceof PMMLRuleCompoundPredicate) {
            PMMLRuleCompoundPredicate comp = (PMMLRuleCompoundPredicate)predicate;
            CompoundPredicate p = simpleRule.addNewCompoundPredicate();
            setCompound(p, comp);
        } else if (predicate instanceof PMMLSimpleSetPredicate) {
            PMMLSimpleSetPredicate set = (PMMLSimpleSetPredicate)predicate;
            SimpleSetPredicate s = simpleRule.addNewSimpleSetPredicate();
            setSetPredicate(s, set);
        }
    }

    /**
     * Initializes the set predicate.
     *
     * @param ssp The xml {@link SimpleSetPredicate}.
     * @param setPred The {@link PMMLSimpleSetPredicate} containing the parameters.
     */
    private void setSetPredicate(final SimpleSetPredicate ssp, final PMMLSimpleSetPredicate setPred) {
        PMMLPredicateTranslator.initSimpleSetPred(setPred, ssp);
    }

    /**
     * Sets the operator of {@code pred} based on the properties of {@code simple.}
     *
     * @param pred An xml {@link SimplePredicate}.
     * @param simple A {@link PMMLSimplePredicate}.
     */
    private void setOperator(final SimplePredicate pred, final PMMLSimplePredicate simple) {
        PMMLOperator x = simple.getOperator();
        Enum e = PMMLPredicateTranslator.getOperator(x);
        if (e == null) {
            throw new UnsupportedOperationException("Unknown operator: " + x);
        }
        pred.setOperator(e);
    }

    /**
     * For an xml {@link CompoundPredicate} ({@code cp}) sets the parameters based on {@code pred}'s properties.
     *
     * @param cp An xml {@link CompoundPredicate}.
     * @param pred The {@link PMMLPredicate} with the rule version subclasses.
     */
    private void setPredicate(final CompoundPredicate cp, final PMMLPredicate pred) {
        if (pred instanceof PMMLFalsePredicate) {
            cp.addNewFalse();
        } else if (pred instanceof PMMLTruePredicate) {
            cp.addNewTrue();
        } else if (pred instanceof PMMLRuleSimplePredicate) {
            PMMLRuleSimplePredicate simple = (PMMLRuleSimplePredicate)pred;
            SimplePredicate s = cp.addNewSimplePredicate();
            s.setField(simple.getSplitAttribute());
            setOperator(s, simple);
            s.setValue(simple.getThreshold());
        } else if (pred instanceof PMMLRuleCompoundPredicate) {
            PMMLRuleCompoundPredicate compound = (PMMLRuleCompoundPredicate)pred;
            CompoundPredicate c = cp.addNewCompoundPredicate();
            setCompound(c, compound);
        } else if (pred instanceof PMMLSimpleSetPredicate) {
            PMMLSimpleSetPredicate set = (PMMLSimpleSetPredicate)pred;
            SimpleSetPredicate ss = cp.addNewSimpleSetPredicate();
            setSetPredicate(ss, set);
        }
    }

    /**
     * Sets {@code cp}s xml content based on {@code compound}'s properties.
     *
     * @param cp An xml {@link CompoundPredicate}.
     * @param compound A {@link PMMLRuleCompoundPredicate}.
     */
    private void setCompound(final CompoundPredicate cp, final PMMLRuleCompoundPredicate compound) {
        PMMLBooleanOperator op = compound.getConnective();
        org.dmg.pmml.CompoundPredicateDocument.CompoundPredicate.BooleanOperator.Enum boolOp = PMMLPredicateTranslator.getOperator(op);
        if (boolOp == null) {
            throw new UnsupportedOperationException("Not supported: " + op);
        }
        cp.setBooleanOperator(boolOp);
        for (PMMLPredicate pp : compound.getPredicates()) {
            setPredicate(cp, pp);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PMMLCompoundPredicate newCompoundPredicate(final String operator) {
        try {
            PMMLBooleanOperator op;
            op = PMMLBooleanOperator.get(operator);
            return new PMMLRuleCompoundPredicate(op);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PMMLSimplePredicate newSimplePredicate(final String field, final String operator, final String value) {
        return new PMMLRuleSimplePredicate(field, operator, value);
    }

    /**
     * @return The rules created by this instance.
     */
    public List<Rule> getRules() {
        return Collections.unmodifiableList(m_rules);
    }

    /**
     * @return the selectionMethodList (should not be empty after initialization).
     */
    public List<RuleSelectionMethod> getSelectionMethodList() {
        if (m_selectionMethodList == null) {
            return Collections.emptyList();
        }
        assert !m_selectionMethodList.isEmpty();
        return Collections.unmodifiableList(m_selectionMethodList);
    }

    /**
     * @return the isScorable property (whether this model can be executed or not).
     */
    public boolean isScorable() {
        return m_isScorable;
    }

    /**
     * @return the dataDictionary (can be empty even after initialization, not modifiable).
     */
    public Map<String, List<String>> getDataDictionary() {
        if (m_dataDictionary == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(m_dataDictionary);
    }

    /**
     * @return the defaultScore
     */
    public String getDefaultScore() {
        return m_defaultScore;
    }

    /**
     * @return the defaultConfidence
     */
    public double getDefaultConfidence() {
        return m_defaultConfidence;
    }
}
