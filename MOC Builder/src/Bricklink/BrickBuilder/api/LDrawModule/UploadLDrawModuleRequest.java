/*
 * GPLv3
 */

package Bricklink.BrickBuilder.api.LDrawModule;

import java.util.ArrayList;
import java.util.List;

import Bricklink.BricklinkAPI;
import Bricklink.BrickBuilder.api.BrickBuilderClient;
import Bricklink.BrickBuilder.api.Connectivity.ConnectivitiesRequest;
import Bricklink.BrickBuilder.data.SubpartDT;
import Bricklink.org.kleini.bricklink.api.HttpRequestT;
import Bricklink.org.kleini.bricklink.api.Parameter;
import Bricklink.org.kleini.bricklink.api.Request;
import Bricklink.org.kleini.bricklink.api.Response;

/**
 * {@link UploadLDrawModuleRequest}
 * 
 * @author <a href="mailto:himself@kleini.org">Marcus Klein</a>
 */
public final class UploadLDrawModuleRequest implements
		Request<UploadLDrawModuleResponse> {
	
	public static void main(String args[]) throws Exception{
		BrickBuilderClient client = BricklinkAPI.getInstance().getBrickBuilderClient();
		List<String>tags = new ArrayList<String>();
		tags.add("truck");
		tags.add("body");
		List<SubpartDT> subparts = new ArrayList<SubpartDT>();
		subparts.add(new SubpartDT("3005", 1, 1));
		Request request = new UploadLDrawModuleRequest("TruckBody", "funface2", tags, subparts, "j:/untitled.ldr");
		Response response = client.execute(request);
	}
	private String moduleName;
	private String author;
	private List<String> tags;
	private List<SubpartDT> subparts;
	private String filePath;	

	public UploadLDrawModuleRequest(String moduleName, String author,
			List<String> tags, List<SubpartDT> subparts, String filePath) {
		super();
		this.moduleName = moduleName;
		this.author = author;
		this.tags = tags;
		this.filePath = filePath;
		this.subparts = subparts;
	}

	@Override
	public String getPath() {
		return "/modules";
	}

	@Override
	public Parameter[] getParameters() {
		List<Parameter> retval = new ArrayList<Parameter>();
		retval.add(new Parameter("file", filePath));
		retval.add(new Parameter("moduleName", moduleName));
		retval.add(new Parameter("author", author));
		retval.add(new Parameter("tags", tags));
		retval.add(new Parameter("subparts", subparts));
		return retval.toArray(new Parameter[retval.size()]);
	}

	@Override
	public UploadLDrawModuleParser getParser() {
		return new UploadLDrawModuleParser();
	}

	@Override
	public HttpRequestT getRequestType() {
		// TODO Auto-generated method stub
		return HttpRequestT.POST;
	}
}
