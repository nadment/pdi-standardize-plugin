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


package org.kettle.trans.steps.standardize;

import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.injection.Injection;

/**
 * Contains the properties of the fields to standardize.
 *
 * @author Nicolas ADMENT
 */
public class StandardizeUrl implements Cloneable {

	/** The target field name */
	@Injection(name = "INPUT_FIELD", group = "FIELDS")
	private String input_field = null;

	@Injection(name = "OUTPUT_FIELD", group = "FIELDS")
	private String output_field = null;

	public StandardizeUrl() {
		super();
	}

	@Override
	public Object clone() {
		StandardizeUrl clone;
		try {
			clone = (StandardizeUrl) super.clone();

		} catch (CloneNotSupportedException e) {
			return null;
		}
		return clone;
	}

	public String getInputField() {
		return input_field;
	}

	public void setInputField(final String field) {		
		this.input_field =  StringUtils.stripToNull(field);
	}

	public String getOutputField() {
		return output_field;
	}

	public void setOutputField(final String field) {
		this.output_field =  StringUtils.stripToNull(field);
	}
}
