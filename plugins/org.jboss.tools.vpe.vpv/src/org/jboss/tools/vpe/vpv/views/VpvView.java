package org.jboss.tools.vpe.vpv.views;

/**
 * @author Yahor Radtsevich (yradtsevich)
 */

import static org.jboss.tools.vpe.vpv.server.HttpConstants.ABOUT_BLANK;
import static org.jboss.tools.vpe.vpv.server.HttpConstants.HTTP;
import static org.jboss.tools.vpe.vpv.server.HttpConstants.LOCALHOST;
import static org.jboss.tools.vpe.vpv.server.HttpConstants.PROJECT_NAME;
import static org.jboss.tools.vpe.vpv.server.HttpConstants.VIEW_ID;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.EditorReference;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.jboss.tools.vpe.vpv.Activator;
import org.jboss.tools.vpe.vpv.transform.DomUtil;
import org.jboss.tools.vpe.vpv.transform.VpvDomBuilder;
import org.jboss.tools.vpe.vpv.transform.VpvVisualModel;
import org.jboss.tools.vpe.vpv.transform.VpvVisualModelHolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class VpvView extends ViewPart implements VpvVisualModelHolder {

	public static final String ID = "org.jboss.tools.vpe.vpv.views.VpvView";

	private Browser browser;
	
	private VpvVisualModel visualModel;
	private int modelHolderId;

	private EditorListener editorListener;
	private SelectionListener selectionListener;
	
	private IEditorPart currentEditor;
	
	
	public VpvView() {
		setModelHolderId(Activator.getDefault().getVisualModelHolderRegistry().registerHolder(this));
	}
	
	@Override
	public void dispose() {
		Activator.getDefault().getVisualModelHolderRegistry().unregisterHolder(this);
		getSite().getPage().removePartListener(editorListener);
		getSite().getPage().removeSelectionListener(selectionListener);
		super.dispose();
	}
	
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		browser = new Browser(parent, SWT.NONE);
		browser.setUrl(ABOUT_BLANK);
		inizializeSelectionListener();	
		inizializeEditorListener(browser, modelHolderId);
	}

	private void inizializeEditorListener(Browser browser, int modelHolderId ) {
		EditorListener editorListener = new EditorListener();
		getSite().getPage().addPartListener(editorListener);
		editorListener.showBootstrapPart();
	}

	private void inizializeSelectionListener() {
		selectionListener = new SelectionListener();
		getSite().getPage().addSelectionListener(selectionListener);	
	}

	public void setFocus() {
		browser.setFocus();
	}

	@Override
	public void setVisualModel(VpvVisualModel visualModel) {
		this.visualModel = visualModel;
	}

	public void setModelHolderId(int modelHolderId) {
		this.modelHolderId = modelHolderId;
	}
	
	
	public IEditorPart getCurrentEditor() {
		return currentEditor;
	}
	
	public void editorChanged(IEditorPart editor) {
		if (currentEditor == editor) {
			return;
		} else if (editor == null) {
			browser.setUrl(ABOUT_BLANK);
		} else if (isImportant(editor)) {
			formRequestToServer(editor);
		} else {
			browser.setUrl(ABOUT_BLANK);
		}

		setCurrentEditor(editor);
	}
	
	private boolean isImportant(IEditorPart editor) {
		if (editor.getAdapter(StructuredTextEditor.class) != null){
			return true; // TODO check DOM model support
		}
		return false;
	}

	public void setCurrentEditor(IEditorPart currentEditor) {
		this.currentEditor = currentEditor;
	}
	
	private boolean isCurrentEditor(IEditorPart editorPart) {
		if (currentEditor == editorPart) {
			return true;
		}
		return false;
	}
	
	private String formUrl(IFile ifile) {
		String projectName = ifile.getProject().getName();
		String projectRelativePath = ifile.getProjectRelativePath().toString();
		int port = Activator.getDefault().getServer().getPort();
		String url = HTTP + LOCALHOST + ":" + port + "/" + projectRelativePath + "?" + PROJECT_NAME + "="
				+ projectName + "&" + VIEW_ID + "=" + modelHolderId;

		return url;
	}

	private IFile getFileOpenedInEditor(IEditorPart editorPart) {
		IFile file = null;
		if (editorPart != null && editorPart.getEditorInput() instanceof IFileEditorInput) {
			IFileEditorInput fileEditorInput = (IFileEditorInput) editorPart.getEditorInput();
			file = fileEditorInput.getFile();
		}
		return file;
	}
	
	private void formRequestToServer(IEditorPart editor) {
		IFile ifile = getFileOpenedInEditor(editor);
		if (ifile != null && SuitableFileExtensions.contains(ifile.getFileExtension().toString())) {
			String url = formUrl(ifile);
			browser.setUrl(url, null, new String[] { "Cache-Control: no-cache, no-store" });
		} else {
			browser.setUrl(ABOUT_BLANK);
		}
	}

	private class EditorListener implements IPartListener2 {

		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			Activator.logInfo(partRef + " is Activated");
			if (partRef instanceof EditorReference) {
				Activator.logInfo("instance of Editor reference");
				IEditorPart editor = ((EditorReference) partRef).getEditor(false);
				editorChanged(editor);
			}
		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
			Activator.logInfo(partRef + " is Opened");
		}

		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
			Activator.logInfo(partRef + " is Closed");
			if (partRef instanceof EditorReference) {
				IEditorPart editorPart = ((EditorReference) partRef).getEditor(false);
				if (isCurrentEditor(editorPart)) {
					editorChanged(null);
				}
			}
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partHidden(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {
		}

		public void showBootstrapPart() {
			IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.getActiveEditor();
			formRequestToServer(activeEditor);
		}

	}
	
	private class SelectionListener implements ISelectionListener {

		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if(selection instanceof IStructuredSelection && isInCurrentEditor((IStructuredSelection) selection)){
				Node sourceNode = getNodeFromSelection((IStructuredSelection) selection);
				Long idForSelection = getIdForSelection(sourceNode, visualModel);
				setBrowserSelection(idForSelection);
			}
		}

		
	}

	private boolean isInCurrentEditor(IStructuredSelection selection) {
		Node selectedNode = getNodeFromSelection(selection);
		Document selectionDocument = null;
		if (selectedNode != null) {
			selectionDocument = selectedNode.getOwnerDocument();
		}
		
		Document editorDocument = null;
		IDOMModel editorModel = null;
		if (currentEditor != null) {
			editorModel = (IDOMModel) currentEditor.getAdapter(IDOMModel.class);
		}
		IDOMDocument editorIdomDocument = null;
		if (editorModel != null) {
			editorIdomDocument = editorModel.getDocument();
		}
		
		Element editorDocumentElement = null;
		if (editorIdomDocument != null) {
			editorDocumentElement = editorIdomDocument.getDocumentElement();
		}
		
		if (editorDocumentElement != null) {
			editorDocument = editorDocumentElement.getOwnerDocument();
		}
		
		
		if (selectionDocument != null && selectionDocument == editorDocument) {
			return true;
		} else {
			return false;
		}
	}
	
	private Node getNodeFromSelection(IStructuredSelection selection) {
		Object firstElement = selection.getFirstElement();
		if (firstElement instanceof Node) {
			return (Node) firstElement;
		} else {
			return null;
		}
	}
	
	public Long getIdForSelection(Node selectedSourceNode, VpvVisualModel visualModel) {
		Map<Node, Node> sourceVisuaMapping = visualModel.getSourceVisualMapping();
		
		Node visualNode = null;
		Node sourceNode = selectedSourceNode;
		do {
			visualNode = sourceVisuaMapping.get(sourceNode);
			sourceNode = DomUtil.getParentNode(sourceNode);
		} while (visualNode == null && sourceNode != null);
		
		if (!(visualNode instanceof Element)) { // text node, comment, etc
			visualNode = DomUtil.getParentNode(visualNode); // should be element now or null
		}
		
		String idString = null;
		if (visualNode instanceof Element) {
			Element elementNode = (Element) visualNode;
			idString = elementNode.getAttribute(VpvDomBuilder.ATTR_VPV_ID);
		}
		
		Long id = null;
		if (idString != null && !idString.isEmpty()) {
			try {
				id = Long.parseLong(idString);
			} catch (NumberFormatException e) {
				Activator.logError(e);
			}
		}
		
		return id;
	}
	
	private void setBrowserSelection(Long idForSelection) {
		browser.execute(
		"(function(css) {" +
			"var style=document.getElementById('VPV-STYLESHEET');" +
//			"if ('\\v' == 'v') /* ie only */ {alert('ie');" +
//				"if (style == null) {" +
//					"style = document.createStyleSheet();" +
//				"}" +
//				"style.cssText = css;" +
//			"}" +
//			"else {" +
				"if (style == null) {" +
					"style = document.createElement('STYLE');" +
					"style.type = 'text/css';" +
				"}" +
				"style.innerHTML = css;" +
				"document.body.appendChild(style);" +
//			"}" +
			"style.id = 'VPV-STYLESHEET';" + 
			"})('[" + VpvDomBuilder.ATTR_VPV_ID + "=\"" + idForSelection + "\"] {outline: 2px solid blue;}')");
		
		System.out.println("(function(css) {" +
				"var style=document.getElementById('VPV-STYLESHEET');" +
				"if ('\\v' == 'v') /* old ie only */ {" +
					"if (style == null) {" +
						"style = document.createStyleSheet();" +
					"}" +
					"style.cssText = css;" + // TODO won't work in compatibility mode HTML1113 (without proper doctype)
				"}" +
				"else {" +
					"if (style == null) {" +
						"style = document.createElement('STYLE');" +
						"style.type = 'text/css';" +
					"}" +
					"style.innerHTML = css;" +
					"document.body.appendChild(style);" +
//				"}" +
				"style.id = 'VPV-STYLESHEET';" + 
				"})('[" + VpvDomBuilder.ATTR_VPV_ID + "=\"" + idForSelection + "\"] {outline: 2px solid blue;}')");
		
	}
}
