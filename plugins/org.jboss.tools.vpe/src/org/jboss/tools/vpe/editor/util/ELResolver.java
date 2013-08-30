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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.jboss.tools.common.resref.core.ResourceReference;
import org.jboss.tools.jst.jsp.bundle.BundleMapUtil;
import org.jboss.tools.jst.web.tld.TaglibData;
import org.jboss.tools.vpe.VpePlugin;
import org.jboss.tools.vpe.editor.context.VpePageContext;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Class which for resolve EL Expressions
 * @author mareshkau
 *
 */
public class ELResolver {
	protected VpePageContext pageContext;
	
	public ELResolver(VpePageContext pageContext) {
		this.pageContext = pageContext;
	}

	/**
	 * Replace el.
	 * 
	 * @param pageContext
	 * @param resourceString
	 *            the resource string
	 * 
	 * @return the string
	 * 
	 * @see IELService#replaceEl(IFile, String)
	 */
	public String replaceEl(String resourceString) {
		if (resourceString == null) {
			return ""; //$NON-NLS-1$
		}
		String rst = resourceString;
		final ResourceReference[] references = getResourceReferences();
		if (references == null || references.length == 0) {
			return rst;
		}
		rst = ElServiceUtil.replace(resourceString, references);
		return rst;
	}

	protected String replaceCustomAttributes(String value) {
		String result = value;
		final int nPrefLen = ElServiceUtil.getPrefLen(); // for small-simple optimization
		for (String el : pageContext.getKeysCustomElementsAttributes()) {
			if (el.length() + nPrefLen > result.length()) {
				continue; // no sense for sequence contains checks...
			}
			final String dollarEl = ElServiceUtil.envelopeInDollarEl(el);
			final String sharpEl = ElServiceUtil.envelopeInSharpEl(el);

			if (result.contains(dollarEl)) {
				result = result.replace(dollarEl, pageContext.getAttributefromCustomElementsMapValue(el));
			}
			if (result.contains(sharpEl)) {
				result = result.replace(sharpEl, pageContext.getAttributefromCustomElementsMapValue(el));
			}
		}
		return result;
	}

	/**
	 * Checks if is cloneable node.
	 * 
	 * @param sourceNode
	 *            the source node
	 * @param pageContext
	 *            the page context
	 * 
	 * @return true, if is cloneable node
	 */
	public boolean isELNode(Node sourceNode) {
		if (isInCustomElementsAttributes(sourceNode)) {
			return true;
		} else if (isAvailableForNode(sourceNode)
				|| BundleMapUtil.isInResourcesBundle(pageContext.getBundle(), sourceNode)) {
			return true;
		} else if (Jsf2ResourceUtil.isContainJSFContextPath(sourceNode)) {
			return true;
		} else if (Jsf2ResourceUtil.isContainJSF2ResourceAttributes(sourceNode)) {
			// added by Maksim Areshkau, see JBIDE-4812
			return true;
		} else if (JstlCoreUrlUtil.isContainigJstlCoreUrlInAttributes(pageContext, sourceNode)) {
			return true;
		}
		return false;
	}

	/**
	 * Checks is node exist in source custom element attribute map and if so, then retrun true
	 * 
	 * @param pageContext
	 * @param sourceNode
	 * @return
	 */
	protected boolean isInCustomElementsAttributes(Node sourceNode) {
		boolean res = false;
		String textValue = null;
		if (sourceNode.getNodeType() == Node.TEXT_NODE) {
			textValue = sourceNode.getNodeValue();
			res = isInCustomElementsAttributes(textValue);
		} else if (sourceNode.getNodeType() == Node.ELEMENT_NODE) {
			NamedNodeMap attributesMap = sourceNode.getAttributes();
			for (int i = 0; !res && i < attributesMap.getLength(); i++) {
				Attr attr = (Attr) attributesMap.item(i);
				textValue = attr.getValue();
				res = isInCustomElementsAttributes(textValue);
			}
		}
		return res;
	}

	protected boolean isInCustomElementsAttributes(String textValue) {
		boolean res = false;
		if (textValue != null) {
			for (String key : pageContext.getKeysCustomElementsAttributes()) {
				if (ElServiceUtil.equalsExppression(textValue, key)) {
					res = true;
					break;
				}
			}
		}
		return res;
	}

	/**
	 * Checks if is available for node.
	 * 
	 * @param resourceFile
	 *            the resource file
	 * @param sourceNode
	 *            the source node
	 * 
	 * @return true, if is available for node
	 */
	protected boolean isAvailableForNode(Node sourceNode) {
		boolean rst = findForNode(sourceNode);
		return rst;
	}

	/**
	 * @param sourceNode
	 * @param resourceFile
	 * @return
	 */
	protected boolean findForNode(Node sourceNode) {
		/*
		 * Case 1
		 */
		final ResourceReference[] references = getResourceReferences();
		if (references == null || references.length == 0) {
			return false;
		}
		/*
		 * Case 2
		 */
		String textValue = null;
		if (sourceNode.getNodeType() == Node.TEXT_NODE) {
			textValue = sourceNode.getNodeValue();
			if (textValue != null && ElServiceUtil.isInReferenceResourcesList(references, textValue)) {
				return true;
			}
		}
		/*
		 * Case 3
		 */
		final NamedNodeMap nodeMap = sourceNode.getAttributes();
		if (nodeMap != null) {
			for (int i = 0; i < nodeMap.getLength(); i++) {
				if (ElServiceUtil.isInReferenceResourcesList(references, ((Attr) nodeMap.item(i)).getValue())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public ResourceReference[] getResourceReferences() {
		final ResourceReference[] res = (ResourceReference[])pageContext.getValue(VpePageContext.RES_REFERENCES);
		return res;
	}

	/**
	 * @deprecated use {@link #replaceElAndResources(String, Node)} instead 
	 */
	public String replaceElAndResources(String value) {
		return replaceElAndResources(value, null);
	}
	
	public String replaceElAndResources(String value, Node contextNode) {
		String rst = value;
		
		rst = JstlCoreUrlUtil.processJstlCoreUrlIfNeeded(pageContext, contextNode, value);
		
		rst = ResourceUtil.getBundleValue(pageContext, rst);
		rst = replaceEl(rst);
		
		// replace custom attributes
		rst = replaceCustomAttributes(rst);
		
		if (Jsf2ResourceUtil.isExternalContextPathString(rst)) {
			rst = Jsf2ResourceUtil.processExternalContextPath(rst);
		}
		if (Jsf2ResourceUtil.isRequestContextPathString(rst)) {
			rst = Jsf2ResourceUtil.processRequestContextPath(rst);
		}
		if (Jsf2ResourceUtil.isJSF2ResourceString(rst)) {
			rst = Jsf2ResourceUtil.processCustomJSFAttributes(pageContext, rst);
		}
		
		return rst;
	}
	
	public void createScope() {
	}
	
	public void resolveScope() {
	}
}
