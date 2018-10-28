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

import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.injection.Injection;

import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

/**
 * Contains the properties of the fields to standardize.
 *
 * @author Nicolas ADMENT
 */
public class StandardizePhoneNumber implements Cloneable {

	/** The input field name */
	@Injection(name = "INPUT_FIELD", group = "FIELDS")
	private String inputField = null;

	/** The target field name */
	@Injection(name = "OUTPUT_FIELD", group = "FIELDS")
	private String outputField = null;

	/** The country field name */
	@Injection(name = "COUNTRY_FIELD", group = "FIELDS")
	private String countryField = null;

	@Injection(name = "TYPE_FIELD", group = "FIELDS")
	private String typeField;

	@Injection(name = "IS_VALID_FIELD", group = "FIELDS")
	private String isValidNumberField= null;

	/** The format */
	@Injection(name = "FORMAT", group = "FIELDS")
	private PhoneNumberFormat format = PhoneNumberFormat.E164;

	public StandardizePhoneNumber() {
		super();		
	}

	@Override
	public Object clone() {
		StandardizePhoneNumber clone;
		try {
			clone = (StandardizePhoneNumber) super.clone();

		} catch (CloneNotSupportedException e) {
			return null;
		}
		return clone;
	}

	public String getInputField() {
		return inputField;
	}

	public void setInputField(final String field) {		
		this.inputField = StringUtils.stripToNull(field);
	}

	public String getOutputField() {
		return outputField;
	}

	public void setOutputField(final String field) {	
		this.outputField = StringUtils.stripToNull(field);
	}

	public String getCountryField() {
		return countryField;
	}

	public void setCountryField(final String field) {
		this.countryField = StringUtils.stripToNull(field);
	}

	public PhoneNumberFormat getFormat() {
		return format;
	}

	public void setFormat(final PhoneNumberFormat param) {
		if (param == null)
			this.format = PhoneNumberFormat.E164;
		else
			this.format = param;
	}



	public String getPhoneNumberTypeField() {
		return typeField;
	}

	public void setPhoneNumberTypeField(final String phoneNumberTypeField) {
		this.typeField = StringUtils.stripToNull(phoneNumberTypeField);
	}

	public String getIsValidPhoneNumberField() {
		return isValidNumberField;
	}

	public void setIsValidPhoneNumberField(final String field) {		
		this.isValidNumberField = StringUtils.stripToNull(field);
	}
}
