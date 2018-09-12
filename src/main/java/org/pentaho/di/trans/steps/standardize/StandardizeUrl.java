package org.pentaho.di.trans.steps.standardize;

import org.pentaho.di.core.injection.Injection;



/**
 * Contains the properties of the fields to standardize.
 *
 * @author Nicolas ADMENT
 */
public class StandardizeUrl implements Cloneable {




	/** The target field name */
	@Injection(name = "INPUT_FIELD")
	private String field;

	@Injection(name = "NAME")
	private String name;

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
		return field;
	}

	public void setInputField(final String name) {
		this.field = name;
	}

	public String getOutputField() {
		return name;
	}

	public void setOutputField(final String name) {
		this.name = name;
	}


	@Override
	public String toString() {
		return field;
	}
}
