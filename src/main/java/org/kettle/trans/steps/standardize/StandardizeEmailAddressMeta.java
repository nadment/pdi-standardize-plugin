/******************************************************************************
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

package org.kettle.trans.steps.standardize;

import java.util.ArrayList;
import java.util.List;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.injection.InjectionDeep;
import org.pentaho.di.core.injection.InjectionSupported;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

/**
 * Lets you combine multiple fields into one, selecting the first value that is
 * non-null.
 * 
 * @author Nicolas ADMENT
 *
 */
@Step(id = "StandardizeEmailAddress", image = "standardizeemailaddress.svg", i18nPackageName = "org.kettle.trans.steps.standardize", name = "StandardizeEmailAddress.Name", description = "StandardizeEmailAddress.Description", categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.DataQuality", documentationUrl = "https://help.pentaho.com")
@InjectionSupported(localizationPrefix = "StandardizeEmailAddressMeta.Injection.", groups = { "FIELDS" })
public class StandardizeEmailAddressMeta extends BaseStepMeta implements StepMetaInterface {

	private static final Class<?> PKG = StandardizeEmailAddressMeta.class; // for i18n purposes

	/**
	 * Constants:
	 */

	private static final String TAG_INPUT_FIELD = "input_field"; //$NON-NLS-1$

	private static final String TAG_OUTPUT_FIELD = "output_field"; //$NON-NLS-1$

	private static final String TAG_VALID_FIELD = "valid_field"; //$NON-NLS-1$

	/** The email to standardize */
	@InjectionDeep
	private List<StandardizeEmailAddress> standardizes = new ArrayList<>();

	public StandardizeEmailAddressMeta() {
		super();
	}

	/**
	 * Called by PDI to get a new instance of the step implementation. A
	 * standard implementation passing the arguments to the constructor of the
	 * step class is recommended.
	 *
	 * @param stepMeta
	 *            description of the step
	 * @param stepDataInterface
	 *            instance of a step data class
	 * @param cnr
	 *            copy number
	 * @param transMeta
	 *            description of the transformation
	 * @param disp
	 *            runtime implementation of the transformation
	 * @return the new instance of a step implementation
	 */
	@Override
	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
			Trans disp) {
		return new StandardizeEmailAddressStep(stepMeta, stepDataInterface, cnr, transMeta, disp);
	}

	/**
	 * Called by PDI to get a new instance of the step data class.
	 */
	@Override
	public StepDataInterface getStepData() {
		return new StandardizeEmailAddressData();
	}

	/**
	 * This method is called every time a new step is created and should
	 * allocate/set the step configuration to sensible defaults. The values set
	 * here will be used by Spoon when a new step is created.
	 */
	@Override
	public void setDefault() {

		this.standardizes = new ArrayList<>();
	}

	@Override
	public Object clone() {
		StandardizeEmailAddressMeta clone = (StandardizeEmailAddressMeta) super.clone();

		return clone;
	}

	@Override
	public String getXML() throws KettleValueException {

		StringBuilder xml = new StringBuilder(500);

		xml.append("<fields>");
		for (StandardizeEmailAddress standardize : this.getStandardizeEmailAddresses()) {
			xml.append("<field>");
			xml.append(XMLHandler.addTagValue(TAG_INPUT_FIELD, standardize.getInputField()));
			xml.append(XMLHandler.addTagValue(TAG_OUTPUT_FIELD, standardize.getOutputField()));
			xml.append(XMLHandler.addTagValue(TAG_VALID_FIELD, standardize.getValidField()));
			xml.append("</field>");
		}
		xml.append("</fields>");

		return xml.toString();
	}

	@Override
	public void loadXML(Node stepNode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {

		try {
			Node fields = XMLHandler.getSubNode(stepNode, "fields");
			int count = XMLHandler.countNodes(fields, "field");
			this.standardizes = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				Node field = XMLHandler.getSubNodeByNr(fields, "field", i);

				StandardizeEmailAddress standardize = new StandardizeEmailAddress();
				standardize.setInputField(XMLHandler.getTagValue(field, TAG_INPUT_FIELD));
				standardize.setOutputField(XMLHandler.getTagValue(field, TAG_OUTPUT_FIELD));
				standardize.setValidField(XMLHandler.getTagValue(field, TAG_VALID_FIELD));
				standardizes.add(standardize);
			}
		} catch (Exception e) {
			throw new KettleXMLException(
					BaseMessages.getString(PKG, "StandardizeMeta.Exception.UnableToReadStepInfoFromXML"), e);
		}
	}

	@Override
	public void saveRep(Repository repository, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step)
			throws KettleException {
		try {
			for (int i = 0; i < this.standardizes.size(); i++) {
				StandardizeEmailAddress standardize = standardizes.get(i);
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_INPUT_FIELD,
						standardize.getInputField());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_OUTPUT_FIELD,
						standardize.getOutputField());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_VALID_FIELD,
						standardize.getValidField());
			}
		} catch (Exception e) {
			throw new KettleException(
					BaseMessages.getString(PKG, "StandardizeMeta.Exception.UnableToSaveRepository", id_step), e);
		}
	}

	@Override
	public void readRep(Repository repository, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases)
			throws KettleException {
		try {

			int count = repository.countNrStepAttributes(id_step, TAG_INPUT_FIELD);
			this.standardizes = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				StandardizeEmailAddress standardize = new StandardizeEmailAddress();
				standardize.setInputField(repository.getStepAttributeString(id_step, i, TAG_INPUT_FIELD));
				standardize.setOutputField(repository.getStepAttributeString(id_step, i, TAG_OUTPUT_FIELD));
				standardize.setValidField(repository.getStepAttributeString(id_step, i, TAG_VALID_FIELD));
				standardizes.add(standardize);
			}
		} catch (Exception e) {

			throw new KettleException(
					BaseMessages.getString(PKG, "StandardizeMeta.Exception.UnableToReadRepository", id_step), e);
		}
	}

	/**
	 * This method is called to determine the changes the step is making to the
	 * row-stream.
	 *
	 * @param inputRowMeta
	 *            the row structure coming in to the step
	 * @param stepName
	 *            the name of the step making the changes
	 * @param info
	 *            row structures of any info steps coming in
	 * @param nextStep
	 *            the description of a step this step is passing rows to
	 * @param space
	 *            the variable space for resolving variables
	 * @param repository
	 *            the repository instance optionally read from
	 * @param metaStore
	 *            the metaStore to optionally read from
	 */
	@Override
	public void getFields(RowMetaInterface inputRowMeta, String stepName, RowMetaInterface[] info, StepMeta nextStep,
			VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException {
		try {
			// add the output fields if specified
			for (StandardizeEmailAddress standardize : this.getStandardizeEmailAddresses()) {
				if (!Utils.isEmpty(standardize.getOutputField())) {
					// create ValueMeta
					ValueMetaInterface vm = ValueMetaFactory.createValueMeta(standardize.getOutputField(),
							ValueMetaInterface.TYPE_STRING);
					vm.setOrigin(stepName);
					inputRowMeta.addValueMeta(vm);
					
					// add valid field
					if (!Utils.isEmpty(standardize.getValidField())) {
						vm = ValueMetaFactory.createValueMeta(standardize.getValidField(),
								ValueMetaInterface.TYPE_BOOLEAN);
						vm.setOrigin(stepName);
						inputRowMeta.addValueMeta(vm);
					}

				}
			}

		} catch (Exception e) {
			throw new KettleStepException(e);
		}
	}

	/**
	 * This method is called when the user selects the "Verify Transformation"
	 * option in Spoon.
	 *
	 * @param remarks
	 *            the list of remarks to append to
	 * @param transMeta
	 *            the description of the transformation
	 * @param stepMeta
	 *            the description of the step
	 * @param prev
	 *            the structure of the incoming row-stream
	 * @param input
	 *            names of steps sending input to the step
	 * @param output
	 *            names of steps this step is sending output to
	 * @param info
	 *            fields coming in from info steps
	 * @param metaStore
	 *            metaStore to optionally read from
	 */
	@Override
	public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev,
			String input[], String output[], RowMetaInterface info, VariableSpace space, Repository repository,
			IMetaStore metaStore) {

		// See if we have fields from previous steps
		if (prev == null || prev.size() == 0) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_WARNING,
					BaseMessages.getString(PKG, "StandardizeMeta.CheckResult.NotReceivingFieldsFromPreviousSteps"),
					stepMeta));
		} else {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG,
					"StandardizeMeta.CheckResult.ReceivingFieldsFromPreviousSteps", prev.size()), stepMeta));
		}

		// See if there are input streams leading to this step!
		if (input.length > 0) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_OK,
					BaseMessages.getString(PKG, "StandardizeMeta.CheckResult.ReceivingInfoFromOtherSteps"), stepMeta));

			// Check only if input fields
			for (StandardizeEmailAddress standardize : this.getStandardizeEmailAddresses()) {

				// See if there are missing input streams
				ValueMetaInterface vmi = prev.searchValueMeta(standardize.getInputField());
				if (vmi == null) {
					String message = BaseMessages.getString(PKG, "StandardizeMeta.CheckResult.MissingInputField",
							standardize.getInputField());
					remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, message, stepMeta));
				}
			}
		} else {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR,
					BaseMessages.getString(PKG, "StandardizeMeta.CheckResult.NotReceivingInfoFromOtherSteps"),
					stepMeta));
		}
	}

	public List<StandardizeEmailAddress> getStandardizeEmailAddresses() {
		return this.standardizes;
	}

	public void setStandardizeEmailAddresses(final List<StandardizeEmailAddress> standardizes) {
		this.standardizes = standardizes;
	}
}