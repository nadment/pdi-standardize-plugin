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
import java.util.List;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
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

/**
 * Make different URLs "equivalent" (i.e. eliminate URL variations pointing to
 * the same resource).
 * 
 * <p>
 * Several normalization methods implemented come from the
 * <a href="http://tools.ietf.org/html/rfc3986">RFC 3986</a> standard. These
 * standards and several more normalization techniques are very well summarized
 * on the Wikipedia article titled
 * <i><a href="http://en.wikipedia.org/wiki/URL_normalization"> URL
 * Normalization</a></i>.
 * </p>
 * 
 * @author Nicolas ADMENT
 *
 */
@Step(id = "StandardizeUrl", image = "standardizeurl.svg", i18nPackageName = "org.pentaho.di.trans.steps.standardize", name = "StandardizeUrl.Name", description = "StandardizeUrl.Description", categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.DataQuality", documentationUrl = "https://github.com/nadment/pdi-standardize-plugin/wiki")
@InjectionSupported(localizationPrefix = "StandardizeUrlMeta.Injection.", groups = { "FIELDS" })
public class StandardizeUrlMeta extends BaseStepMeta implements StepMetaInterface {

	private static Class<?> PKG = StandardizeUrlMeta.class; // for i18n purposes

	/**
	 * Constants:
	 */
	private static final String TAG_INPUT_FIELD = "input_field"; //$NON-NLS-1$
	private static final String TAG_OUTPUT_FIELD = "output_field"; //$NON-NLS-1$
	private static final String TAG_UNSHORTEN = "unshorten"; //$NON-NLS-1$
	private static final String TAG_REPLACE_IP_WITH_DOMAIN_NAME = "replace_ip"; //$NON-NLS-1$
	private static final String TAG_SORT_QUERY_PARAMETERS = "sort_query_parameters"; //$NON-NLS-1$
	private static final String TAG_REMOVE_WWW = "remove_www"; //$NON-NLS-1$
	private static final String TAG_REMOVE_DEFAULT_PORT = "remove_default_port"; //$NON-NLS-1$
	private static final String TAG_REMOVE_DIRECTORY_INDEX = "remove_directory_index"; //$NON-NLS-1$
	private static final String TAG_REMOVE_DOT_SEGMENTS = "remove_dot_seglents"; //$NON-NLS-1$
	private static final String TAG_REMOVE_DUPLICATE_SLASHES = "remove_duplicate_slashes"; //$NON-NLS-1$
	private static final String TAG_REMOVE_TRAILING_SLASH = "remove_trailing_slash"; //$NON-NLS-1$
	private static final String TAG_REMOVE_FRAGMENT = "remove_fragment"; //$NON-NLS-1$
	private static final String TAG_REMOVE_SESSION_ID = "remove_session_id"; //$NON-NLS-1$

	/** The urls to standardize */
	@InjectionDeep
	private List<StandardizeUrl> standardizes = new ArrayList<>();

	@Injection(name = "UNSHORTEN") //$NON-NLS-1$
	private boolean unshorten;
	@Injection(name = "REPLACE_IP_WITH_DOMAIN_NAME") //$NON-NLS-1$
	private boolean replaceIPWithDomainName;
	@Injection(name = "SORT_QUERY_PARAMETERS") //$NON-NLS-1$
	private boolean sortQueryParameters;
	@Injection(name = "REMOVE_WWW") //$NON-NLS-1$
	private boolean removeWWW;
	@Injection(name = "REMOVE_DEFAULT_PORT") //$NON-NLS-1$
	private boolean removeDefaultPort;
	@Injection(name = "REMOVE_DOT_SEGMENTS") //$NON-NLS-1$
	private boolean removeDotSegments;
	@Injection(name = "REMOVE_DIRECTORY_INDEX") //$NON-NLS-1$
	private boolean removeDirectoryIndex;
	@Injection(name = "REMOVE_FRAGMENT") //$NON-NLS-1$
	private boolean removeFragment;
	@Injection(name = "REMOVE_DUPLICATE_SLASHES") //$NON-NLS-1$
	private boolean removeDuplicateSlashes;
	@Injection(name = "REMOVE_TRAILING_SLASH") //$NON-NLS-1$
	private boolean removeTrailingSlash;
	@Injection(name = "REMOVE_SESSION_ID") //$NON-NLS-1$
	private boolean removeSessionId;

	public StandardizeUrlMeta() {
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
		return new StandardizeUrlStep(stepMeta, stepDataInterface, cnr, transMeta, disp);
	}

	/**
	 * Called by PDI to get a new instance of the step data class.
	 */
	@Override
	public StepDataInterface getStepData() {
		return new StandardizeUrlData();
	}

	/**
	 * This method is called every time a new step is created and should
	 * allocate/set the step configuration to sensible defaults. The values set
	 * here will be used by Spoon when a new step is created.
	 */
	@Override
	public void setDefault() {
		this.removeDefaultPort = true;
	}

	@Override
	public Object clone() {
		StandardizeUrlMeta clone = (StandardizeUrlMeta) super.clone();

		return clone;
	}

	@Override
	public String getXML() throws KettleValueException {

		StringBuilder xml = new StringBuilder(500);

		xml.append(XMLHandler.addTagValue(TAG_UNSHORTEN, this.isUnshorten()));
		xml.append(XMLHandler.addTagValue(TAG_REPLACE_IP_WITH_DOMAIN_NAME, this.isReplaceIPWithDomainName()));
		xml.append(XMLHandler.addTagValue(TAG_SORT_QUERY_PARAMETERS, this.isSortQueryParameters()));
		xml.append(XMLHandler.addTagValue(TAG_REMOVE_DEFAULT_PORT, this.isRemoveDefaultPort()));
		xml.append(XMLHandler.addTagValue(TAG_REMOVE_FRAGMENT, this.isRemoveFragment()));
		xml.append(XMLHandler.addTagValue(TAG_REMOVE_DUPLICATE_SLASHES, this.isRemoveDuplicateSlashes()));
		xml.append(XMLHandler.addTagValue(TAG_REMOVE_DOT_SEGMENTS, this.isRemoveDotSegments()));
		xml.append(XMLHandler.addTagValue(TAG_REMOVE_WWW, this.isRemoveWWW()));
		xml.append(XMLHandler.addTagValue(TAG_REMOVE_SESSION_ID, this.isRemoveSessionId()));
		xml.append(XMLHandler.addTagValue(TAG_REMOVE_DIRECTORY_INDEX, this.isRemoveDirectoryIndex()));
		xml.append(XMLHandler.addTagValue(TAG_REMOVE_TRAILING_SLASH, this.isRemoveTrailingSlash()));

		xml.append("<fields>");
		for (StandardizeUrl standardize : this.getStandardizeUrls()) {
			xml.append("<field>");
			xml.append(XMLHandler.addTagValue(TAG_INPUT_FIELD, standardize.getInputField()));
			xml.append(XMLHandler.addTagValue(TAG_OUTPUT_FIELD, standardize.getOutputField()));
			xml.append("</field>");
		}
		xml.append("</fields>");

		return xml.toString();
	}

	@Override
	public void loadXML(Node stepNode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {

		try {
			this.setUnshorten("Y".equalsIgnoreCase(XMLHandler.getTagValue(stepNode, TAG_UNSHORTEN)));
			this.setReplaceIPWithDomainName(
					"Y".equalsIgnoreCase(XMLHandler.getTagValue(stepNode, TAG_REPLACE_IP_WITH_DOMAIN_NAME)));
			this.setSortQueryParameters(
					"Y".equalsIgnoreCase(XMLHandler.getTagValue(stepNode, TAG_SORT_QUERY_PARAMETERS)));
			this.setRemoveWWW("Y".equalsIgnoreCase(XMLHandler.getTagValue(stepNode, TAG_REMOVE_WWW)));
			this.setRemoveDotSegments("Y".equalsIgnoreCase(XMLHandler.getTagValue(stepNode, TAG_REMOVE_DOT_SEGMENTS)));
			this.setRemoveFragment("Y".equalsIgnoreCase(XMLHandler.getTagValue(stepNode, TAG_REMOVE_FRAGMENT)));
			this.setRemoveDuplicateSlashes(
					"Y".equalsIgnoreCase(XMLHandler.getTagValue(stepNode, TAG_REMOVE_DUPLICATE_SLASHES)));
			this.setRemoveSessionId("Y".equalsIgnoreCase(XMLHandler.getTagValue(stepNode, TAG_REMOVE_SESSION_ID)));
			this.setRemoveDirectoryIndex(
					"Y".equalsIgnoreCase(XMLHandler.getTagValue(stepNode, TAG_REMOVE_DIRECTORY_INDEX)));
			this.setRemoveTrailingSlash(
					"Y".equalsIgnoreCase(XMLHandler.getTagValue(stepNode, TAG_REMOVE_TRAILING_SLASH)));
			this.setRemoveDefaultPort("Y".equalsIgnoreCase(XMLHandler.getTagValue(stepNode, TAG_REMOVE_DEFAULT_PORT)));

			Node fields = XMLHandler.getSubNode(stepNode, "fields");
			int count = XMLHandler.countNodes(fields, "field");
			standardizes = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				Node field = XMLHandler.getSubNodeByNr(fields, "field", i);

				StandardizeUrl standardize = new StandardizeUrl();
				standardize.setInputField(XMLHandler.getTagValue(field, TAG_INPUT_FIELD));
				standardize.setOutputField(XMLHandler.getTagValue(field, TAG_OUTPUT_FIELD));
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
			repository.saveStepAttribute(id_transformation, id_step, TAG_UNSHORTEN, this.isUnshorten());
			repository.saveStepAttribute(id_transformation, id_step, TAG_REPLACE_IP_WITH_DOMAIN_NAME,
					this.isReplaceIPWithDomainName());
			repository.saveStepAttribute(id_transformation, id_step, TAG_SORT_QUERY_PARAMETERS,
					this.isSortQueryParameters());
			repository.saveStepAttribute(id_transformation, id_step, TAG_REMOVE_WWW, this.isRemoveWWW());
			repository.saveStepAttribute(id_transformation, id_step, TAG_REMOVE_FRAGMENT, this.isRemoveFragment());
			repository.saveStepAttribute(id_transformation, id_step, TAG_REMOVE_DOT_SEGMENTS,
					this.isRemoveDotSegments());
			repository.saveStepAttribute(id_transformation, id_step, TAG_REMOVE_DUPLICATE_SLASHES,
					this.isRemoveDuplicateSlashes());
			repository.saveStepAttribute(id_transformation, id_step, TAG_REMOVE_SESSION_ID, this.isRemoveSessionId());
			repository.saveStepAttribute(id_transformation, id_step, TAG_REMOVE_DIRECTORY_INDEX,
					this.isRemoveDirectoryIndex());
			repository.saveStepAttribute(id_transformation, id_step, TAG_REMOVE_TRAILING_SLASH,
					this.isRemoveTrailingSlash());
			repository.saveStepAttribute(id_transformation, id_step, TAG_REMOVE_DEFAULT_PORT,
					this.isRemoveDefaultPort());

			for (int i = 0; i < this.standardizes.size(); i++) {
				StandardizeUrl standardize = standardizes.get(i);
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_INPUT_FIELD,
						standardize.getInputField());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_OUTPUT_FIELD,
						standardize.getOutputField());
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
			this.setUnshorten(repository.getStepAttributeBoolean(id_step, TAG_UNSHORTEN));
			this.setReplaceIPWithDomainName(
					repository.getStepAttributeBoolean(id_step, TAG_REPLACE_IP_WITH_DOMAIN_NAME));
			this.setSortQueryParameters(repository.getStepAttributeBoolean(id_step, TAG_SORT_QUERY_PARAMETERS));
			this.setRemoveWWW(repository.getStepAttributeBoolean(id_step, TAG_REMOVE_WWW));
			this.setRemoveFragment(repository.getStepAttributeBoolean(id_step, TAG_REMOVE_FRAGMENT));
			this.setRemoveDotSegments(repository.getStepAttributeBoolean(id_step, TAG_REMOVE_DOT_SEGMENTS));
			this.setRemoveDuplicateSlashes(repository.getStepAttributeBoolean(id_step, TAG_REMOVE_DUPLICATE_SLASHES));
			this.setRemoveSessionId(repository.getStepAttributeBoolean(id_step, TAG_REMOVE_SESSION_ID));
			this.setRemoveDirectoryIndex(repository.getStepAttributeBoolean(id_step, TAG_REMOVE_DIRECTORY_INDEX));
			this.setRemoveTrailingSlash(repository.getStepAttributeBoolean(id_step, TAG_REMOVE_TRAILING_SLASH));
			this.setRemoveDefaultPort(repository.getStepAttributeBoolean(id_step, TAG_REMOVE_DEFAULT_PORT));

			int count = repository.countNrStepAttributes(id_step, TAG_INPUT_FIELD);
			standardizes = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				StandardizeUrl standardize = new StandardizeUrl();
				standardize.setInputField(repository.getStepAttributeString(id_step, i, TAG_INPUT_FIELD));
				standardize.setOutputField(repository.getStepAttributeString(id_step, i, TAG_OUTPUT_FIELD));
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
			// add the extra fields if specified
			for (StandardizeUrl standardize : this.getStandardizeUrls()) {

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
			for (StandardizeUrl standardize : this.getStandardizeUrls()) {

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

	public List<StandardizeUrl> getStandardizeUrls() {
		return this.standardizes;
	}

	public void setStandardizeUrls(final List<StandardizeUrl> standardizes) {
		this.standardizes = standardizes;
	}

	public boolean isUnshorten() {
		return unshorten;
	}

	public void setUnshorten(boolean unshorten) {
		this.unshorten = unshorten;
	}

	public boolean isReplaceIPWithDomainName() {
		return replaceIPWithDomainName;
	}

	public void setReplaceIPWithDomainName(boolean replaceIP) {
		this.replaceIPWithDomainName = replaceIP;
	}

	public boolean isSortQueryParameters() {
		return sortQueryParameters;
	}

	public void setSortQueryParameters(boolean sortParamters) {
		this.sortQueryParameters = sortParamters;
	}

	public boolean isRemoveFragment() {
		return removeFragment;
	}

	public void setRemoveFragment(boolean removeFragment) {
		this.removeFragment = removeFragment;
	}

	public boolean isRemoveDuplicateSlashes() {
		return removeDuplicateSlashes;
	}

	public void setRemoveDuplicateSlashes(boolean removeDuplicateSlashes) {
		this.removeDuplicateSlashes = removeDuplicateSlashes;
	}

	public boolean isRemoveWWW() {
		return removeWWW;
	}

	public void setRemoveWWW(boolean removeWWW) {
		this.removeWWW = removeWWW;
	}

	public boolean isRemoveDotSegments() {
		return removeDotSegments;
	}

	public void setRemoveDotSegments(boolean removeDotSegments) {
		this.removeDotSegments = removeDotSegments;
	}

	public boolean isRemoveSessionId() {
		return removeSessionId;
	}

	public void setRemoveSessionId(boolean removeSessionId) {
		this.removeSessionId = removeSessionId;
	}

	public boolean isRemoveDirectoryIndex() {
		return removeDirectoryIndex;
	}

	public void setRemoveDirectoryIndex(boolean removeDirectoryIndex) {
		this.removeDirectoryIndex = removeDirectoryIndex;
	}

	public boolean isRemoveTrailingSlash() {
		return removeTrailingSlash;
	}

	public void setRemoveTrailingSlash(boolean removeTrailingSlash) {
		this.removeTrailingSlash = removeTrailingSlash;
	}

	public boolean isRemoveDefaultPort() {
		return removeDefaultPort;
	}

	public void setRemoveDefaultPort(boolean removeDefaultPort) {
		this.removeDefaultPort = removeDefaultPort;
	}

}