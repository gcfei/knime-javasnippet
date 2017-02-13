/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   17.10.2016 (Jonathan Hale): created
 */
package org.knime.base.node.jsnippet.util.field;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * A field in the java snippet representing flow variable.
 *
 * @author Heiko Hofer
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class JavaFlowVarField extends JavaField {
    private Type m_knimeType;

    /**
     * Get the type of the flow variable referenced by this field.
     *
     * @return the flow variable type
     */
    public Type getFlowVarType() {
        return m_knimeType;
    }

    /**
     * Set the type of the flow variable referenced by this field.
     *
     * @param knimeType the knimeType to set
     */
    public void setFlowVarType(final Type knimeType) {
        m_knimeType = knimeType;
    }

    /**
     * Set the type of the java field
     *
     * @param javaType the javaType to set
     */
    public void setJavaType(final Class<?> javaType) {
        m_javaType = javaType;
    }

    @Override
    public void saveSettings(final Config config) {
        super.saveSettings(config);
        config.addString(KNIME_TYPE, m_knimeType.toString());
    }

    @Override
    public void loadSettings(final Config config) throws InvalidSettingsException {
        super.loadSettings(config);
        String typeName = config.getString(KNIME_TYPE);
        m_knimeType = Type.valueOf(typeName);

        loadJavaType();
    }

    @Override
    public void loadSettingsForDialog(final Config config) {
        super.loadSettingsForDialog(config);
        String typeName = config.getString(KNIME_TYPE, null);
        m_knimeType = null != typeName ? Type.valueOf(typeName) : null;

        loadJavaTypeForDialog();
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.FlowVariable;
    }
}