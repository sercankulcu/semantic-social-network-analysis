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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.jena.rdf.model.Resource;

import edu.uci.ics.jung.algorithms.cluster.EdgeBetweennessClusterer;
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
import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;

/*
 * Handle fetching data from twitter, clustering 
 * 
 * */
public class TwitterInterface {

	private JenaInterface jenaInt = new JenaInterface();
	private String screenName = "skulcu";
	private List<Long> nodes = new ArrayList<Long>();
	private List<Long> edgesfrom = new ArrayList<Long>();
	private List<Long> edgesto = new ArrayList<Long>();
	private List<Resource> resources = new ArrayList<Resource>();

	/*
	 * return index of given id from the list
	 * */
	public int getIndex(long id) {

		for(int i = 0; i < nodes.size(); i++) {

			if(id == nodes.get(i)) {
				return i;
			}
		}
		return -1;
	}

	/*
	 * constructor
	 * sets screenname
	 * */
	public TwitterInterface(String s) {

		screenName = s;
	}

	/*
	 * fetches users followers and their followers
	 * */
	public void getFollowers() {


		try {
			/* can run without these parameters 
			ConfigurationBuilder cb = new ConfigurationBuilder();  
			cb.setOAuthConsumerKey("----- YOUR DATA HERE ------");
			cb.setOAuthConsumerSecret("----- YOUR DATA HERE ------");
			cb.setOAuthAccessToken("----- YOUR DATA HERE ------");
			cb.setOAuthAccessTokenSecret("----- YOUR DATA HERE ------"); 
			TwitterFactory twitterF = new TwitterFactory(cb.build());    
			 */
			Twitter twitter = new TwitterFactory().getInstance();
			User user = twitter.showUser(screenName);
			Resource res = null ;

			int result = getIndex(user.getId()); // get first users id

			if(result == -1) { // user does not exist, add this user to the list

				res = jenaInt.addResourceToRDF(Long.toString(user.getId()));
				nodes.add(user.getId());
				resources.add(res);
			}

			long cursor = -1; // used for twitter time frame constraint
			IDs ids;
			int index = 0;
			int index2 = 0;

			do {

				ids = twitter.getFollowersIDs(screenName, cursor);

				long[] followerID = ids.getIDs();

				for (index = 0; index < followerID.length; index++) {

					result = getIndex(followerID[index]);

					if(result == -1) { // user does not exist, add this user to the list

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

						for (index2 = 0; index2 < followerID.length; index2++) {

							result = getIndex(followerID[index2]);

							if(result == -1) {

								index++;

								Resource res2 = jenaInt.addResourceToRDF(Long.toString(followerID[index2]));

								jenaInt.addPropertyToRDF(resources.get(cycle), res2);
								nodes.add(followerID[index2]);
								resources.add(res2);
								edgesfrom.add(nodes.get(cycle));
								edgesto.add(nodes.get(index));
								// jena interface add resource userid, id , property userid->id

								if (count++ > 100) { // do not add more than 100 friends

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
						System.out.println("Failed to get friends' ids: " + te.getMessage());
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
					if(nodes.size() > 0) {

						writer.write("{id: " + i + ",label: '" + nodes.get(i) + "'}\n");
					}
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

	public void clustering() throws IOException {


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

				if(line.contains("id")) {
					String[] r = line.split("'");
					g.addVertex(r[1]);
					nodestest.add(r[1]);
				}
				else if(line.contains("from")) {

					StringTokenizer st = new StringTokenizer(line, " ,}");
					
					while(st.hasMoreTokens()) {
					
						st.nextToken();
						
						String from = st.nextToken();
						st.nextToken();
						
						String to = st.nextToken();
						
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

		EdgeBetweennessClusterer<String, Integer> clusterer = new EdgeBetweennessClusterer<String, Integer>(500);
		Set<Set<String>> clusterSet = clusterer.transform(g);

		//int numCandidates = 2500;
		//VoltageClusterer<String, Integer> clusterer = new VoltageClusterer<String, Integer>(g, numCandidates);
		//Collection<Set<String>> clusterSet = (Collection<Set<String>>) clusterer.cluster(numCandidates); 


		int cluster = 0;
		Writer writer1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("EdgeBetweennessClustererOutput.txt"), "utf-8"));

		for (Iterator<Set<String>> it1 = clusterSet.iterator(); it1.hasNext();) {

			Set<String> verticesSet = (Set<String>) it1.next();
			writer1.write("Cluster " + cluster + ":\n");
			for (Iterator<String> it2 = verticesSet.iterator(); it2.hasNext();) {
			
				String vertex = (String) it2.next();
				writer1.write("    " + vertex + "\n");
				
				boolean found = false;
				for(int i = 0; i < nodestest.size(); i++) {
					if(nodestest.get(i).equals(vertex))
						found = true;
				}
				if(found == false)
					System.out.println(vertex + " could not found!");

				for(int i = 0; i < edgesfromtest.size(); i++) {

					if(edgesfromtest.get(i).equals(vertex))
						edgesfromtest.set(i, String.valueOf(cluster));

					if(edgestotest.get(i).equals(vertex))
						edgestotest.set(i, String.valueOf(cluster));
					
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
