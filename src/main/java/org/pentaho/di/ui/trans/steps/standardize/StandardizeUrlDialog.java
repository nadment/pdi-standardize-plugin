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

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.annotations.PluginDialog;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.standardize.StandardizeUrl;
import org.pentaho.di.trans.steps.standardize.StandardizeUrlMeta;
import org.pentaho.di.ui.core.FormDataBuilder;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.ColumnsResizer;
import org.pentaho.di.ui.core.widget.TableView;

@PluginDialog(id = "standardizeurl", image = "standardizeurl.svg", pluginType = PluginDialog.PluginType.STEP, documentationUrl = "https://github.com/nadment/pdi-standardize-plugin/wiki")
public class StandardizeUrlDialog extends AbstractStepDialog<StandardizeUrlMeta> {

	private static Class<?> PKG = StandardizeUrlMeta.class; // for i18n purposes

	private Button btnUnshorten;
	private Button btnSortQueryParameters;
	private Button btnReplaceIPWithDomainName;
	private Button btnRemoveWWW;
	private Button btnRemoveFragment;
	private Button btnRemoveDuplicateSlashes;
	private Button btnRemoveDotSegments;
	private Button btnRemoveSessionId;
	private Button btnRemoveDirectoryIndex;
	private Button btnRemoveTrailingSlash;
	private Button btnRemoveDefaultPort;

	private TableView tblFields;

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

		this.btnUnshorten.setSelection(meta.isUnshorten());
		this.btnRemoveDefaultPort.setSelection(meta.isRemoveDefaultPort());
		this.btnRemoveWWW.setSelection(meta.isRemoveWWW());
		this.btnRemoveDotSegments.setSelection(meta.isRemoveDotSegments());
		this.btnRemoveTrailingSlash.setSelection(meta.isRemoveTrailingSlash());
		this.btnRemoveDirectoryIndex.setSelection(meta.isRemoveDirectoryIndex());
		this.btnRemoveFragment.setSelection(meta.isRemoveFragment());
		this.btnRemoveSessionId.setSelection(meta.isRemoveSessionId());
		this.btnRemoveDuplicateSlashes.setSelection(meta.isRemoveDuplicateSlashes());
		this.btnReplaceIPWithDomainName.setSelection(meta.isReplaceIPWithDomainName());
		this.btnSortQueryParameters.setSelection(meta.isSortQueryParameters());

		// Fields
		List<StandardizeUrl> standardizes = meta.getStandardizeUrls();
		if (standardizes.size() > 0) {
			Table table = tblFields.getTable();
			table.removeAll();
			for (int i = 0; i < standardizes.size(); i++) {
				StandardizeUrl standardize = standardizes.get(i);

				TableItem ti = new TableItem(table, SWT.NONE);
				ti.setText(1, StringUtils.stripToEmpty(standardize.getInputField()));
				ti.setText(2, StringUtils.stripToEmpty(standardize.getOutputField()));
			}
		}

		tblFields.removeEmptyRows();
		tblFields.setRowNums();
		tblFields.optWidth(true);

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
		meta.setUnshorten(btnUnshorten.getSelection());
		meta.setRemoveDefaultPort(btnRemoveDefaultPort.getSelection());
		meta.setRemoveWWW(this.btnRemoveWWW.getSelection());
		meta.setRemoveDotSegments(this.btnRemoveDotSegments.getSelection());
		meta.setRemoveTrailingSlash(this.btnRemoveTrailingSlash.getSelection());
		meta.setRemoveDirectoryIndex(this.btnRemoveDirectoryIndex.getSelection());
		meta.setRemoveFragment(this.btnRemoveFragment.getSelection());
		meta.setRemoveSessionId(this.btnRemoveSessionId.getSelection());
		meta.setRemoveDuplicateSlashes(this.btnRemoveDuplicateSlashes.getSelection());
		meta.setReplaceIPWithDomainName(this.btnReplaceIPWithDomainName.getSelection());
		meta.setSortQueryParameters(this.btnSortQueryParameters.getSelection());

		// fields
		List<StandardizeUrl> standardizes = new ArrayList<>();
		for (int i = 0; i < tblFields.nrNonEmpty(); i++) {
			TableItem item = tblFields.getNonEmpty(i);

			StandardizeUrl standardize = new StandardizeUrl();
			standardize.setInputField(StringUtils.stripToNull(item.getText(1)));
			standardize.setOutputField(StringUtils.stripToNull(item.getText(2)));
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

		// -----------------------------------------------------------------------------
		// Preserving semantics
		// -----------------------------------------------------------------------------

		Group groupPreservingSemantics = new Group(parent, SWT.SHADOW_IN);
		groupPreservingSemantics
				.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.Group.PreservingSemantics.Label"));
		RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
		rowLayout.marginWidth = 10;
		rowLayout.marginHeight = 10;
		groupPreservingSemantics.setLayout(rowLayout);
		groupPreservingSemantics.setLayoutData(new FormDataBuilder().top().fullWidth().result());
		props.setLook(groupPreservingSemantics);

		btnRemoveDefaultPort = new Button(groupPreservingSemantics, SWT.CHECK);
		btnRemoveDefaultPort.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDefaultPort.Label"));
		btnRemoveDefaultPort
				.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDefaultPort.Tooltip"));
		btnRemoveDefaultPort.addSelectionListener(lsDef);
		props.setLook(btnRemoveDefaultPort);

		// -----------------------------------------------------------------------------
		// Usually preserving semantics
		// -----------------------------------------------------------------------------

		Group groupUsuallyPreservingSemantics = new Group(parent, SWT.SHADOW_IN);
		groupUsuallyPreservingSemantics
				.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.Group.UsuallyPreservingSemantics.Label"));
		rowLayout = new RowLayout(SWT.VERTICAL);
		rowLayout.marginWidth = 10;
		rowLayout.marginHeight = 10;
		groupUsuallyPreservingSemantics.setLayout(rowLayout);
		groupUsuallyPreservingSemantics
				.setLayoutData(new FormDataBuilder().top(groupPreservingSemantics).fullWidth().result());
		props.setLook(groupUsuallyPreservingSemantics);

		btnRemoveDotSegments = new Button(groupUsuallyPreservingSemantics, SWT.CHECK);
		btnRemoveDotSegments.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDotSegments.Label"));
		btnRemoveDotSegments
				.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDotSegments.Tooltip"));
		btnRemoveDotSegments.addSelectionListener(lsDef);
		props.setLook(btnRemoveDotSegments);

		btnSortQueryParameters = new Button(groupUsuallyPreservingSemantics, SWT.CHECK);
		btnSortQueryParameters.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.SortQueryParameters.Label"));
		btnSortQueryParameters
				.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.SortQueryParameters.Tooltip"));
		btnSortQueryParameters.addSelectionListener(lsDef);
		props.setLook(btnSortQueryParameters);

		// -----------------------------------------------------------------------------
		// Not preserving semantics
		// -----------------------------------------------------------------------------

		Group groupNotPreservingSemantics = new Group(parent, SWT.SHADOW_IN);
		groupNotPreservingSemantics
				.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.Group.NotPreservingSemantics.Label"));
		rowLayout = new RowLayout(SWT.VERTICAL);
		rowLayout.marginWidth = 10;
		rowLayout.marginHeight = 10;
		groupNotPreservingSemantics.setLayout(rowLayout);
		groupNotPreservingSemantics
				.setLayoutData(new FormDataBuilder().top(groupUsuallyPreservingSemantics).fullWidth().result());
		props.setLook(groupNotPreservingSemantics);

		btnRemoveWWW = new Button(groupNotPreservingSemantics, SWT.CHECK);
		btnRemoveWWW.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveWWW.Label"));
		btnRemoveWWW.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveWWW.Tooltip"));
		btnRemoveWWW.addSelectionListener(lsDef);
		props.setLook(btnRemoveWWW);

		btnRemoveTrailingSlash = new Button(groupNotPreservingSemantics, SWT.CHECK);
		btnRemoveTrailingSlash.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveTrailingSlash.Label"));
		btnRemoveTrailingSlash
				.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveTrailingSlash.Tooltip"));
		btnRemoveTrailingSlash.addSelectionListener(lsDef);
		props.setLook(btnRemoveTrailingSlash);

		btnRemoveDirectoryIndex = new Button(groupNotPreservingSemantics, SWT.CHECK);
		btnRemoveDirectoryIndex.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDirectoryIndex.Label"));
		btnRemoveDirectoryIndex
				.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDirectoryIndex.Tooltip"));
		btnRemoveDirectoryIndex.addSelectionListener(lsDef);
		props.setLook(btnRemoveDirectoryIndex);

		btnRemoveFragment = new Button(groupNotPreservingSemantics, SWT.CHECK);
		btnRemoveFragment.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveFragment.Label"));
		btnRemoveFragment.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveFragment.Tooltip"));
		btnRemoveFragment.addSelectionListener(lsDef);
		props.setLook(btnRemoveFragment);

		btnRemoveSessionId = new Button(groupNotPreservingSemantics, SWT.CHECK);
		btnRemoveSessionId.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveSessionId.Label"));
		btnRemoveSessionId.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveSessionId.Tooltip"));
		btnRemoveSessionId.addSelectionListener(lsDef);
		props.setLook(btnRemoveSessionId);

		btnRemoveDuplicateSlashes = new Button(groupNotPreservingSemantics, SWT.CHECK);
		btnRemoveDuplicateSlashes
				.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDuplicateSlashes.Label"));
		btnRemoveDuplicateSlashes
				.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.RemoveDuplicateSlashes.Tooltip"));
		btnRemoveDuplicateSlashes.addSelectionListener(lsDef);
		props.setLook(btnRemoveSessionId);

		btnReplaceIPWithDomainName = new Button(groupNotPreservingSemantics, SWT.CHECK);
		btnReplaceIPWithDomainName
				.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.ReplaceIPWithDomainName.Label"));
		btnReplaceIPWithDomainName
				.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.ReplaceIPWithDomainName.Tooltip"));
		btnReplaceIPWithDomainName.addSelectionListener(lsDef);
		props.setLook(btnReplaceIPWithDomainName);

		btnUnshorten = new Button(groupNotPreservingSemantics, SWT.CHECK);
		btnUnshorten.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.UnshortenUrl.Label"));
		btnUnshorten.setToolTipText(BaseMessages.getString(PKG, "StandardizeUrlDialog.UnshortenUrl.Tooltip"));
		btnUnshorten.addSelectionListener(lsDef);
		props.setLook(btnUnshorten);

		Label lblFields = new Label(parent, SWT.NONE);
		lblFields.setText(BaseMessages.getString(PKG, "StandardizeUrlDialog.Fields.Label"));
		lblFields.setLayoutData(
				new FormDataBuilder().top(groupNotPreservingSemantics, 2 * Const.MARGIN).fullWidth().result());
		props.setLook(lblFields);

		ColumnInfo[] columns = new ColumnInfo[] {
				new ColumnInfo(BaseMessages.getString(PKG, "StandardizeUrlDialog.ColumnInfo.InputField.Label"),
						ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "" }, false),
				new ColumnInfo(BaseMessages.getString(PKG, "StandardizeUrlDialog.ColumnInfo.OutputField.Label"),
						ColumnInfo.COLUMN_TYPE_TEXT, new String[] { "" }, false) };

		columns[1].setToolTip(BaseMessages.getString(PKG, "StandardizeUrlDialog.ColumnInfo.OutputField.Tooltip"));
		columns[1].setUsingVariables(true);

		tblFields = new TableView(transMeta, parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, columns, 0, lsMod,
				props);
		tblFields
				.setLayoutData(new FormDataBuilder().left().fullWidth().top(lblFields, Const.MARGIN).bottom().result());
		tblFields.getTable().addListener(SWT.Resize, new ColumnsResizer(4, 48, 48));

		// -----------------------------------------------------------------------------
		// Search the fields in the background
		// -----------------------------------------------------------------------------
		final Runnable runnable = () -> {
			StepMeta stepMeta = transMeta.findStep(stepname);
			if (stepMeta != null) {
				try {
					RowMetaInterface row = transMeta.getPrevStepFields(stepMeta);
					final List<String> inputFields = new ArrayList<>();

					if (row != null) {
						for (ValueMetaInterface vm : row.getValueMetaList()) {
							inputFields.add(vm.getName());
						}

						// Sort by name
						String[] fieldNames = Const.sortStrings(inputFields.toArray(new String[0]));

						columns[0].setComboValues(fieldNames);
					}
				} catch (KettleException e) {
					logError(BaseMessages.getString(PKG, "System.Dialog.GetFieldsFailed.Message"));
				}
			}
		};
		new Thread(runnable).start();

		return parent;
	}

}