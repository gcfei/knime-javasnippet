/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 *
 * History
 *   24.11.2011 (hofer): created
 */
package org.knime.base.node.jsnippet;

import static org.knime.base.node.jsnippet.JavaSnippet.ROWCOUNT;
import static org.knime.base.node.jsnippet.JavaSnippet.ROWINDEX;
import static org.knime.base.node.jsnippet.guarded.JavaSnippetDocument.GUARDED_BODY_END;
import static org.knime.base.node.jsnippet.guarded.JavaSnippetDocument.GUARDED_BODY_START;
import static org.knime.base.node.jsnippet.guarded.JavaSnippetDocument.GUARDED_FIELDS;

import java.io.File;
import java.io.IOException;

import javax.swing.text.BadLocationException;

import org.apache.commons.lang3.StringUtils;
import org.knime.base.node.jsnippet.util.FlowVariableRepository;
import org.knime.base.node.jsnippet.util.JavaSnippetSettings;
import org.knime.base.node.jsnippet.util.ValidationReport;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * The node model of the java snippet node.
 *
 * @author Heiko Hofer
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class JavaSnippetNodeModel extends AbstractConditionalStreamingNodeModel {

    private final JavaSnippetSettings m_settings;

    private final JavaSnippet m_snippet;

    private FlowVariableRepository m_flowVarRepository;

    /**
     * Create a new instance.
     */
    public JavaSnippetNodeModel() {
        m_settings = new JavaSnippetSettings();
        m_snippet = new JavaSnippet();
        m_snippet.attachLogger(getLogger());
    }

    private void pushModifiedFlowVariables() {
        for (final FlowVariable flowVar : m_flowVarRepository.getModified()) {
            final Type type = flowVar.getType();
            if (type.equals(Type.INTEGER)) {
                pushFlowVariableInt(flowVar.getName(), flowVar.getIntValue());
            } else if (type.equals(Type.DOUBLE)) {
                pushFlowVariableDouble(flowVar.getName(), flowVar.getDoubleValue());
            } else { // case: type.equals(Type.STRING)
                pushFlowVariableString(flowVar.getName(), flowVar.getStringValue());
            }
        }
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        m_snippet.setSettings(m_settings);

        m_flowVarRepository = new FlowVariableRepository(getAvailableInputFlowVariables());
        // The following method also compile-checks the code and checks for missing converter factories
        final ValidationReport report = m_snippet.validateSettings(inSpecs[0], m_flowVarRepository);
        if (report.hasWarnings()) {
            setWarningMessage(StringUtils.join(report.getWarnings(), "\n"));
        }
        if (report.hasErrors()) {
            throw new InvalidSettingsException(StringUtils.join(report.getErrors(), "\n"));
        }
        final DataTableSpec outSpec = m_snippet.configure(inSpecs[0], m_flowVarRepository);
        pushModifiedFlowVariables();

        return new DataTableSpec[]{outSpec};
    }

    @Override
    public StreamableOperatorInternals createInitialStreamableOperatorInternals() {
        final NodeLogger log = getLogger();

        if (emitsFlowVariables()) {
            log.info("Flow variables are emitted by the snippet.");
            log.info("The snippet could be stateful and cannot be streamed in distributed manner.");
            log.info("Also, all output tables will have to be cached before downstream nodes can commence execution");
        } else if (isStateful()) {
            log.info("Global variables are defined in the snippet.");
            log.info("The snippet could be stateful and cannot be streamed in distributed manner.");
        } else if (usesRowIndex()) {
            log.info("The ROWINDEX field is used in the snippet. Calculations cannot be done in distributed manner.");
        }

        if (usesRowCount()) {
            log.info("The ROWCOUNT field is used in the snippet. An additional iteration is required for streaming.");
        }

        return super.createInitialStreamableOperatorInternals();
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        m_snippet.setSettings(m_settings);

        final FlowVariableRepository flowVarRepo = new FlowVariableRepository(getAvailableInputFlowVariables());
        final BufferedDataTable output = m_snippet.execute(inData[0], flowVarRepo, exec);
        pushModifiedFlowVariables();

        setWarningMessage(m_snippet.getWarningMessage());

        return new BufferedDataTable[]{output};
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec, final long rowCount)
        throws InvalidSettingsException {
        m_snippet.setSettings(m_settings);
        final FlowVariableRepository flowVarRepo = new FlowVariableRepository(getAvailableInputFlowVariables());
        return m_snippet.createRearranger(spec, flowVarRepo, (int)rowCount, null);
    }

    private boolean checkSnippetForText(final String fromGuard, final String toGuard, final String text) {
        try {
            final String snippetCode = m_snippet.getDocument().getTextBetween(fromGuard, toGuard);

            return snippetCode.contains(text);
        } catch (BadLocationException e) {
            //should not happen -> implementation error
            throw new RuntimeException("Most likely an implementation error.", e);
        }
    }

    @Override
    protected boolean usesRowIndex() {
        return checkSnippetForText(GUARDED_BODY_START, GUARDED_BODY_END, ROWINDEX);
    }

    @Override
    protected boolean usesRowCount() {
        return checkSnippetForText(GUARDED_BODY_START, GUARDED_BODY_END, ROWCOUNT);
    }

    @Override
    protected boolean isStateful() {
        return emitsFlowVariables() || checkSnippetForText(GUARDED_FIELDS, GUARDED_BODY_START, ";");
    }

    @Override
    protected boolean emitsFlowVariables() {
        return m_flowVarRepository.getModified().size() > 0;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final JavaSnippetSettings s = new JavaSnippetSettings();
        s.loadSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    @Override
    protected void reset() {
        // no internals, nothing to reset.
    }

    @Override
    protected void onDispose() {
        super.onDispose();
        m_snippet.invalidate();
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals.
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals.
    }
}
