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

package org.pentaho.di.trans.steps.standardize;

import java.util.Arrays;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * 
 * @author Nicolas ADMENT
 * @since 18-mai-2018
 *
 */

public class StandardizeEmailAddressStep extends BaseStep implements StepInterface {

	private static Class<?> PKG = StandardizeEmailAddressMeta.class;

	public StandardizeEmailAddressStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr,
			TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}

	@Override
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		// Casting to step-specific implementation classes is safe
		StandardizeEmailAddressMeta meta = (StandardizeEmailAddressMeta) smi;
		StandardizeEmailAddressData data = (StandardizeEmailAddressData) sdi;

		if (super.init(meta, data)) {
			first = true;

			return true;
		}

		return false;
	}

	@Override
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {

		// safely cast the step settings (meta) and runtime info (data) to
		// specific implementations
		StandardizeEmailAddressMeta meta = (StandardizeEmailAddressMeta) smi;
		StandardizeEmailAddressData data = (StandardizeEmailAddressData) sdi;

		// get incoming row, getRow() potentially blocks waiting for more rows,
		// returns null if no more rows expected
		Object[] row = getRow();

		// if no more rows are expected, indicate step is finished and
		// processRow() should not be called again
		if (row == null) {
			setOutputDone();
			return false;
		}

		// the "first" flag is inherited from the base step implementation
		// it is used to guard some processing tasks, like figuring out field
		// indexes
		// in the row structure that only need to be done once
		if (first) {
			if (log.isDebug()) {
				logDebug(BaseMessages.getString(PKG, "StandardizeStep.Log.StartedProcessing"));
			}

			first = false;
			// clone the input row structure and place it in our data object
			data.outputRowMeta = getInputRowMeta().clone();
			// use meta.getFields() to change it, so it reflects the output row
			// structure
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this, null, null);

		}

		RowMetaInterface inputRowMeta = getInputRowMeta();

		// copies row into outputRowValues and pads extra null-default slots for
		// the output values
		Object[] outputRow = Arrays.copyOf(row, data.outputRowMeta.size());

		// apply rules by order
		for (StandardizeEmailAddress standardize : meta.getStandardizeEmailAddresses()) {
			int index = data.outputRowMeta.indexOfValue(standardize.getInputField());
			
			// if input field not found
			if ( index<0 ) { 
				this.logError(BaseMessages.getString(PKG, "StandardizeEmailAddressStep.Log.InputFieldNotFound",standardize.getInputField()));
				this.setErrors(1);
				return false;
			}
			
			ValueMetaInterface valueMeta = data.outputRowMeta.getValueMeta(index);
			try {
				String value = inputRowMeta.getString(row, index);
				String result = value;

				if (value != null) {
					// The InternetAddress class in the Java Mail 1.3 API has a
					// constructor that does strict parsing. But the 1.1 version
					// doesn't. However, we can use the parse() method to do the
					// checking; it does do strict RFC822 syntax checks.

					InternetAddress[] addresses = InternetAddress.parse(value);
					if (addresses.length != 1) {
						throw new AddressException("\"" + value + "\" is an improperly formed " + "email address");
					}

					result = addresses[0].getAddress().toLowerCase();
				}

				if (!Utils.isEmpty(standardize.getOutputField())) {
					index = data.outputRowMeta.indexOfValue(standardize.getOutputField());
				}
				outputRow[index] = result;
				
//				if (!Utils.isEmpty(standardize.getValidField())) {
//					int i = data.outputRowMeta.indexOfValue(standardize.getValidField());
//					outputRow[i] = false;
//				}
			} catch (Exception e) {
				logError(BaseMessages.getString(PKG, "StandardizeEmailAddressStep.Log.DataIncompatibleError",
						String.valueOf(row[index]), inputRowMeta.getValueMeta(index).toString(), valueMeta.toString()));
			}
		}

		// put the row to the output row stream
		putRow(data.outputRowMeta, outputRow);

		if (log.isRowLevel()) {
			logRowlevel(BaseMessages.getString(PKG, "StandardizeEmailAddressStep.Log.WroteRowToNextStep", outputRow));
		}

		// log progress if it is time to to so
		if (checkFeedback(getLinesRead())) {
			logBasic("Line nr " + getLinesRead()); // Some basic logging
		}

		// indicate that processRow() should be called again
		return true;
	}

	/**
	 * This method is called by PDI once the step is done processing.
	 *
	 * The dispose() method is the counterpart to init() and should release any
	 * resources acquired for step execution like file handles or database
	 * connections.
	 */
	@Override
	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {

		// Casting to step-specific implementation classes is safe
		StandardizeEmailAddressMeta meta = (StandardizeEmailAddressMeta) smi;
		StandardizeEmailAddressData data = (StandardizeEmailAddressData) sdi;

		data.outputRowMeta = null;

		super.dispose(meta, data);
	}
}