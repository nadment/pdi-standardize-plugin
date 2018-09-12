package org.pentaho.di.trans.steps.standardize;

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
	@Injection(name = "INPUT", group = "FIELDS")
	private String field;

	@Injection(name = "OUTPUT", group = "FIELDS")
	private String name;

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
		return field;
	}

	public void setInputField(final String name) {
		this.field = name;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return field;
	}
}
