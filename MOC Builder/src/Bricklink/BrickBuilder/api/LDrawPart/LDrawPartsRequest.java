/*
 * GPLv3
 */

package Bricklink.BrickBuilder.api.LDrawPart;

import java.util.ArrayList;
import java.util.List;

import Bricklink.org.kleini.bricklink.api.HttpRequestT;
import Bricklink.org.kleini.bricklink.api.Parameter;
import Bricklink.org.kleini.bricklink.api.Request;
import Bricklink.org.kleini.bricklink.data.CurrencyT;
import Bricklink.org.kleini.bricklink.data.ItemType;
import Exports.PartDomainT;

/**
 * {@link LDrawPartsRequest}
 * 
 * @author <a href="mailto:himself@kleini.org">Marcus Klein</a>
 */
public final class LDrawPartsRequest implements
		Request<LDrawPartsResponse> {
	private boolean composeUncertified = false;

	public LDrawPartsRequest() {
		super();
	}

	public LDrawPartsRequest(boolean compoaseUncertified) {
		super();
		this.composeUncertified = compoaseUncertified;
	}

	@Override
	public String getPath() {
		return "/parts";
	}

	@Override
	public Parameter[] getParameters() {
		List<Parameter> retval = new ArrayList<Parameter>();
		retval.add(new Parameter("composeuncertified",
				this.composeUncertified ? "1" : "0"));
		return retval.toArray(new Parameter[retval.size()]);
	}

	@Override
	public LDrawPartsParser getParser() {
		return new LDrawPartsParser();
	}

	@Override
	public HttpRequestT getRequestType() {
		// TODO Auto-generated method stub
		return HttpRequestT.GET;
	}
}
