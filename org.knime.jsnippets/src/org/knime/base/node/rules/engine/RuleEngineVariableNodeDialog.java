/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 * History
 *   11.04.2008 (thor): created
 */
package org.knime.base.node.rules.engine;

import org.knime.base.node.rules.engine.rsyntax.VariableRuleParser;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Rule Engine node dialog, but also usable for rule engine filter/splitter.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Gabor Bakos
 * @since 2.8
 */
public class RuleEngineVariableNodeDialog extends NodeDialogPane {
    static {
        new VariableRuleParser.RuleLanguageSupport();
//        LanguageSupportFactory.get().addLanguageSupport(RuleParser.SYNTAX_STYLE_RULE,
//                                                        RuleParser.RuleLanguageSupport.class.getName());
    }

    /** Default default label. */
    static final String DEFAULT_LABEL = "default";

    /** Default name for the newly appended column. */
    static final String NEW_COL_NAME = "prediction";

    /** The default text for the rule editor. */
    /** The default text for the rule editor. */
    static final String RULE_LABEL = "// enter ordered set of rules, e.g.:\n"+
    "// $${Ddouble variable name}$$ > 5.0 => \"large\"\n" +
    "// $${Sstring column name}$$ LIKE \"*blue*\" => \"small and blue\"\n" +
    "// TRUE => \"default outcome\"\n";

    private final boolean m_warnOnColRefsInStrings;

    private RulePanel m_rulePanel;

    /**
     * Constructs the default {@link RuleEngineVariableNodeDialog}.
     */
    public RuleEngineVariableNodeDialog() {
        this(true);
    }

    /**
     * Constructs a {@link RuleEngineVariableNodeDialog}.
     *
     * @param warnOnColRefsInStrings Whether to warn if there are column references in the outcome strings.
     */
    RuleEngineVariableNodeDialog(final boolean warnOnColRefsInStrings) {
        this.m_warnOnColRefsInStrings = warnOnColRefsInStrings;
        initializeComponent();
    }

    private void initializeComponent() {
        addTab("Rule Editor", m_rulePanel = createRulePanel());
    }

    /**
     * @return The {@link RulePanel}.
     */
    private RulePanel createRulePanel() {
//        return new RulePanel(true/*hasOutput*/, true/*hasDefaultOutcome*/, m_warnOnColRefsInStrings,
//                false/*showColumns*/, null, RuleManipulatorProvider.getVariableProvider(),
//                new KnimeCompletionProvider() {
//                    @Override
//                    public String escapeColumnName(final String colName) {
//                        throw new UnsupportedOperationException("No columns!");
//                    }
//
//                    @Override
//                    public String escapeFlowVariableName(final String varName) {
//                        return "$${" + varName + "}$$";
//                    }
//                });
        return new RulePanel(RuleNodeSettings.VariableRule, m_warnOnColRefsInStrings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        m_rulePanel.loadSettingsFrom(settings, new DataTableSpec[] {new DataTableSpec()}, getAvailableFlowVariables());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_rulePanel.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean closeOnESC() {
        // Important to not close on esc, because after that the JSyntaxTextArea do not work properly.
        return false;
    }
}
