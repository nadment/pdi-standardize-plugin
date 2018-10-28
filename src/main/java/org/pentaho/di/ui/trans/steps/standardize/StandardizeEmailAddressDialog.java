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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
import org.pentaho.di.trans.steps.standardize.StandardizeEmailAddress;
import org.pentaho.di.trans.steps.standardize.StandardizeEmailAddressMeta;
import org.pentaho.di.ui.core.FormDataBuilder;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.ColumnsResizer;
import org.pentaho.di.ui.core.widget.TableView;

@PluginDialog(id = "standardizeemailaddress", image = "standardizeemailaddress.svg", pluginType = PluginDialog.PluginType.STEP, documentationUrl = "https://github.com/nadment/pdi-standardize-plugin/wiki")
public class StandardizeEmailAddressDialog extends AbstractStepDialog<StandardizeEmailAddressMeta> {

	private static Class<?> PKG = StandardizeEmailAddressMeta.class; // for i18n
																		// purposes

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
	public StandardizeEmailAddressDialog(Shell parent, Object in, TransMeta transMeta, String sName) {
		super(parent, in, transMeta, sName);

		setText(BaseMessages.getString(PKG, "StandardizeEmailAddressDialog.Shell.Title"));

	}

	@Override
	protected void loadMeta(final StandardizeEmailAddressMeta meta) {

		// Fields
		List<StandardizeEmailAddress> standardizes = meta.getStandardizeEmailAddresses();
		if (standardizes.size() > 0) {
			Table table = tblFields.getTable();
			// table.removeAll();
			for (int i = 0; i < standardizes.size(); i++) {
				StandardizeEmailAddress standardize = standardizes.get(i);
				TableItem ti = new TableItem(table, SWT.NONE);
				ti.setText(1, StringUtils.stripToEmpty(standardize.getInputField()));
				ti.setText(2, StringUtils.stripToEmpty(standardize.getOutputField()));
				ti.setText(3, StringUtils.stripToEmpty(standardize.getValidField()));
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
		return new Point(600, 300);
	}

	@Override
	protected void saveMeta(final StandardizeEmailAddressMeta meta) {

		// save step name
		stepname = wStepname.getText();

		// fields
		List<StandardizeEmailAddress> standardizes = new ArrayList<>();
		for (int i = 0; i < tblFields.nrNonEmpty(); i++) {
			TableItem item = tblFields.getNonEmpty(i);

			StandardizeEmailAddress standardize = new StandardizeEmailAddress();
			standardize.setInputField(StringUtils.stripToNull(item.getText(1)));
			standardize.setOutputField(StringUtils.stripToNull(item.getText(2)));
			standardize.setValidField(StringUtils.stripToNull(item.getText(3)));
			standardizes.add(standardize);
		}
		meta.setStandardizeEmailAddresses(standardizes);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		FormLayout generalLayout = new FormLayout();
		generalLayout.marginWidth = Const.FORM_MARGIN;
		generalLayout.marginHeight = Const.FORM_MARGIN;
		parent.setLayout(generalLayout);

		Label lblFields = new Label(parent, SWT.NONE);
		lblFields.setText(BaseMessages.getString(PKG, "StandardizeEmailAddressDialog.Fields.Label"));
		lblFields.setLayoutData(new FormDataBuilder().left().top().result());
		props.setLook(lblFields);

		ColumnInfo[] columns = new ColumnInfo[] {
				new ColumnInfo(BaseMessages.getString(PKG, "StandardizeEmailAddressDialog.ColumnInfo.InputField.Label"),
						ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "" }, false),
				new ColumnInfo(
						BaseMessages.getString(PKG, "StandardizeEmailAddressDialog.ColumnInfo.OutputField.Label"),
						ColumnInfo.COLUMN_TYPE_TEXT, new String[] { "" }, false),
				new ColumnInfo(BaseMessages.getString(PKG, "StandardizeEmailAddressDialog.ColumnInfo.ValidField.Label"),
						ColumnInfo.COLUMN_TYPE_TEXT, new String[] { "" }, false) };

		columns[1].setToolTip(
				BaseMessages.getString(PKG, "StandardizeEmailAddressDialog.ColumnInfo.OutputField.Tooltip"));
		columns[1].setUsingVariables(true);
		columns[2]
				.setToolTip(BaseMessages.getString(PKG, "StandardizeEmailAddressDialog.ColumnInfo.ValidField.Tooltip"));
		columns[2].setUsingVariables(true);

		tblFields = new TableView(transMeta, parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, columns, 0, lsMod,
				props);
		tblFields.setLayoutData(new FormDataBuilder().left().fullWidth().top(lblFields, Const.MARGIN).bottom().result());
		tblFields.getTable().addListener(SWT.Resize, new ColumnsResizer(4, 40, 40, 16));

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