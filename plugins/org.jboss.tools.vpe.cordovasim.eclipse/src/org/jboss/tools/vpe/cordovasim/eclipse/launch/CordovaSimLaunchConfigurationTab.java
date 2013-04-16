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
package org.jboss.tools.vpe.cordovasim.eclipse.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsMessages;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.WorkingDirectoryBlock;
import org.eclipse.jdt.internal.ui.viewsupport.FilteredElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ArchiveFileFilter;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;
import org.jboss.tools.vpe.cordovasim.eclipse.Activator;

/**
 * @author "Yahor Radtsevich (yradtsevich)"
 */
@SuppressWarnings("restriction")
public class CordovaSimLaunchConfigurationTab extends
		AbstractLaunchConfigurationTab {

	private Image image = Activator.getImageDescriptor("icons/cordovasim_16.png").createImage();
	private WidgetListener defaultListener = new WidgetListener();
	private Text rootFolderText;
	private Text startPageText;
	
	public CordovaSimLaunchConfigurationTab() {
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_BOTH);
		((GridLayout) comp.getLayout()).verticalSpacing = 0;
		
		createRootFolderEditor(comp);
		createStartPageEditor(comp);
				
		setControl(comp);
	}

	private void createRootFolderEditor(Composite parent) {
		Group group = SWTFactory.createGroup(parent, "Root Folder:", 2, 1, GridData.FILL_HORIZONTAL);
		rootFolderText = SWTFactory.createSingleText(group, 1);
		rootFolderText.addModifyListener(defaultListener);
		Button rootFolderButton = createPushButton(group, "&Browse...", null); 
		rootFolderButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRootFolderButtonSelected();				
			}
		});
	}
	
	private void createStartPageEditor(Composite parent) {
		Group group = SWTFactory.createGroup(parent, "Start Page:", 2, 1, GridData.FILL_HORIZONTAL);
		startPageText = SWTFactory.createSingleText(group, 1);
		startPageText.addModifyListener(defaultListener);
		Button rootFolderButton = createPushButton(group, "&Browse...", null); 
		rootFolderButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleStartPageButtonSelected();				
			}
		});
	}
	
	protected void handleRootFolderButtonSelected() {
		String rootFolderString = rootFolderText.getText();
		IContainer rootFolder = getContainer(rootFolderString);
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(),
				rootFolder,
				false,
				"Select location of the root folder");
		dialog.setDialogBoundsSettings(getDialogBoundsSettings(Activator.PLUGIN_ID + ".ROOT_FOLDER_LOCATION_DIALOG"),
				Dialog.DIALOG_PERSISTSIZE);
		dialog.showClosedProjects(false);
		
		dialog.open();
		Object[] results = dialog.getResult();	
		if ((results != null) && (results.length > 0) && (results[0] instanceof IPath)) {
			IPath path = (IPath)results[0];
			String containerName = path.toString();
			rootFolderText.setText(containerName);
		}
	}
	
	protected void handleStartPageButtonSelected() {
		String rootFolderString = rootFolderText.getText();
		IContainer rootFolder = getContainer(rootFolderString);
		if (rootFolder == null) {
			rootFolder = ResourcesPlugin.getWorkspace().getRoot();
		}
		
		String startPageString = startPageText.getText();
		IResource startPage = getResource(rootFolder, startPageString);
		
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setTitle("Start Page Selection");
		dialog.setMessage("&Choose the file to be the start page:");
		dialog.setInput(rootFolder);
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
		dialog.setDialogBoundsSettings(getDialogBoundsSettings(Activator.PLUGIN_ID + ".ROOT_START_PAGE_LOCATION_DIALOG"),
				Dialog.DIALOG_PERSISTSIZE);
		dialog.setInitialSelection(startPage);
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IFile) {
					String extension = ((IFile) element).getFileExtension();
					return "html".equals(extension) || "htm".equals(extension);
				}
				return true;
			}
		});

		dialog.open();
		Object result = dialog.getFirstResult();
		if (result instanceof IFile) {
			IFile file = (IFile) result;
			if (rootFolder != null) {
				int fullPathSegmentsCount = rootFolder.getFullPath().segmentCount();
				IPath relativeFile = file.getFullPath().removeFirstSegments(fullPathSegmentsCount);
				startPageText.setText(relativeFile.toString());
			}
		}
	}
	
	private IResource getResource(IContainer root, String resourcePath) {
		if (resourcePath != null && resourcePath.length() > 0) {
			return root.findMember(new Path(resourcePath));
		}
		return null;
	}
	
	private IContainer getContainer(String containerPath) {
		IContainer root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = getResource(root, containerPath);
		if (resource instanceof IContainer) {
			return (IContainer) resource;
		}
		return null;
	}

	/**
	 * Returns the {@link IDialogSettings} for the given id
	 * 
	 * @param id the id of the dialog settings to get
	 * @return the {@link IDialogSettings} to pass into the {@link ContainerSelectionDialog}
	 */
	IDialogSettings getDialogBoundsSettings(String id) {
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(id);
		if (section == null) {
			section = settings.addNewSection(id);
		} 
		return section;
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		return "&Main";
	}
	
	@Override
	public Image getImage() {
		return image;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		image.dispose();
	}
	
	private class WidgetListener implements ModifyListener, SelectionListener {
		
		public void modifyText(ModifyEvent e) {
			updateLaunchConfigurationDialog();
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {/*do nothing*/}
		
		public void widgetSelected(SelectionEvent e) {
			updateLaunchConfigurationDialog();
		}
	}
}

class CordovaSimWorkingDirectoryBlock extends WorkingDirectoryBlock {

	protected CordovaSimWorkingDirectoryBlock() {
		super("org.jboss.tools.vpe.cordovasim.eclipse.launch.rootFolder");
	}

	@Override
	protected IProject getProject(ILaunchConfiguration configuration)
			throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}
}
