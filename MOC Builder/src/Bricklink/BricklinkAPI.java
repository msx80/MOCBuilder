package Bricklink;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import Bricklink.BrickBuilder.api.BrickBuilderClient;
import Bricklink.BrickBuilder.api.Connectivity.ConnectivitiesRequest;
import Bricklink.BrickBuilder.api.Connectivity.UploadConnectivityRequest;
import Bricklink.org.kleini.bricklink.api.BrickLinkClient;
import Bricklink.org.kleini.bricklink.api.Request;
import Bricklink.org.kleini.bricklink.api.Response;

public class BricklinkAPI {

	private String consumerKey = "7F6C5B4041BB447F9BDB8A2924638576";
	private String consumerSecret = "5CE136DB3BD34E6BA75A3E3D69FD0956";
	private String tokenValue = "F95D308FD7624B66B944B851027D6A86";
	private String tokenSecret = "6065F1052CA84BF89DE68C2F1B7B6644";

	private static BricklinkAPI _instance = null;
	private BrickLinkClient bricklinkClientForOpenAPI = null;
	private BrickBuilderClient brickBuilderClient = null;

	public static void main(String args[]) throws Exception {
		BrickBuilderClient client = BricklinkAPI.getInstance().getBrickBuilderClient();
		Request request = new ConnectivitiesRequest(false);
		Response response = client.execute(request);
		
//		System.out.println(response.getMappingList().size());
//		for(IDMappingDT dt : response.getMappingList())
//			System.out.println(dt.getBLItemNo());
		
//		BrickLinkClient client = BricklinkAPI.getInstance()
//				.getClientForOpenAPI();
//
//		KnownColorsRequest request = new KnownColorsRequest(ItemType.PART,
//				"3005");
//		KnownColorsResponse response = client.execute(request);
//		for (KnownColorDT knownColorDT : response.getKnownColors())
//			System.out.println(knownColorDT.getIdentifier());
	}

	private BricklinkAPI() {
		try {
			bricklinkClientForOpenAPI = new BrickLinkClient(consumerKey,
					consumerSecret, tokenValue, tokenSecret);
			brickBuilderClient = new BrickBuilderClient();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized static BricklinkAPI getInstance() {
		if (_instance == null)
			_instance = new BricklinkAPI();
		return _instance;
	}

	public BrickLinkClient getClientForOpenAPI() {
		return bricklinkClientForOpenAPI;
	}

	public BrickBuilderClient getBrickBuilderClient() {
		return brickBuilderClient;
	}
}
