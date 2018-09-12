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
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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
import org.pentaho.di.trans.steps.standardize.StandardizePhoneNumber;
import org.pentaho.di.trans.steps.standardize.StandardizePhoneNumberMeta;
import org.pentaho.di.ui.core.FormDataBuilder;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.ColumnsResizer;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.dialog.AbstractStepDialog;

import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

public class StandardizePhoneNumberDialog extends AbstractStepDialog<StandardizePhoneNumberMeta> {

	private static Class<?> PKG = StandardizePhoneNumberMeta.class; // for i18n
																	// purposes
	private CCombo wCountry;
	private TableView wFields;
	private ColumnInfo[] columnInfos;
	private List<String> inputFields;

	public static void main(String[] args) {
		try {
			StandardizePhoneNumberDialog dialog = new StandardizePhoneNumberDialog(null,
					new StandardizePhoneNumberMeta(), null, "noname");
			dialog.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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
	public StandardizePhoneNumberDialog(Shell parent, Object in, TransMeta transMeta, String sName) {
		super(parent, in, transMeta, sName);

		setText(BaseMessages.getString(PKG, "StandardizePhoneNumberDialog.Shell.Title"));
	}

	@Override
	public Point getMinimumSize() {
		return new Point(600, 300);
	}

	@Override
	protected void loadMeta(final StandardizePhoneNumberMeta meta) {
		


		// Fields
		List<StandardizePhoneNumber> standardizes = meta.getStandardizePhoneNumbers();
		if (standardizes.size() > 0) {
			Table table = wFields.getTable();
			//table.removeAll();
			for (int i = 0; i < standardizes.size(); i++) {
				StandardizePhoneNumber standardize = standardizes.get(i);
				TableItem ti = new TableItem(table, SWT.NONE);
				ti.setText(1, standardize.getInputField());
				ti.setText(2, standardize.getOutputField());
				ti.setText(3, standardize.getCountryField());
				ti.setText(4, standardize.getFormat().name());				
				ti.setText(5, standardize.getPhoneNumberTypeField());
				ti.setText(6, standardize.getIsValidPhoneNumberField());				
			}
		}

		// Default country
		for (int i = 0; i < wCountry.getItemCount(); i++) {
			if (wCountry.getItem(i).equals(meta.getDefaultCountry())) {
				wCountry.select(i);
				break;
			}
		}

		wFields.removeEmptyRows();
		wFields.setRowNums();
		wFields.optWidth(true);

		wStepname.selectAll();
		wStepname.setFocus();
	}

	@Override
	protected void saveMeta(final StandardizePhoneNumberMeta meta) {

		// save step name
		stepname = wStepname.getText();
		
		List<StandardizePhoneNumber> standardizes = new ArrayList<>();
		for (int i = 0; i < wFields.nrNonEmpty(); i++) {
			TableItem item = wFields.getNonEmpty(i);

			StandardizePhoneNumber standardize = new StandardizePhoneNumber();
			standardize.setInputField(item.getText(1));
			standardize.setOutputField(item.getText(2));
			standardize.setCountryField(item.getText(3));
			standardize.setPhoneNumberTypeField(item.getText(5));			
			standardize.setIsValidPhoneNumberField(item.getText(6));

			try {
				standardize.setFormat(PhoneNumberFormat.valueOf(item.getText(4)));
			} catch (Exception e) {
				this.logError("Error parsing phone number format",e);
				standardize.setFormat(PhoneNumberFormat.E164);
			}

			standardizes.add(standardize);
		}
		meta.setStandardizePhoneNumbers(standardizes);

		if (wCountry.getSelectionIndex()>0) 
			meta.setDefaultCountry(wCountry.getItem(wCountry.getSelectionIndex()));
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		// Default country
		Label wlCountry = new Label(parent, SWT.NONE);
		wlCountry.setText(BaseMessages.getString(PKG, "StandardizePhoneNumberDialog.Country.Label"));
		wlCountry.setLayoutData(new FormDataBuilder().top().fullWidth().result());

		props.setLook(wlCountry);

		wCountry = new CCombo(parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		wCountry.setItems(this.getStepMeta().getSupportedCountries());
		wCountry.setLayoutData(new FormDataBuilder().top(wlCountry, Const.MARGIN).fullWidth().result());
		wCountry.addModifyListener(lsMod);
		props.setLook(wCountry);

		// Table with fields
		Label wlFields = new Label(parent, SWT.LEFT);
		wlFields.setText(BaseMessages.getString(PKG, "StandardizePhoneNumberDialog.Fields.Label"));
		wlFields.setLayoutData(new FormDataBuilder().top(wCountry, Const.MARGIN * 2).fullWidth().result());
		props.setLook(wlFields);

		columnInfos = new ColumnInfo[] {
				new ColumnInfo(BaseMessages.getString(PKG, "StandardizePhoneNumberDialog.ColumnInfo.InputField.Label"),
						ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "" }, false),
				new ColumnInfo(BaseMessages.getString(PKG, "StandardizePhoneNumberDialog.ColumnInfo.OutputField.Label"),
						ColumnInfo.COLUMN_TYPE_TEXT, new String[] { "" }, false),
				new ColumnInfo(BaseMessages.getString(PKG, "StandardizePhoneNumberDialog.ColumnInfo.CountryField.Label"),
						ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "" }, false),
				new ColumnInfo(BaseMessages.getString(PKG, "StandardizePhoneNumberDialog.ColumnInfo.Format.Label"),
						ColumnInfo.COLUMN_TYPE_CCOMBO, this.getStepMeta().getSupportedFormats(), false), 
				new ColumnInfo(BaseMessages.getString(PKG, "StandardizePhoneNumberDialog.ColumnInfo.PhoneNumberTypeField.Label"),
						ColumnInfo.COLUMN_TYPE_TEXT, new String[] { "" }, false),
				new ColumnInfo(BaseMessages.getString(PKG, "StandardizePhoneNumberDialog.ColumnInfo.IsValidPhoneNumberField.Label"),
						ColumnInfo.COLUMN_TYPE_TEXT, new String[] { "" }, false)
		};

		columnInfos[1].setToolTip(BaseMessages.getString(PKG, "StandardizePhoneNumberDialog.ColumnInfo.OutputField.Tooltip"));
		columnInfos[1].setUsingVariables(true);
		columnInfos[3].setToolTip(BaseMessages.getString(PKG, "StandardizePhoneNumberDialog.ColumnInfo.Format.Tooltip"));	
		columnInfos[4].setUsingVariables(true);
		columnInfos[4].setToolTip(BaseMessages.getString(PKG, "StandardizePhoneNumberDialog.ColumnInfo.PhoneNumberTypeField.Tooltip"));	
		columnInfos[5].setUsingVariables(true);
		columnInfos[5].setToolTip(BaseMessages.getString(PKG, "StandardizePhoneNumberDialog.ColumnInfo.IsValidPhoneNumberField.Tooltip"));	
		
		
		wFields = new TableView(transMeta, parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, columnInfos,
				getStepMeta().getStandardizePhoneNumbers().size(), lsMod, props);
		wFields.setLayoutData(new FormDataBuilder().left().fullWidth().top(wlFields, Const.MARGIN).bottom().result());
		wFields.getTable().addListener(SWT.Resize, new ColumnsResizer(2,25,25,12,12,12,12));
		
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
						columnInfos[2].setComboValues(fieldNames);
					}

					// Display in red missing field names
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							if (!wFields.isDisposed()) {
								for (int i = 0; i < wFields.table.getItemCount(); i++) {
									TableItem it = wFields.table.getItem(i);
									
									// Input field
									if (!Utils.isEmpty(it.getText(1))) {
										if (!inputFields.contains(it.getText(1))) {
											it.setBackground(GUIResource.getInstance().getColorRed());
										}
									}
									
									// Country field
									if (!Utils.isEmpty(it.getText(3))) {
										if (!inputFields.contains(it.getText(3))) {
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