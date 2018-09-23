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

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
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


	
	public StandardizeEmailAddressStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
			Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}

	@Override
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		// Casting to step-specific implementation classes is safe
		StandardizeEmailAddressMeta meta = (StandardizeEmailAddressMeta) smi;
		StandardizeEmailAddressData data = (StandardizeEmailAddressData) sdi;

	    if ( super.init( meta, data ) ) {
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
		Object[] outputRowValues = Arrays.copyOf(row, data.outputRowMeta.size());

		//RowMetaInterface inputRowMeta = getInputRowMeta();

		// apply rules by order
//		for (StandardizePhoneNumber standardize : meta.getStandardizePhoneNumbers()) {
//			int index = data.outputRowMeta.indexOfValue(standardize.getInputField());
//			ValueMetaInterface vm = data.outputRowMeta.getValueMeta(index);
//			try {
//				// Get value from output in case we apply multi rule on same
//				// field
//				Object value = outputRowValues[index];
//				Object result = value;
//				if (value != null) {
//					PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
//					
//					try {
//					  PhoneNumber phoneNumber = phoneUtil.parse(value.toString(), meta.getCountryCode());
//					  result = phoneUtil.format(phoneNumber, com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.E164);
//
//					} catch (NumberParseException e) {
//					  System.err.println("NumberParseException was thrown: " + e.toString());
//					}					
//				}
//
//				outputRowValues[index] = vm.convertData(inputRowMeta.getValueMeta(index), result);
//			} catch (KettleValueException e) {
//				logError(BaseMessages.getString(PKG, "StandardizePhoneNumberStep.Log.DataIncompatibleError", String.valueOf(row[index]),
//						inputRowMeta.getValueMeta(index).toString(), vm.toString()));
//				throw e;
//			}
//		}
		
		
		
		
		
		

		// put the row to the output row stream
		putRow(data.outputRowMeta, outputRowValues);

		if (log.isRowLevel()) {
			logRowlevel(BaseMessages.getString(PKG, "StandardizePhoneNumberStep.Log.WroteRowToNextStep", outputRowValues));
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