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
 *   04.10.2011 (hofer): created
 */
package org.knime.base.node.preproc.stringmanipulation.manipulator;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Escapes characters that are not allowed in an URL by replacing them with ASCII characters using
 * {@link URLEncoder#encode(String, String)} and the UTF-8 character set.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
public class UrlEncoderScopeManipulator extends UrlEncoderManipulator {

    private static final String PATH_DELIMITER = "/";

    /**
     * Replaces characters not allowed in an URL by ASCII characters. Considers only either the path or query part
     * (encode scope) of the URI.
     *
     * @param scope the part of the URI to fix
     * @param str the URI to fix
     * @return the escaped string
     */
    public static String urlEncode(final String scope, final String str) {

        try {

            if ("path".equals(scope)) {
                // split the path part by delimiter and encode each individual part

                int schemeDelimiterPos = str.indexOf("//");
                int queryDelimiterPos = str.indexOf("?");
                int fragmentDelimiterPos = str.indexOf("#");

                // use first slash as path begin marker
                // skip initial scheme:// if present
                int pathStart;
                if(schemeDelimiterPos != -1) {
                    pathStart = str.indexOf("/", schemeDelimiterPos + 2);
                } else {
                    pathStart = str.indexOf("/");
                }

                // use fragment start marker ("#") or query start marker ("?") if present as path end marker
                // if query exists, it ends the path
                int pathEnd;
                if (queryDelimiterPos != -1) {
                    pathEnd = queryDelimiterPos;
                    // if no query exists the path might be ended by a fragment
                } else if (fragmentDelimiterPos != -1) {
                    pathEnd = fragmentDelimiterPos;
                } else {
                    // otherwise it extends until the end
                    pathEnd = str.length();
                }

                String path = str.substring(pathStart + 1, pathEnd);
                final String encodedPath = Arrays.stream(path.split(PATH_DELIMITER)).map(part -> UrlEncoderManipulator.urlEncode(part))
                    .collect(Collectors.joining(PATH_DELIMITER));

                return str.substring(0, pathStart + 1) + encodedPath + str.substring(pathEnd - 1);


            } else if ("query".equals(scope)) {

                int firstQuestionMark = str.indexOf("?");
                // assume everything after the first question mark is query
                String queryPart = str.substring(firstQuestionMark + 1);

                // encode query part
                final String encodedQuery = UrlEncoderManipulator.urlEncode(queryPart);

                // include the question mark again
                return str.substring(0, firstQuestionMark + 1) + encodedQuery;

            } else {
                throw new IllegalArgumentException(
                    String.format("Scope %s is not supported. Use \"path\" or \"query\".", scope));
            }

        } catch (IllegalArgumentException e) {
            // String Manipulator Framework doesn't allow Manipulators to throw errors. : (
            return null;
        }
    }

    /**
     * Replaces characters not allowed in an URL by ASCII characters. Considers only either the path or query part
     * (encode scope) of the URI.
     *
     * @param scope the part of the URI to fix
     * @param str the URI to fix
     * @return the escaped string
     */
    @Deprecated
    public static String _urlEncode(final String scope, final String str) {

        try {

            // extract query parts of the string
            // needs to be well formed URI, which prior to escaping is typically not the case
            final URI uri = new URI(str);

            String encoded;

            if ("path".equals(scope)) {
                // split the path part by delimiter and encode each individual part
                String path = uri.getPath();
                String[] pathParts = path.split(PATH_DELIMITER);
                final String encodedPath = Arrays.stream(pathParts).map(part -> UrlEncoderManipulator.urlEncode(part))
                    .collect(Collectors.joining(PATH_DELIMITER));

                // rebuild URI
                encoded = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                    encodedPath, uri.getQuery(), uri.getFragment()).toString();


            } else if ("query".equals(scope)) {
                // encode query part
                final String encodedQuery = UrlEncoderManipulator.urlEncode(uri.getQuery());

                // rebuild URI
                encoded = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                    uri.getPath(), encodedQuery, uri.getFragment()).toString();

            } else {
                throw new IllegalArgumentException(
                    String.format("Scope %s is not supported. Use \"path\" or \"query\".", scope));
            }

            // what's passed as query to URI will be encoded again (unfortunately)
            // resulting in escaping the % signs in the percent encoded query
            // revert the double encoding
            return encoded.replace("%25", "%");

        } catch (IllegalArgumentException | URISyntaxException e) {
            // String Manipulator Framework doesn't allow Manipulators to throw errors. : (
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return getName() + "(scope, str)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrArgs() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Replaces forbidden characters in a part of a URL. The part can either be the query part "
                + "(everything after the question mark) or the path part (between host part and query part).<br/><br/>"
                + "<table>\n" +
                "  <tr>\n" +
                "    <th>Supported Scopes</th>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>path</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>query</td>\n" +
                "  </tr>\n" +
                "</table><br/>"+
                "\n" +
                "For instance, in the URL<br/>\n" +
                "https://hub.knime.com/search?type=Node&q=statistics<br/>\n" +
                "The path part is \"/search\" and the query part is \"type=Node&q=statistics\".<br/><br/>" +
                "Since URLs allow only a restricted set of characters, the query \"type=Node&q=what's new?\" needs "
                + "encoding (see urlEncode(str)).<br/>\n" +
                "Since encoding also removes slashes (/) it can not be simply applied to an entire URL. If you "
                + "want to encode a part of an URL without taking it apart manually, pass a scope parameter:<br/>" +
                "<br/><table>\n" +
                "  <tr>\n" +
                "    <th>input:</th>\n" +
                "    <th>urlEncode(\"query\", \"https://hub.knime.com/search?type=Node&amp;q=what's new?\")</th>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>output:</td>\n" +
                "    <td>\"https://hub.knime.com/search?type%3DNode%26q%3Dwhat%27s+new%3F\"</td>\n" +
                "  </tr>\n" +
                "</table><br/>"
                + "<table>\n" +
                "  <tr>\n" +
                "    <th>input:</th>\n" +
                "    <th>urlEncode(\"path\", \"https://ab.com/path&nbsp;%&nbsp;to&nbsp;funny/?c=[grn, blu]\")</th>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>output:</td>\n" +
                "    <td>\"https://ab.com/path+%25+to+funny/?c=[grn, blu]\"</td>\n" +
                "  </tr>\n" +
                "</table>";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getReturnType() {
        return String.class;
    }
}
