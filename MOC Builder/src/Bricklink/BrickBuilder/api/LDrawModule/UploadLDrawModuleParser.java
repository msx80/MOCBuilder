/*
 * GPLv3
 */

package Bricklink.BrickBuilder.api.LDrawModule;

import org.codehaus.jackson.type.TypeReference;

import Bricklink.BrickBuilder.data.LDrawModuleDT;
import Bricklink.BrickBuilder.data.LDrawPartDT;
import Bricklink.org.kleini.bricklink.api.Parser;
import Bricklink.org.kleini.bricklink.data.ResponseDT;

/**
 * {@link UploadLDrawModuleParser}
 * 
 * @author <a href="mailto:himself@kleini.org">Marcus Klein</a>
 */
public final class UploadLDrawModuleParser extends
		Parser<UploadLDrawModuleResponse, LDrawModuleDT> {

	public UploadLDrawModuleParser() {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	protected TypeReference<ResponseDT<LDrawModuleDT>> getResponseType() {
		return new TypeReference<ResponseDT<LDrawModuleDT>>() {
			// Nothing to do.
		};
	}

	@Override
	protected UploadLDrawModuleResponse createResponse(
			ResponseDT<LDrawModuleDT> response) {
		return new UploadLDrawModuleResponse(response);
	}
}
