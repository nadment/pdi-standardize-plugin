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

/**
 * Contains the properties of the fields to standardize.
 *
 * @author Nicolas ADMENT
 */
public class StandardizeEmailAddress implements Cloneable {

	public StandardizeEmailAddress() {
		super();
	}

	/** The target field name */
	@Injection(name = "INPUT_FIELD", group = "FIELDS")
	private String inputField = null;

	@Injection(name = "OUTPUT_FIELD", group = "FIELDS")
	private String outputField = null;
	
	@Injection(name = "VALID_FIELD", group = "FIELDS")
	private String validField= null;

	@Override
	public Object clone() {
		StandardizeEmailAddress clone;
		try {
			clone = (StandardizeEmailAddress) super.clone();

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

	public String getValidField() {
		return validField;
	}

	public void setValidField(final String field) {
		this.validField = StringUtils.stripToNull(field);
	}
}
