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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.WorkingDirectoryBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author "Yahor Radtsevich (yradtsevich)"
 */
public class CordovaSimLaunchConfigurationTab extends
		AbstractLaunchConfigurationTab {

	private Text rootFolderText;
	private CordovaSimWorkingDirectoryBlock rootFolderBlock;
	
	public CordovaSimLaunchConfigurationTab() {
		rootFolderBlock = new CordovaSimWorkingDirectoryBlock();
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_BOTH);
		((GridLayout) comp.getLayout()).verticalSpacing = 0;
		
		Group group = SWTFactory.createGroup(comp, "Root Folder:", 2, 1, GridData.FILL_HORIZONTAL);
		rootFolderText = SWTFactory.createSingleText(group, 1);
		
		Label fileLabel = new Label(comp, SWT.NONE);
		fileLabel.setText("File"); //TODO
		
		Text fileText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		fileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Button fileButton = new Button(comp, SWT.PUSH);
		fileButton.setText("Browse...");
		
		rootFolderBlock.createControl(comp);
		
		setControl(comp);		
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
