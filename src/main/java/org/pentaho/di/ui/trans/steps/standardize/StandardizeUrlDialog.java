/*******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.ui.trans.steps.standardize;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.standardize.StandardizeUrl;
import org.pentaho.di.trans.steps.standardize.StandardizeUrlMeta;
import org.pentaho.di.ui.core.FormDataBuilder;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.ColumnsResizer;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.dialog.AbstractStepDialog;

public class StandardizeUrlDialog extends AbstractStepDialog<StandardizeUrlMeta> {

	private static Class<?> PKG = StandardizeUrlMeta.class; // for i18n purposes

	private Button wUnshorten;
	private Button wSortQueryParameters;
	private Button wReplaceIPWithDomainName;
	private Button wRemoveWWW;
	private Button wRemoveFragment;
	private Button wRemoveDuplicateSlashes;	
	private Button wRemoveDotSegments;
	private Button wRemoveSessionId;
	private Button wRemoveDirectoryIndex;
	private Button wRemoveTrailingSlash;
	private Button wRemoveDefaultPort;
	
	private TableView wFields;
	private ColumnInfo[] columnInfos;
	private List<String> inputFields;
	
	/**
	 * Constructor that saves incoming meta object to a local variable, so it
	 * can conveniently read and write settings from/to it.
	 *
	 * @param parent
	 *            the SWT shell to open the dialog in
	 * @param in
	 *            the meta object holding the step's settings
	 * @param transMeta
	 *            transformation description
	 * @param sName
	 *            the step name
	 */
	public StandardizeUrlDialog(Shell parent, Object in, TransMeta transMeta, String sName) {
		super(parent, in, transMeta, sName);

		setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.Shell.Title"));

	}

	@Override
	protected void loadMeta(final StandardizeUrlMeta meta) {
		
		this.wUnshorten.setSelection(meta.isUnshorten());
		this.wRemoveDefaultPort.setSelection(meta.isRemoveDefaultPort());
		this.wRemoveWWW.setSelection(meta.isRemoveWWW());
		this.wRemoveDotSegments.setSelection(meta.isRemoveDotSegments());
		this.wRemoveTrailingSlash.setSelection(meta.isRemoveTrailingSlash());
		this.wRemoveDirectoryIndex.setSelection(meta.isRemoveDirectoryIndex());
		this.wRemoveFragment.setSelection(meta.isRemoveFragment());		
		this.wRemoveSessionId.setSelection(meta.isRemoveSessionId());
		this.wRemoveDuplicateSlashes.setSelection(meta.isRemoveDuplicateSlashes());
		this.wReplaceIPWithDomainName.setSelection(meta.isReplaceIPWithDomainName());
		this.wSortQueryParameters.setSelection(meta.isSortQueryParameters());
		
		// Fields
		List<StandardizeUrl> standardizes = meta.getStandardizeUrls();
		if (standardizes.size() > 0) {
			Table table = wFields.getTable();
			//table.removeAll();
			for (int i = 0; i < standardizes.size(); i++) {
				StandardizeUrl standardize = standardizes.get(i);
				TableItem ti = new TableItem(table, SWT.NONE);
				ti.setText(1, standardize.getInputField());
				ti.setText(2, standardize.getOutputField());				
			}
		}

		wFields.removeEmptyRows();
		wFields.setRowNums();
		wFields.optWidth(true);

		wStepname.selectAll();
		wStepname.setFocus();
	}

	@Override
	public Point getMinimumSize() {
		return new Point(400, 400);
	}

	@Override
	protected void saveMeta(final StandardizeUrlMeta meta) {

		// save step name
		stepname = wStepname.getText();

		// options
		meta.setUnshorten(wUnshorten.getSelection());
		meta.setRemoveDefaultPort(wRemoveDefaultPort.getSelection());
		meta.setRemoveWWW(this.wRemoveWWW.getSelection());
		meta.setRemoveDotSegments(this.wRemoveDotSegments.getSelection());	
		meta.setRemoveTrailingSlash(this.wRemoveTrailingSlash.getSelection());
		meta.setRemoveDirectoryIndex(this.wRemoveDirectoryIndex.getSelection());
		meta.setRemoveFragment(this.wRemoveFragment.getSelection());
		meta.setRemoveSessionId(this.wRemoveSessionId.getSelection());
		meta.setRemoveDuplicateSlashes(this.wRemoveDuplicateSlashes.getSelection());
		meta.setReplaceIPWithDomainName(this.wReplaceIPWithDomainName.getSelection());
		meta.setSortQueryParameters(this.wSortQueryParameters.getSelection());
		
		// fields
		List<StandardizeUrl> standardizes = new ArrayList<>();
		for (int i = 0; i < wFields.nrNonEmpty(); i++) {
			TableItem item = wFields.getNonEmpty(i);

			StandardizeUrl standardize = new StandardizeUrl();
			standardize.setInputField(item.getText(1));
			standardize.setOutputField(item.getText(2));
			standardizes.add(standardize);
		}
		meta.setStandardizeUrls(standardizes);	
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		
		this.lsDef = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				baseStepMeta.setChanged();
			}
		};		
		
	    //-----------------------------------------------------------------------------
	    // Preserving semantics
	    //-----------------------------------------------------------------------------
		
	    Group groupPreservingSemantics = new Group(parent, SWT.SHADOW_IN);
	    groupPreservingSemantics.setText(BaseMessages.getString(PKG,"StandardizeUrlDialog.Group.PreservingSemantics.Label"));
	    RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
	    rowLayout.marginWidth = 10;
	    rowLayout.marginHeight = 10;    
	    groupPreservingSemantics.setLayout(rowLayout);
	    groupPreservingSemantics.setLayoutData(new FormDataBuilder().top().fullWidth().result());
	    props.setLook(groupPreservingSemantics);
		
	    
		wRemoveDefaultPort = new Button(groupPreservingSemantics, SWT.CHECK);
		wRemoveDefaultPort.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDefaultPort.Label"));
		wRemoveDefaultPort.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDefaultPort.Tooltip"));
		wRemoveDefaultPort.addSelectionListener(lsDef);
		props.setLook(wRemoveDefaultPort);
	    	    
	    //-----------------------------------------------------------------------------
	    // Usually preserving semantics
	    //-----------------------------------------------------------------------------
	    	    
	    Group groupUsuallyPreservingSemantics = new Group(parent, SWT.SHADOW_IN);
	    groupUsuallyPreservingSemantics.setText(BaseMessages.getString(PKG,"StandardizeUrlDialog.Group.UsuallyPreservingSemantics.Label"));
	    rowLayout = new RowLayout(SWT.VERTICAL);
	    rowLayout.marginWidth = 10;
	    rowLayout.marginHeight = 10;    
	    groupUsuallyPreservingSemantics.setLayout(rowLayout);
	    groupUsuallyPreservingSemantics.setLayoutData(new FormDataBuilder().top(groupPreservingSemantics).fullWidth().result());
	    props.setLook(groupUsuallyPreservingSemantics);
	    
		wRemoveDotSegments = new Button(groupUsuallyPreservingSemantics, SWT.CHECK);
		wRemoveDotSegments.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDotSegments.Label"));
		wRemoveDotSegments.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDotSegments.Tooltip"));
		wRemoveDotSegments.addSelectionListener(lsDef);
		props.setLook(wRemoveDotSegments);
	    
		wSortQueryParameters = new Button(groupUsuallyPreservingSemantics, SWT.CHECK);
		wSortQueryParameters.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.SortQueryParameters.Label"));
		wSortQueryParameters.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.SortQueryParameters.Tooltip"));
		wSortQueryParameters.addSelectionListener(lsDef);
		props.setLook(wSortQueryParameters);
		
	    //-----------------------------------------------------------------------------
	    // Not preserving semantics
	    //-----------------------------------------------------------------------------
	    
	    Group groupNotPreservingSemantics = new Group(parent, SWT.SHADOW_IN);
	    groupNotPreservingSemantics.setText(BaseMessages.getString(PKG,"StandardizeUrlDialog.Group.NotPreservingSemantics.Label"));
	    rowLayout = new RowLayout(SWT.VERTICAL);
	    rowLayout.marginWidth = 10;
	    rowLayout.marginHeight = 10;    
	    groupNotPreservingSemantics.setLayout(rowLayout);
	    groupNotPreservingSemantics.setLayoutData(new FormDataBuilder().top(groupUsuallyPreservingSemantics).fullWidth().result());
	    props.setLook(groupNotPreservingSemantics);
	    	    
		wRemoveWWW = new Button(groupNotPreservingSemantics, SWT.CHECK);
		wRemoveWWW.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveWWW.Label"));
		wRemoveWWW.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveWWW.Tooltip"));
		wRemoveWWW.addSelectionListener(lsDef);
		props.setLook(wRemoveWWW);

		wRemoveTrailingSlash = new Button(groupNotPreservingSemantics, SWT.CHECK);
		wRemoveTrailingSlash.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveTrailingSlash.Label"));
		wRemoveTrailingSlash.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveTrailingSlash.Tooltip"));
		wRemoveTrailingSlash.addSelectionListener(lsDef);
		props.setLook(wRemoveTrailingSlash);
		
		wRemoveDirectoryIndex = new Button(groupNotPreservingSemantics, SWT.CHECK);
		wRemoveDirectoryIndex.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDirectoryIndex.Label"));
		wRemoveDirectoryIndex.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDirectoryIndex.Tooltip"));
		wRemoveDirectoryIndex.addSelectionListener(lsDef);
		props.setLook(wRemoveDirectoryIndex);
				
		wRemoveFragment = new Button(groupNotPreservingSemantics, SWT.CHECK);
		wRemoveFragment.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveFragment.Label"));
		wRemoveFragment.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveFragment.Tooltip"));
		wRemoveFragment.addSelectionListener(lsDef);
		props.setLook(wRemoveFragment);
				
		wRemoveSessionId = new Button(groupNotPreservingSemantics, SWT.CHECK);
		wRemoveSessionId.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveSessionId.Label"));
		wRemoveSessionId.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveSessionId.Tooltip"));
		wRemoveSessionId.addSelectionListener(lsDef);
		props.setLook(wRemoveSessionId);
		
		wRemoveDuplicateSlashes = new Button(groupNotPreservingSemantics, SWT.CHECK);
		wRemoveDuplicateSlashes.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDuplicateSlashes.Label"));
		wRemoveDuplicateSlashes.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDuplicateSlashes.Tooltip"));
		wRemoveDuplicateSlashes.addSelectionListener(lsDef);
		props.setLook(wRemoveSessionId);
		
		wReplaceIPWithDomainName = new Button(groupNotPreservingSemantics, SWT.CHECK);
		wReplaceIPWithDomainName.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.ReplaceIPWithDomainName.Label"));
		wReplaceIPWithDomainName.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.ReplaceIPWithDomainName.Tooltip"));
		wReplaceIPWithDomainName.addSelectionListener(lsDef);
		props.setLook(wReplaceIPWithDomainName);

		

		
		wUnshorten = new Button(groupNotPreservingSemantics, SWT.CHECK);
		wUnshorten.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.UnshortenUrl.Label"));
		wUnshorten.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.UnshortenUrl.Tooltip"));
		wUnshorten.addSelectionListener(lsDef);
		props.setLook(wUnshorten);

						
		Label wlFields = new Label(parent, SWT.NONE);
		wlFields.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.Fields.Label"));
		wlFields.setLayoutData(new FormDataBuilder().top(groupNotPreservingSemantics, 2 * Const.MARGIN).fullWidth().result());
		props.setLook(wlFields);


		columnInfos = new ColumnInfo[] {
				new ColumnInfo(BaseMessages.getString(PKG, "StandardizeUrlDialog.ColumnInfo.InputField.Label"),
						ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "" }, false),
				new ColumnInfo(BaseMessages.getString(PKG, "StandardizeUrlDialog.ColumnInfo.OutputField.Label"),
						ColumnInfo.COLUMN_TYPE_TEXT, new String[] { "" }, false)
		};

		columnInfos[1].setToolTip(BaseMessages.getString(PKG, "StandardizeUrlDialog.ColumnInfo.OutputField.Tooltip"));
		columnInfos[1].setUsingVariables(true);
		
		
		wFields = new TableView(transMeta, parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, columnInfos,
				getStepMeta().getStandardizeUrls().size(), lsMod, props);
		wFields.setLayoutData(new FormDataBuilder().left().fullWidth().top(wlFields, Const.MARGIN).bottom().result());
		wFields.getTable().addListener(SWT.Resize, new ColumnsResizer(4,48,48));
		
		//
		// Search the fields in the background
		//

		final Runnable runnable = () -> {
			StepMeta stepMeta = transMeta.findStep(stepname);
			if (stepMeta != null) {
				try {
					RowMetaInterface row = transMeta.getPrevStepFields(stepMeta);
					if (row != null) {
						// Remember these fields...
						inputFields = new ArrayList<>();
						for (ValueMetaInterface vm : row.getValueMetaList()) {
							inputFields.add(vm.getName());
						}					
						
						// Sort by name
						String[] fieldNames = Const.sortStrings(inputFields.toArray(new String[0]));
						columnInfos[0].setComboValues(fieldNames);						
					}

					// Display in red missing field names
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							if (!wFields.isDisposed()) {
								for (int i = 0; i < wFields.table.getItemCount(); i++) {
									TableItem it = wFields.table.getItem(i);
									if (!Utils.isEmpty(it.getText(1))) {
										if (!inputFields.contains(it.getText(1))) {
											it.setBackground(GUIResource.getInstance().getColorRed());
										}
									}
								}
							}
						}
					});

				} catch (KettleException e) {
					logError(BaseMessages.getString(PKG, "System.Dialog.GetFieldsFailed.Message"));
				}
			}
		};
		new Thread(runnable).start();

		
		return parent;
	}

}