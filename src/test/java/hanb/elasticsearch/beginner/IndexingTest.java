package hanb.elasticsearch.beginner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;
import java.util.List;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.indices.optimize.OptimizeRequest;
import org.elasticsearch.action.admin.indices.optimize.OptimizeResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.broadcast.BroadcastOperationThreading;
import org.elasticsearch.action.support.replication.ReplicationType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexingTest {
	private static final Logger log = LoggerFactory.getLogger(IndexingTest.class);
/*
	카테고리
		TV	1
			LED TV	1_1
				삼성	1_1_1
				LG	1_1_2
				기타	1_1_3
			3D TV	1_2
				삼성	1_2_1
				LG	1_2_2
				기타	1_2_3
		DSLR	2
			DSLR 카메라	2_1
				캐논		2_1_1
				니콘		2_1_2
				기타		2_1_3
			DSLR 렌즈		2_2
				캐논		2_2_1
				니콘		2_2_2
				기타		2_2_3

*/
	@Test
//	@Ignore
	public void addIndexing() throws Exception {
		Settings settings;
		Client client;
		
		settings = ImmutableSettings
				.settingsBuilder()
				.build();
		
		client = buildClient(settings);

		XContentBuilder builder = XContentFactory.jsonBuilder()
			    .startObject()
			        .field("user", "kimchy")
			        .field("postDate", new Date())
			        .field("message", "trying out Elasticsearch")
			    .endObject();
		String json = builder.string();
		System.out.println("json:" + json);
		
		IndexResponse response = client.prepareIndex("twitter", "tweet", "1")
		        .setSource(builder)
		        .execute()
		        .actionGet();
		
		// Index name
		String _index = response.getIndex();
		// Type name
		String _type = response.getType();
		// Document ID (generated or not)
		String _id = response.getId();
		// Version (if it's the first time you index this document, you will get: 1)
		long _version = response.getVersion();
	}
	
//	@Test
	@Ignore
	public void bulkIndexing() throws Exception {
		Settings settings;
		Client client;
		String setting = "";
		String mapping = "";
		BulkRequestBuilder bulkRequest;
		BulkResponse bulkResponse;
		
		settings = ImmutableSettings
				.settingsBuilder()
				.build();
		
		client = buildClient(settings);
		
		BufferedReader br = new BufferedReader(new FileReader("schema/market.row.json"));
		int id = 1;
		
		buildBeforeConfigApply(client, "open_market");
		
	    try {
	        String doc = "";
	        bulkRequest = client.prepareBulk();

	        while ((doc = br.readLine()) != null) {
	        	bulkRequest.add (
						client
							.prepareIndex("open_market", "market")
							.setOperationThreaded(false)
							.setSource(doc)
							.setReplicationType(ReplicationType.ASYNC)
							.setConsistencyLevel(WriteConsistencyLevel.QUORUM)
							.setRefresh(false)
					);
	        	
	        	if ( (id%5) == 0 ) {
	        		bulkResponse = bulkRequest.execute().actionGet();
	        		log.debug("{}", bulkResponse.getTookInMillis());
	        		bulkRequest = client.prepareBulk();
	        	}
	        	
	            id++;
	        }
	        
	        bulkResponse = bulkRequest.execute().actionGet();
	        log.debug("{}", bulkResponse.getTookInMillis());
	    } finally {
	        br.close();
	    }
	    
	    buildAfterConfigApply(client, "open_market");
	    buildOptimizeApply(client, "open_market");
	    
	    client.close();
	    
	}
	
	public void buildOptimizeApply(Client client, String indice) {
		OptimizeResponse response = client.admin().indices()
			.optimize(
					new OptimizeRequest()
						.indices(indice)
						.flush(true)
						.onlyExpungeDeletes(false)
						.waitForMerge(true)
						.operationThreading(BroadcastOperationThreading.THREAD_PER_SHARD)
						.maxNumSegments(1))
			.actionGet();
	}
	
	public void buildBeforeConfigApply(Client client, String indice) {
		String bulkIndexSettings = "{\"index\" : {\"number_of_replicas\" : 0, \"refresh_interval\" : \"-1\", \"merge\" : { \"policy\" : { \"max_merge_at_once\" : 10, \"segments_per_tier\" : 30 } }} }";
		client.admin().indices().prepareUpdateSettings(indice).setSettings(bulkIndexSettings).execute().actionGet();
	}
	
	public void buildAfterConfigApply(Client client, String indice) {
		String bulkIndexSettings = "{\"index\" : {\"number_of_replicas\" : 0, \"refresh_interval\" : \"1s\", \"merge\" : { \"policy\" : { \"max_merge_at_once\" : 5, \"segments_per_tier\" : 15 } }} }";
		client.admin().indices().prepareUpdateSettings(indice).setSettings(bulkIndexSettings).execute().actionGet();
	}
	
	/**
	 * file reader
	 * 
	 * @param file		input file path
	 * @return
	 * @throws Exception
	 */
	protected String readTextFile(String file) throws Exception {
		String ret = "";
		BufferedReader br = new BufferedReader(new FileReader(file));
		
	    try {
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();

	        while (line != null) {
	            sb.append(line);
	            line = br.readLine();
	        }
	        
	        ret = sb.toString();
	    } finally {
	        br.close();
	    }
	    
	    return ret;
	}
	
	/**
     * Reference : https://github.com/dadoonet/spring-elasticsearch
     * 
     * elasticsearch client TransportAddress 등록.<br>
     * 	cluster.node.list<br>
     * 
     * @param settings		client settings 정보.
     * @throws Exception
     */
	protected Client buildClient(Settings settings) throws Exception {
//		TransportClient client = new TransportClient(settings);
//		String nodes = "localhost:9300,localhost:9301,localhost:9302";
//		String[] nodeList = nodes.split(",");
//		int nodeSize = nodeList.length;
//
//		for (int i = 0; i < nodeSize; i++) {
//			client.addTransportAddress(toAddress(nodeList[i]));
//		}
		
		Settings settings2 = ImmutableSettings.settingsBuilder()
		        .put("cluster.name", "myClusterName").build();
		Client client = new TransportClient(settings2)
        .addTransportAddress(new InetSocketTransportAddress("localhost", 9300))
        .addTransportAddress(new InetSocketTransportAddress("localhost", 9301))
        .addTransportAddress(new InetSocketTransportAddress("localhost", 9302));

		return client;
	}
	
	/**
     * Reference : https://github.com/dadoonet/spring-elasticsearch
     * 
     * InetSocketTransportAddress 등록.<br>
     * 
     * @param address		node 들의 ip:port 정보.
     * @return InetSocketTransportAddress
     * @throws Exception
     */
	private InetSocketTransportAddress toAddress(String address) {
		if (address == null) return null;
		
		String[] splitted = address.split(":");
		int port = 9300;
		
		if (splitted.length > 1) {
			port = Integer.parseInt(splitted[1]);
		}
		
		return new InetSocketTransportAddress(splitted[0], port);
	}
}


