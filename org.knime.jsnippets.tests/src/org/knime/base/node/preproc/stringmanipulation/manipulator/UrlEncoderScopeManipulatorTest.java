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
 *   2 Apr 2020 (carlwitt): created
 */
package org.knime.base.node.preproc.stringmanipulation.manipulator;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */

@RunWith(Parameterized.class)
public final class UrlEncoderScopeManipulatorTest {

    /**
     * @return input / expected output pairs for test methods
     */
    @Parameters(name="{index}: scope={0}; input={1}; expected={2}")
    public static Iterable<String[]> getParameters() {
        return Arrays.asList(new String[][]{
        {
            "query",
            "testReport/api/json?tree=duration,suites[name]",
            "testReport/api/json?tree%3Dduration%2Csuites%5Bname%5D"},
        {
            "query",
            "https://example.com/illegal path with space/to/page?colors=[green, blue]",
            "https://example.com/illegal path with space/to/page?colors%3D%5Bgreen%2C+blue%5D"},
        {
            "query",
            "https://hub.knime.com/search?type=Node&q=what's new?",
            "https://hub.knime.com/search?type%3DNode%26q%3Dwhat%27s+new%3F"},
        {
            "path",
            "https://ab.com/path % to funny/?c=[grn, blu]",
            "https://ab.com/path+%25+to+funny/?c=[grn, blu]"},
       });
    }

    private final String m_scope;
    private final String m_input;
    private final String m_expected;

    /**
     * @param scope the scope for encoding
     * @param input the string to encode
     * @param expected the expected output of the string manipulator
     */
    public UrlEncoderScopeManipulatorTest(final String scope, final String input, final String expected) {
        m_scope = scope;
        m_input = input;
        m_expected = expected;
    }

    /**
     * Compare computed to expected URL encodings.
     * @throws URISyntaxException
     */
    @Test
    public void testUrlEncoderExamples() throws URISyntaxException {
        assertEquals(m_expected, UrlEncoderScopeManipulator.urlEncode(m_scope, m_input));
    }

}