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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Const;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.steps.standardize.StandardizeEmailAddressMeta;
import org.pentaho.di.ui.core.FormDataBuilder;
import org.pentaho.di.ui.dialog.AbstractStepDialog;

public class StandardizeEmailAddressDialog extends AbstractStepDialog<StandardizeEmailAddressMeta> {

	private static Class<?> PKG = StandardizeEmailAddressMeta.class; // for i18n purposes

	private Button wUnshorten;
	
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

	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		FormLayout generalLayout = new FormLayout();
		generalLayout.marginWidth = Const.FORM_MARGIN;
		generalLayout.marginHeight = Const.FORM_MARGIN;
		parent.setLayout(generalLayout);

//		// Widget Spaces and Nulls
//		wUnshorten = new Button(parent, SWT.CHECK);
//		wUnshorten.setText(BaseMessages.getString(PKG, "StandardizeEmailDialog.UnshortenUrl.Label"));
//		wUnshorten.setLayoutData(new FormDataBuilder().left().top().result());
//		wUnshorten.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				baseStepMeta.setChanged();
//			}
//		});
//		props.setLook(wUnshorten);

		Label wlFields = new Label(parent, SWT.NONE);
		wlFields.setText(BaseMessages.getString(PKG, "StandardizeEmailAddressDialog.Fields.Label"));
		wlFields.setLayoutData(new FormDataBuilder().left().top().result());
		props.setLook(wlFields);

		return parent;
	}

}