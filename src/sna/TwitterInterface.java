package sna;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.jena.rdf.model.Resource;

import edu.uci.ics.jung.algorithms.cluster.EdgeBetweennessClusterer;
import edu.uci.ics.jung.algorithms.cluster.VoltageClusterer;
import edu.uci.ics.jung.algorithms.scoring.BarycenterScorer;
import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.scoring.ClosenessCentrality;
import edu.uci.ics.jung.algorithms.scoring.DegreeScorer;
import edu.uci.ics.jung.algorithms.scoring.DistanceCentralityScorer;
import edu.uci.ics.jung.algorithms.scoring.EigenvectorCentrality;
import edu.uci.ics.jung.algorithms.scoring.HITS;
import edu.uci.ics.jung.algorithms.scoring.HITS.Scores;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.event.GraphEvent.Vertex;
import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;

public class TwitterInterface {

	private JenaInterface jenaInt = new JenaInterface();
	private String screenName = "skulcu";
	private List<Long> nodes = new ArrayList<Long>();
	private List<Long> edgesfrom = new ArrayList<Long>();
	private List<Long> edgesto = new ArrayList<Long>();
	private List<Resource> resources = new ArrayList<Resource>();

	public int getIndex(long id) {

		for(int i = 0; i < nodes.size(); i++) {

			if(id == nodes.get(i)) {
				return i;
			}
		}
		return -1;
	}

	public TwitterInterface(String s) {

		screenName = s;
	}

	public void getFollowers() {


		try {
			/*
			ConfigurationBuilder cb = new ConfigurationBuilder();  
			cb.setOAuthConsumerKey("----- YOUR DATA HERE ------");
			cb.setOAuthConsumerSecret("----- YOUR DATA HERE ------");
			cb.setOAuthAccessToken("----- YOUR DATA HERE ------");
			cb.setOAuthAccessTokenSecret("----- YOUR DATA HERE ------"); 

			TwitterFactory twitterF = new TwitterFactory(cb.build());    
			 */
			Twitter twitter = new TwitterFactory().getInstance();

			User user = twitter.showUser(screenName);

			if (user.getStatus() != null) {

				System.out.println("@" + user.getScreenName() + " - " + user.getStatus().getText());
			} else {

				// the user is protected
				System.out.println("@" + user.getScreenName());
			}

			Resource res = null ;
			int result = getIndex(user.getId()); // get first users id
			if(result == -1) {

				res = jenaInt.addResourceToRDF(Long.toString(user.getId()));
				nodes.add(user.getId());
				resources.add(res);
			}

			long cursor = -1;
			IDs ids;
			System.out.println("Listing following ids.");
			int index = 0;
			int index2 = 0;
			do {

				//ids = twitter.getFriendsIDs(screenName, cursor);
				ids = twitter.getFollowersIDs(screenName, cursor);

				long[] followerID = ids.getIDs();

				System.out.println("id.length " + followerID.length);

				for (index = 0; index < followerID.length; index++) {

					System.out.println(index + " cursor " + cursor + "  " + followerID[index]);

					result = getIndex(followerID[index]);
					if(result == -1) {

						Resource res2 = jenaInt.addResourceToRDF(Long.toString(followerID[index]));
						jenaInt.addPropertyToRDF(res, res2);
						nodes.add(followerID[index]);
						resources.add(res2);
						edgesfrom.add(nodes.get(0));
						edgesto.add(nodes.get(index));
					}
					// jena interface add resource userid, id , property userid->id 
				}

				int cyclelimit = index;
				for(int cycle = 1; cycle < cyclelimit; cycle++)
				{

					int count = 0;
					try {

						IDs ids1 = twitter.getFollowersIDs(nodes.get(cycle), -1/*cursor*/);
						followerID = ids1.getIDs();
						System.out.println(followerID.length);

						for (index2 = 0; index2 < followerID.length; index2++) {

							result = getIndex(followerID[index2]);
							System.out.println("for " + index + " cycle " + cycle + "  " + followerID[index2] + "result " + result);
							if(result == -1) {

								index++;

								Resource res2 = jenaInt.addResourceToRDF(Long.toString(followerID[index2]));

								jenaInt.addPropertyToRDF(resources.get(cycle), res2);

								nodes.add(followerID[index2]);
								resources.add(res2);
								edgesfrom.add(nodes.get(cycle));
								edgesto.add(nodes.get(index));
								// jena interface add resource userid, id , property userid->id

								if (count++ > 100) {

									index2 = followerID.length;
								}
							}
							else {

								jenaInt.addPropertyToRDF(resources.get(cycle), resources.get(result));
								edgesfrom.add(nodes.get(cycle));
								edgesto.add(nodes.get(result));
							}


						}

					} catch (TwitterException te) {

						te.printStackTrace();
						System.out.println("for ici Failed to get friends' ids: " + te.getMessage());
						//System.exit(-1);
					}


					try {
						Thread.sleep(60000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					jenaInt.save();
				}

				jenaInt.save();

				/*JSON*/
				Writer writer = null;

				Graph<Long, Integer> g = new DirectedSparseGraph<Long, Integer>();

				try {

					int i = 0;
					writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("webroot/dist/network.js"), "utf-8"));
					writer.write("var nodes = [" + "\n");
					for(i = 0; i < nodes.size() - 1; i++) {

						writer.write("{id: " + i + ",label: '" + nodes.get(i) + "'},\n");
						g.addVertex(nodes.get(i));
					}
					if(nodes.size() > 0)
						writer.write("{id: " + i + ",label: '" + nodes.get(i) + "'}\n");
					writer.write("];" + "\n");

					writer.write("var edges = [" + "\n");
					for(i = 0; i < edgesfrom.size() - 1; i++) {

						writer.write("{from: " + edgesfrom.get(i) + ", to: " + edgesto.get(i) + "},\n");
						g.addEdge(i, edgesfrom.get(i), edgesto.get(i));
					}
					if(nodes.size() > 0)
						writer.write("{from: " + edgesfrom.get(i) + ", to: " + edgesto.get(i) + "}\n");
					writer.write("];" + "\n");

				} catch (IOException ex) {

					System.out.println("error file");
				} finally {
					try {writer.close();} catch (Exception ex) {/*ignore*/}
				}

				for(int i = 0; i < nodes.size(); i++) {

					jenaInt.calculateInOutDegree("in", Long.toString(nodes.get(i)), resources.get(i));
					jenaInt.calculateInOutDegree("out", Long.toString(nodes.get(i)), resources.get(i));
				}

				BetweennessCentrality<Long, Integer> bc = new BetweennessCentrality<Long, Integer>(g);
				for (Long v : g.getVertices()) {
					jenaInt.addLiteralBetweenness(resources.get(getIndex(v)), bc.getVertexScore(v));
				}

				ClosenessCentrality<Long, Integer> cc = new ClosenessCentrality<Long, Integer>(g);
				for (Long v : g.getVertices()) {
					jenaInt.addLiteralCloseness(resources.get(getIndex(v)), cc.getVertexScore(v));
				}

				EigenvectorCentrality<Long, Integer> ec = new EigenvectorCentrality<Long, Integer>(g);
				ec.acceptDisconnectedGraph(true);
				ec.evaluate();
				for (Long v : g.getVertices()) {
					jenaInt.addLiteralEigenvector(resources.get(getIndex(v)), ec.getVertexScore(v));
				}

				PageRank<Long, Integer> pr = new PageRank<Long, Integer>(g, 0.8);
				pr.evaluate();
				for (Long v : g.getVertices()) {
					jenaInt.addLiteralPageRank(resources.get(getIndex(v)), pr.getVertexScore(v));
				}

				BarycenterScorer<Long, Integer> bs = new BarycenterScorer<Long, Integer>(g);
				for (Long v : g.getVertices()) {
					jenaInt.addLiteralBary(resources.get(getIndex(v)), bs.getVertexScore(v));
				}

				DegreeScorer<Long> ds = new DegreeScorer<Long>(g);
				for (Long v : g.getVertices()) {
					jenaInt.addLiteralDegree(resources.get(getIndex(v)), ds.getVertexScore(v));
				}

				DistanceCentralityScorer<Long, Integer> dcs = new DistanceCentralityScorer<Long, Integer>(g, false);
				for (Long v : g.getVertices()) {
					jenaInt.addLiteralDistance(resources.get(getIndex(v)), dcs.getVertexScore(v));
				}

				HITS<Long, Integer> hits = new HITS<Long, Integer>(g);
				hits.initialize();
				hits.evaluate();
				for (Long v : g.getVertices()) {
					Scores s = hits.getVertexScore(v);	
					jenaInt.addLiteralHits(resources.get(getIndex(v)), s.hub);
				}

				jenaInt.save();

			} while (/*(cursor = ids.getNextCursor()) != 0*/ false);

		} catch (TwitterException te) {

			te.printStackTrace();
			System.out.println("Failed to get friends' ids: " + te.getMessage());
			//System.exit(-1);
		}
	}

	public void test() throws IOException {

		
		 List<String> nodestest = new ArrayList<String>();
		 List<String> edgesfromtest = new ArrayList<String>();
		 List<String> edgestotest = new ArrayList<String>();
		
		Graph<String, Integer> g = new DirectedSparseGraph<String, Integer>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("webroot/dist/network.js"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {

			String line = br.readLine();
			int count = 0;

			while (line != null) {
				//sb.append(line);
				//sb.append(System.lineSeparator());
				
				//System.out.println(line);
				if(line.contains("id")) {
					String[] r = line.split("'");
					g.addVertex(r[1]);
					nodestest.add(r[1]);
					//System.out.println(r[0] + "--" + r[1] +  "--");					
				}
				else if(line.contains("from")) {
				//	System.out.println(line);
					StringTokenizer st = new StringTokenizer(line, " ,}");
				//	System.out.println(line);
					while(st.hasMoreTokens()) {
						//System.out.println(line);
						st.nextToken();
						String from = st.nextToken();
						st.nextToken();
						String to = st.nextToken();
						//System.out.println(from + "  " + to);
						g.addEdge(count++, from, to);
						edgesfromtest.add(from);
						edgestotest.add(to);
					}
				}
				line = br.readLine();
			}
		} finally {
			br.close();
		}

		
		int removevertex = g.getVertexCount();
		
		EdgeBetweennessClusterer<String, Integer> clusterer = new EdgeBetweennessClusterer<String, Integer>(500);
		Set<Set<String>> clusterSet = clusterer.transform(g);
		
		//int numCandidates = 2500;
		//VoltageClusterer<String, Integer> clusterer = new VoltageClusterer<String, Integer>(g, numCandidates);
		//Collection<Set<String>> clusterSet = (Collection<Set<String>>) clusterer.cluster(numCandidates); 
		
		
		int cluster = 0;
		int index = 0;
		
		Writer writer1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("EdgeBetweennessClustererOutput.txt"), "utf-8"));
		
		for (Iterator<Set<String>> it1 = clusterSet.iterator(); it1.hasNext();) {
			Set<String> verticesSet = (Set<String>) it1.next();
			writer1.write("Cluster " + cluster + ":\n");
 			for (Iterator<String> it2 = verticesSet.iterator(); it2.hasNext();) {
 				String vertex = (String) it2.next();
 				writer1.write("    " + vertex + "\n");
				System.out.println(index++ + " " + cluster + "\t" + vertex);
				
				boolean bulundu = false;
				for(int i = 0; i < nodestest.size(); i++) {
					if(nodestest.get(i).equals(vertex))
						bulundu = true;
				}
				if(bulundu == false)
					System.out.println(vertex + " bulunamdi");
				
				for(int i = 0; i < edgesfromtest.size(); i++) {
	
			//		System.out.println(i);
					if(edgesfromtest.get(i).equals(vertex))
						edgesfromtest.set(i, String.valueOf(cluster));
			//		System.out.println(i);
					if(edgestotest.get(i).equals(vertex))
						edgestotest.set(i, String.valueOf(cluster));
			//		System.out.println(i);
					if(i < nodestest.size()) {
						if(nodestest.get(i).equals(vertex))
							nodestest.set(i, String.valueOf(cluster));
					}
				}
			}
			cluster++;
		}
		writer1.close();

		Writer writer = null;
		
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("webroot/dist/network2.js"), "utf-8"));

		try {

			int i = 0;
			writer.write("var nodes = [" + "\n");
			for(i = 0; i < cluster - 1; i++) {

				writer.write("{id: " + i + ",label: '" + i + "', size: " + i + ", group: " + i + " },\n");
			}
			//if(nodestest.size() > 0)
			writer.write("{id: " + i + ",label: '" + i + "', size: " + i + ", group: " + i + " }\n");
			writer.write("];" + "\n");

			writer.write("var edges = [" + "\n");
			for(i = 0; i < edgesfromtest.size() - 1; i++) {

				if(edgesfromtest.get(i).equals(edgestotest.get(i)) == false )
					writer.write("{from: " + edgesfromtest.get(i) + ", to: " + edgestotest.get(i) + "},\n");
			}
			if(edgesfromtest.size() > 0)
				writer.write("{from: " + edgesfromtest.get(i) + ", to: " + edgestotest.get(i) + "}\n");
			writer.write("];" + "\n");

		} catch (IOException ex) {

			System.out.println("error file");
		} finally {
			try {writer.close();} catch (Exception ex) {/*ignore*/}
		}
	}
}
