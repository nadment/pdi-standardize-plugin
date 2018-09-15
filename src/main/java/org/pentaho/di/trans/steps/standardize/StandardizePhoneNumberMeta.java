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

package org.pentaho.di.trans.steps.standardize;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.injection.Injection;
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

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

/**
 * The step normalize phone number in a standardized and consistent manner.
 * 
 * @author Nicolas ADMENT
 *
 */
@Step(id = "StandardizePhoneNumber", image = "standardizephonenumber.svg", i18nPackageName = "org.pentaho.di.trans.steps.standardize", name = "StandardizePhoneNumber.Name", description = "StandardizePhoneNumber.Description", categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.DataQuality",
		documentationUrl = "https://github.com/nadment/pdi-standardize-plugin/wiki")
@InjectionSupported(localizationPrefix = "StandardizePhoneNumberMeta.Injection.", groups = { "FIELDS" })
public class StandardizePhoneNumberMeta extends BaseStepMeta implements StepMetaInterface {

	private static Class<?> PKG = StandardizePhoneNumberMeta.class; // for i18n
																	// purposes
	/**
	 * Constants:
	 */
	private static final String TAG_INPUT = "input"; //$NON-NLS-1$
	private static final String TAG_OUTPUT = "output"; //$NON-NLS-1$
	private static final String TAG_COUNTRY = "country"; //$NON-NLS-1$
	private static final String TAG_FORMAT = "format"; //$NON-NLS-1$
	private static final String TAG_DEFAULT_COUNTRY = "country"; //$NON-NLS-1$
	private static final String TAG_PHONE_NUMBER_TYPE = "phonenumbertype"; //$NON-NLS-1$
	private static final String TAG_IS_VALID_NUMBER = "isvalidphonenumber"; //$NON-NLS-1$

	private static final Set<PhoneNumberFormat> SUPPORTED_FORMATS = EnumSet.of(PhoneNumberFormat.E164,
			PhoneNumberFormat.INTERNATIONAL, PhoneNumberFormat.NATIONAL, PhoneNumberFormat.RFC3966);

	@Injection(name = "DEFAULT_COUNTRY")
	private String defaultCountry;

	/** The phone number to standardize */
	@InjectionDeep
	private List<StandardizePhoneNumber> standardizePhoneNumbers = new ArrayList<>();

	/**
	 * additional options
	 */

	public StandardizePhoneNumberMeta() {
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
		return new StandardizePhoneNumberStep(stepMeta, stepDataInterface, cnr, transMeta, disp);
	}

	/**
	 * Called by PDI to get a new instance of the step data class.
	 */
	@Override
	public StepDataInterface getStepData() {
		return new StandardizePhoneNumberData();
	}

	/**
	 * This method is called every time a new step is created and should
	 * allocate/set the step configuration to sensible defaults. The values set
	 * here will be used by Spoon when a new step is created.
	 */
	@Override
	public void setDefault() {

		standardizePhoneNumbers = new ArrayList<>();
		this.defaultCountry = "FR";
	}

	@Override
	public Object clone() {
		StandardizePhoneNumberMeta clone = (StandardizePhoneNumberMeta) super.clone();

		return clone;
	}

	@Override
	public String getXML() throws KettleValueException {

		StringBuilder xml = new StringBuilder(500);

		xml.append(XMLHandler.addTagValue(TAG_DEFAULT_COUNTRY, this.defaultCountry));

		xml.append("<fields>").append(Const.CR); //$NON-NLS-1$
		for (StandardizePhoneNumber standardize : this.getStandardizePhoneNumbers()) {
			xml.append("<field>").append(Const.CR); //$NON-NLS-1$
			xml.append(XMLHandler.addTagValue(TAG_INPUT, standardize.getInputField()));
			xml.append(XMLHandler.addTagValue(TAG_OUTPUT, standardize.getOutputField()));
			xml.append(XMLHandler.addTagValue(TAG_COUNTRY, standardize.getCountryField()));
			xml.append(XMLHandler.addTagValue(TAG_FORMAT, standardize.getFormat().name()));
			xml.append(XMLHandler.addTagValue(TAG_PHONE_NUMBER_TYPE, standardize.getPhoneNumberTypeField()));
			xml.append(XMLHandler.addTagValue(TAG_IS_VALID_NUMBER, standardize.getIsValidPhoneNumberField()));
			xml.append("</field>").append(Const.CR); //$NON-NLS-1$
		}
		xml.append("</fields>").append(Const.CR); //$NON-NLS-1$

		return xml.toString();
	}

	@Override
	public void loadXML(Node stepNode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {

		try {
			this.defaultCountry = XMLHandler.getTagValue(stepNode, TAG_DEFAULT_COUNTRY);

			Node fields = XMLHandler.getSubNode(stepNode, "fields");
			int count = XMLHandler.countNodes(fields, "field");
			standardizePhoneNumbers = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				Node field = XMLHandler.getSubNodeByNr(fields, "field", i);

				StandardizePhoneNumber standardize = new StandardizePhoneNumber();
				standardize.setInputField(Const.NVL(XMLHandler.getTagValue(field, TAG_INPUT), ""));
				standardize.setOutputField(Const.NVL(XMLHandler.getTagValue(field, TAG_OUTPUT), ""));
				standardize.setCountryField(Const.NVL(XMLHandler.getTagValue(field, TAG_COUNTRY), ""));
				standardize
						.setPhoneNumberTypeField(Const.NVL(XMLHandler.getTagValue(field, TAG_PHONE_NUMBER_TYPE), ""));
				standardize
						.setIsValidPhoneNumberField(Const.NVL(XMLHandler.getTagValue(field, TAG_IS_VALID_NUMBER), ""));

				try {
					String value = XMLHandler.getTagValue(field, TAG_FORMAT);
					PhoneNumberFormat format = PhoneNumberFormat.valueOf(value);
					standardize.setFormat(format);
				} catch (Exception e) {

				}

				standardizePhoneNumbers.add(standardize);
			}
		} catch (Exception e) {
			throw new KettleXMLException(
					BaseMessages.getString(PKG, "StandardizeMeta.Exception.UnableToReadStepInfoFromXML"), e); //$NON-NLS-1$
		}

	}

	@Override
	public void saveRep(Repository repository, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step)
			throws KettleException {
		try {

			for (int i = 0; i < this.standardizePhoneNumbers.size(); i++) {
				StandardizePhoneNumber standardize = standardizePhoneNumbers.get(i);
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_INPUT, standardize.getInputField());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_OUTPUT, standardize.getOutputField());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_COUNTRY, standardize.getCountryField());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_FORMAT, standardize.getFormat().name());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_PHONE_NUMBER_TYPE,
						standardize.getPhoneNumberTypeField());

				repository.saveStepAttribute(id_transformation, id_step, i, TAG_IS_VALID_NUMBER,
						standardize.getIsValidPhoneNumberField());
			}

			repository.saveStepAttribute(id_transformation, id_step, TAG_DEFAULT_COUNTRY, this.defaultCountry);
		} catch (Exception e) {
			throw new KettleException(
					BaseMessages.getString(PKG, "StandardizeMeta.Exception.UnableToSaveRepository", id_step), e); //$NON-NLS-1$
		}
	}

	@Override
	public void readRep(Repository repository, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases)
			throws KettleException {
		try {
			int count = repository.countNrStepAttributes(id_step, TAG_INPUT);
			standardizePhoneNumbers = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				StandardizePhoneNumber standardize = new StandardizePhoneNumber();
				standardize.setInputField(repository.getStepAttributeString(id_step, i, TAG_INPUT));
				standardize.setOutputField(repository.getStepAttributeString(id_step, i, TAG_OUTPUT));
				standardize.setCountryField(repository.getStepAttributeString(id_step, i, TAG_COUNTRY));
				standardize
						.setPhoneNumberTypeField(repository.getStepAttributeString(id_step, i, TAG_PHONE_NUMBER_TYPE));

				standardize
						.setIsValidPhoneNumberField(repository.getStepAttributeString(id_step, i, TAG_IS_VALID_NUMBER));

				String formatAttribute = repository.getStepAttributeString(id_step, i, TAG_FORMAT);
				if (formatAttribute != null) {
					standardize.setFormat(PhoneNumberFormat.valueOf(formatAttribute));
				}

				standardizePhoneNumbers.add(standardize);
			}

			this.defaultCountry = repository.getStepAttributeString(id_step, TAG_DEFAULT_COUNTRY);

		} catch (Exception e) {

			throw new KettleException(
					BaseMessages.getString(PKG, "StandardizeMeta.Exception.UnableToReadRepository", id_step), e); //$NON-NLS-1$
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
			// add the extra fields if specified
			for (StandardizePhoneNumber standardize : this.getStandardizePhoneNumbers()) {

				// add the output fields if specified
				int index = inputRowMeta.indexOfValue(standardize.getInputField());
				ValueMetaInterface vm = inputRowMeta.getValueMeta(index);
				if (!Utils.isEmpty(standardize.getOutputField())) {
					// created output field only if name changed
					if (!standardize.getOutputField().equals(standardize.getInputField())) {

						vm = ValueMetaFactory.createValueMeta(standardize.getOutputField(),
								ValueMetaInterface.TYPE_STRING);

						inputRowMeta.addValueMeta(vm);
					}
				}
				vm.setOrigin(stepName);

				// phone number type
				if (!Utils.isEmpty(standardize.getPhoneNumberTypeField())) {
					vm = ValueMetaFactory.createValueMeta(standardize.getPhoneNumberTypeField(),
							ValueMetaInterface.TYPE_STRING);
					vm.setOrigin(stepName);
					inputRowMeta.addValueMeta(vm);
				}

				if (!Utils.isEmpty(standardize.getIsValidPhoneNumberField())) {
					vm = ValueMetaFactory.createValueMeta(standardize.getIsValidPhoneNumberField(),
							ValueMetaInterface.TYPE_BOOLEAN);
					vm.setOrigin(stepName);
					inputRowMeta.addValueMeta(vm);
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
					"StandardizeMeta.CheckResult.ReceivingFieldsFromPreviousSteps", prev.size()), stepMeta)); //$NON-NLS-1$
		}

		// See if there are input streams leading to this step!
		if (input.length > 0) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_OK,
					BaseMessages.getString(PKG, "StandardizeMeta.CheckResult.ReceivingInfoFromOtherSteps"), stepMeta));
		} else {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR,
					BaseMessages.getString(PKG, "StandardizeMeta.CheckResult.NotReceivingInfoFromOtherSteps"),
					stepMeta));
		}

		for (StandardizePhoneNumber standardize : this.getStandardizePhoneNumbers()) {

			// Check only if input fields
			if (input.length > 0) {

				// See if there are missing input streams
				ValueMetaInterface vmi = prev.searchValueMeta(standardize.getInputField());
				if (vmi == null) {
					String message = BaseMessages.getString(PKG,
							"StandardizePhoneNumberMeta.CheckResult.MissingInputField", standardize.getInputField());
					remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, message, stepMeta));
				}

				// See if there are missing input streams
				vmi = prev.searchValueMeta(standardize.getCountryField());
				if (vmi == null) {
					String message = BaseMessages.getString(PKG,
							"StandardizePhoneNumberMeta.CheckResult.MissingCountryField",
							standardize.getCountryField());
					remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, message, stepMeta));
				}
			}

		}

	}

	public String[] getSupportedFormats() {

		ArrayList<String> result = new ArrayList<>();
		for (PhoneNumberFormat format : SUPPORTED_FORMATS) {
			result.add(format.name());
		}

		return result.toArray(new String[result.size()]);
	}

	public String[] getSupportedCountries() {

		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

		ArrayList<String> result = new ArrayList<>();
		for (String region : phoneUtil.getSupportedRegions()) {
			result.add(region);
		}

		return result.toArray(new String[result.size()]);
	}

	public List<StandardizePhoneNumber> getStandardizePhoneNumbers() {
		return this.standardizePhoneNumbers;
	}

	public void setStandardizePhoneNumbers(final List<StandardizePhoneNumber> standardizes) {
		this.standardizePhoneNumbers = standardizes;
	}

	/**
	 * Get the country codes (ISO 2)
	 * 
	 * @return
	 */
	public String getDefaultCountry() {
		return defaultCountry;
	}

	/**
	 * Set the country codes (ISO 2)
	 * 
	 * @param country
	 */
	public void setDefaultCountry(String country) {
		this.defaultCountry = country;
	}
}