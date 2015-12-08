package sna;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Resource;

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
						edgesfrom.add(Long.valueOf(0));
						edgesto.add(Long.valueOf(index));
					}
					// jena interface add resource userid, id , property userid->id 
				}
				
				int cyclelimit = index;
				for(int cycle = 1; cycle < 4; cycle++)
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
								edgesfrom.add(Long.valueOf(cycle));
								edgesto.add(Long.valueOf(index));
								// jena interface add resource userid, id , property userid->id
								
								if (count++ > 100) {
									
									index2 = followerID.length;
								}
							}
							else {
							
								jenaInt.addPropertyToRDF(resources.get(cycle), resources.get(result));
								edgesfrom.add(Long.valueOf(cycle));
								edgesto.add(Long.valueOf(result));
							}
							
							
						}

					} catch (TwitterException te) {

						te.printStackTrace();
						System.out.println("for ici Failed to get friends' ids: " + te.getMessage());
						//System.exit(-1);
					}
					
					/*
					try {
						Thread.sleep(60000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					*/
					jenaInt.save();
				}
				/*
				for(int cycle = 25; cycle < 35; cycle++)
				{

					edgesfrom.add(2);
					edgesto.add(cycle);
					edgesfrom.add(5);
					edgesto.add(cycle);
					edgesfrom.add(7);
					edgesto.add(cycle);
					edgesfrom.add(12);
					edgesto.add(cycle);
					edgesfrom.add(22);
					edgesto.add(cycle);
					edgesfrom.add(25);
					edgesto.add(cycle);
				}
				 */
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
						//System.out.println("first vertex count " + g.getVertexCount() + " node size " + nodes.size() + " edgesfrom size " + edgesfrom.size());
						//jenaInt.calculateInDegree(Long.toString(nodes.get(i)));

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
				
				System.out.println("first vertex count " + g.getVertexCount() + " node size " + nodes.size() + " edgesfrom size " + edgesfrom.size());
				
				BetweennessCentrality<Long, Integer> bc = new BetweennessCentrality<Long, Integer>(g);
				for (Long v : g.getVertices()) {
					if(bc.getVertexScore(v) > 0)
						System.out.println("BetweennessCentrality for\t" + v + "\t" + bc.getVertexScore(v));
				}
				
				System.out.println("beetween vertex count " + g.getVertexCount());
				
				ClosenessCentrality<Long, Integer> cc = new ClosenessCentrality<Long, Integer>(g);
				for (Long v : g.getVertices()) {
					if(cc.getVertexScore(v) > 0)
						System.out.println("ClosenessCentrality for\t" + v + "\t" + cc.getVertexScore(v));
				}
				
				System.out.println("closeness vertex count " + g.getVertexCount());
				
				EigenvectorCentrality<Long, Integer> ec = new EigenvectorCentrality<Long, Integer>(g);
				//ec.initialize();
				ec.acceptDisconnectedGraph(true);
				ec.evaluate();
				for (Long v : g.getVertices()) {
					if(ec.getVertexScore(v) > 0.0005)
						System.out.println("EigenvectorCentrality for\t" + v + "\t" + ec.getVertexScore(v));
				}
				
				System.out.println("eigenve vertex count " + g.getVertexCount());
				
				
				PageRank<Long, Integer> pr = new PageRank<Long, Integer>(g, 0.8);
				pr.evaluate();
				for (Long v : g.getVertices()) {
					if(pr.getVertexScore(v) > 0)
						System.out.println("PageRank for\t" + v + "\t" + pr.getVertexScore(v));
				}
				
				BarycenterScorer<Long, Integer> bs = new BarycenterScorer<Long, Integer>(g);
				for (Long v : g.getVertices()) {
					if(bs.getVertexScore(v) > 0)
						System.out.println("BarycenterScorer for\t" + v + "\t" + bs.getVertexScore(v));
				}
				
				
				DegreeScorer<Long> ds = new DegreeScorer<Long>(g);
				for (Long v : g.getVertices()) {
					if(ds.getVertexScore(v) > 1)
						System.out.println("DegreeScorer for\t" + v + "\t" + ds.getVertexScore(v));
				}
				
				System.out.println("degre vertex count " + g.getVertexCount());
				
				DistanceCentralityScorer<Long, Integer> dcs = new DistanceCentralityScorer<Long, Integer>(g, false);
				for (Long v : g.getVertices()) {
					if(dcs.getVertexScore(v) < 1)
						System.out.println("DistanceCentralityScorer for\t" + v + "\t" + dcs.getVertexScore(v));
				}
				
				System.out.println("distance vertex count " + g.getVertexCount());
				
				HITS<Long, Integer> hits = new HITS<Long, Integer>(g);
				hits.initialize();
				hits.evaluate();
				
				for (Long v : g.getVertices()) {
					Scores s = hits.getVertexScore(v);	
					if(s.hub > 0.0029)
						System.out.println("hits for\t" + v + "\t" + s.hub);
				}
				
				System.out.println("hits vertex count " + g.getVertexCount());

			} while (/*(cursor = ids.getNextCursor()) != 0*/ false);

		} catch (TwitterException te) {

			te.printStackTrace();
			System.out.println("Failed to get friends' ids: " + te.getMessage());
			//System.exit(-1);
		}
	}
}
