/*******************************************************************************
 * Copyright (c) 2007-2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.vpe.editor.util;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jboss.tools.jst.web.tld.TaglibData;
import org.jboss.tools.vpe.VpePlugin;
import org.jboss.tools.vpe.editor.context.VpePageContext;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author Yahor Radtsevich (yradtsevich)
 */
public class JstlCoreUrlUtil {
	private static final String JSTL_CORE_TAGLIB = "http://java.sun.com/jsp/jstl/core"; //$NON-NLS-1$
	private static final String JSTL_CORE_URL_PRE_CHECK_STRING = ":url"; //$NON-NLS-1$
	
	public static boolean isContainigJstlCoreUrlInAttributes(VpePageContext pageContext, Node sourceNode) {
		NamedNodeMap attributes = sourceNode.getAttributes();
		if (attributes != null) {
			int attributesLength = attributes.getLength();
			for (int i = 0; i < attributesLength; i++) {
				Node attribute = attributes.item(i);
				String value = attribute.getNodeValue();
				if (value != null && value.contains(JSTL_CORE_URL_PRE_CHECK_STRING)) {// pre-check to not compile regexps every time
					Pattern jstlCoreUrlPattern = createJstlCoreUrlPattern(pageContext, sourceNode);
					if (jstlCoreUrlPattern != null && jstlCoreUrlPattern.matcher(value).find()) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static String processJstlCoreUrlIfNeeded(VpePageContext pageContext, Node contextNode,
			String value) {
		if (contextNode != null && value.contains(JSTL_CORE_URL_PRE_CHECK_STRING)) {
			Pattern jstlCoreUrlPattern = createJstlCoreUrlPattern(pageContext, contextNode);
			if (jstlCoreUrlPattern != null) {
				 return jstlCoreUrlPattern.matcher(value).replaceAll("$2"); //$NON-NLS-1$
			}
		}
		return value;
	}
	
	private static Pattern createJstlCoreUrlPattern(VpePageContext pageContext, Node contextNode) {
		if (contextNode == null) {
			return null;
		}
		Pattern jstlCoreUrlPattern = null;
		List<TaglibData> taglibs = XmlUtil.getTaglibsForNode(contextNode, pageContext);
		TaglibData jstlCoreTaglib = XmlUtil.getTaglibForURI(JSTL_CORE_TAGLIB, taglibs);
		if (jstlCoreTaglib != null) {
			String jstlCorePrefix = jstlCoreTaglib.getPrefix();
			if (jstlCorePrefix != null) {
				try {
					jstlCoreUrlPattern = Pattern.compile(
							"<" + jstlCorePrefix + ":url\\s[^>]*?value\\s*?=\\s*?([\"'])(.*?)(\\1).*?(/>|</" + jstlCorePrefix +":url\\s*>)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				} catch (PatternSyntaxException e) {
					VpePlugin.getPluginLog().logError(e);
				}
			}
		}
		return jstlCoreUrlPattern;
	}

}
