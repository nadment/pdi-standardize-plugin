package org.pentaho.di.trans.steps.standardize;

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
	private String inputField;

	/** The target field name */
	@Injection(name = "OUTPUT_FIELD", group = "FIELDS")
	private String outputField;

	/** The country field name */
	@Injection(name = "COUNTRY", group = "FIELDS")
	private String countryField;

	@Injection(name = "PHONE_NUMBER_TYPE_FIELD", group = "FIELDS")
	private String phoneNumberTypeField;

	@Injection(name = "IS_VALID_PHONE_NUMBER_FIELD", group = "FIELDS")
	private String isValidNumberField;

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
		this.inputField = field;
	}

	public String getOutputField() {
		return outputField;
	}

	public void setOutputField(final String field) {
		this.outputField = field;
	}

	public String getCountryField() {
		return countryField;
	}

	public void setCountryField(String field) {
		this.countryField = field;
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

	@Override
	public String toString() {
		return inputField;
	}

	public String getPhoneNumberTypeField() {
		return phoneNumberTypeField;
	}

	public void setPhoneNumberTypeField(String phoneNumberTypeField) {
		this.phoneNumberTypeField = phoneNumberTypeField;
	}

	public String getIsValidPhoneNumberField() {
		return isValidNumberField;
	}

	public void setIsValidPhoneNumberField(String isValidPhoneNumberField) {
		this.isValidNumberField = isValidPhoneNumberField;
	}
}
