/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 13, 2016 (hornm): created
 */
package org.knime.base.node.jsnippet;

import static org.knime.core.node.streamable.InputPortRole.DISTRIBUTED_STREAMABLE;
import static org.knime.core.node.streamable.InputPortRole.NONDISTRIBUTED_STREAMABLE;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.MergeOperator;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;
import org.knime.core.node.util.CheckUtils;

/**
 * Abstract node model that provides default implementations of the streaming API-methods depending on certain
 * conditions, like whether the row count is required (an additional iteration of the data table is necessary) or the
 * row index is used (i.e. only streaming possible but no distribution).
 *
 * @author Martin Horn
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public abstract class AbstractConditionalStreamingNodeModel extends NodeModel {

    /* config key to store the row count in a streamable operator internals */
    private static final String CFG_ROW_COUNT = "row_count";

    /**
     * Creates a node model with one input and one output port.
     */
    public AbstractConditionalStreamingNodeModel() {
        super(1, 1);
    }

    @Override
    public StreamableOperatorInternals createInitialStreamableOperatorInternals() {
        return new SimpleStreamableOperatorInternals();
    }

    @Override
    public boolean iterate(final StreamableOperatorInternals internals) {
        return usesRowCount() && !((SimpleStreamableOperatorInternals)internals).getConfig().containsKey(CFG_ROW_COUNT);
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        return new StreamableOperator() {

            private SimpleStreamableOperatorInternals m_internals;

            @Override
            public void loadInternals(final StreamableOperatorInternals internals) {
                m_internals = (SimpleStreamableOperatorInternals)internals;
            }

            @Override
            public void runIntermediate(final PortInput[] inputs, final ExecutionContext exec) throws Exception {
                //count number of rows
                long count = 0;
                final RowInput rowInput = (RowInput)inputs[0];
                while (rowInput.poll() != null) {
                    count++;
                }
                m_internals.getConfig().addLong(CFG_ROW_COUNT, count);
            }

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final long rowCount = m_internals.getConfig().getLong(CFG_ROW_COUNT, -1);
                final ColumnRearranger columnRearranger = createColumnRearranger((DataTableSpec)inSpecs[0], rowCount);
                columnRearranger.createStreamableFunction().runFinal(inputs, outputs, exec);
            }

            @Override
            public StreamableOperatorInternals saveInternals() {
                return m_internals;
            }
        };
    }

    @Override
    public MergeOperator createMergeOperator() {
        return new MergeOperator() {

            @Override
            public StreamableOperatorInternals mergeIntermediate(final StreamableOperatorInternals[] operators) {
                //sum up the row counts if necessary
                long count = 0;
                for (StreamableOperatorInternals op : operators) {
                    final SimpleStreamableOperatorInternals simpleInternals = (SimpleStreamableOperatorInternals)op;
                    CheckUtils.checkState(simpleInternals.getConfig().containsKey(CFG_ROW_COUNT),
                        "Config for key " + CFG_ROW_COUNT + " isn't set.");
                    try {
                        count += simpleInternals.getConfig().getLong(CFG_ROW_COUNT);
                    } catch (InvalidSettingsException e) {
                        // should not happen since we checked already
                        throw new RuntimeException(e);
                    }
                }

                final SimpleStreamableOperatorInternals res = new SimpleStreamableOperatorInternals();
                if (count > 0) {
                    res.getConfig().addLong(CFG_ROW_COUNT, count);
                }
                return res;
            }

            @Override
            public StreamableOperatorInternals mergeFinal(final StreamableOperatorInternals[] operators) {
                //nothing to do here
                return null;
            }
        };
    }

    @Override
    public void finishStreamableExecution(final StreamableOperatorInternals internals, final ExecutionContext exec,
        final PortOutput[] output) throws Exception {
        // nothing to do here
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{usesRowIndex() || isStateful() ? NONDISTRIBUTED_STREAMABLE : DISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /**
     * Checks whether the row index is used for the calculations.
     *
     * @return if <code>true</code> the row index will be used for calculation, i.e. node cannot be executed in
     *         distributed fashion
     */
    protected abstract boolean usesRowIndex();

    /**
     * Checks whether the row count is used for the calculations.
     *
     * @return if <code>true</code> the row count will be used for calculation, i.e. an additional iteration over the
     *         input table is necessary
     */
    protected abstract boolean usesRowCount();

    /**
     * Checks whether the node preserves state in between calculations. If it does, the node cannot be executed in
     * distributed fashion.
     *
     * @return true iff the node is stateful
     */
    protected boolean isStateful() {
        return false;
    }

    /**
     * Checks whether the node emits flow variables. If it does, computation has to finish and flow variables have to be
     * pushed before downstream nodes can commence with their computation.
     *
     * @return true iff the node emits flow variables
     */
    protected boolean emitsFlowVariables() {
        return false;
    }

    /**
     * Creates a column rearranger that describes the changes to the input table. Sub classes will check the consistency
     * of the input table with their settings (fail with {@link InvalidSettingsException} if necessary) and then return
     * a customized {@link ColumnRearranger}.
     *
     * @param spec The spec of the input table.
     * @param rowCount the row count if {@link #usesRowCount()} returns <code>true</code>, otherwise <code>-1</code>
     * @return A column rearranger describing the changes, never null.
     * @throws InvalidSettingsException If the settings or the input are invalid.
     */
    protected abstract ColumnRearranger createColumnRearranger(final DataTableSpec spec, long rowCount)
        throws InvalidSettingsException;

}
