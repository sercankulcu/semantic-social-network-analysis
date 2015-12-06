package sna;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Resource;

import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;

public class TwitterInterface {

	private JenaInterface jenaInt = new JenaInterface();
	private String screenName = "skulcu";
	private List<Long> nodes = new ArrayList<Long>();
	private List<Integer> edgesfrom = new ArrayList<Integer>();
	private List<Integer> edgesto = new ArrayList<Integer>();
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
						edgesfrom.add(0);
						edgesto.add(index);
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
								edgesfrom.add(cycle);
								edgesto.add(index);
								// jena interface add resource userid, id , property userid->id
								
								if (count++ > 100) {
									
									index2 = followerID.length;
								}
							}
							else {
							
								jenaInt.addPropertyToRDF(resources.get(cycle), resources.get(result));
								edgesfrom.add(cycle);
								edgesto.add(result);
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

				try {
					int i = 0;
					//{id: 1, label: 'Abdelmoumene Djabou'},
					writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("webroot/dist/network.js"), "utf-8"));
					writer.write("var nodes = [" + "\n");
					for(i = 0; i < nodes.size() - 1; i++)
						writer.write("{id: " + i + ",label: '" + nodes.get(i) + "'},\n");
					if(nodes.size() > 0)
						writer.write("{id: " + i + ",label: '" + nodes.get(i) + "'}\n");
					writer.write("];" + "\n");

					writer.write("var edges = [" + "\n");
					for(i = 0; i < edgesfrom.size() - 1; i++)
						writer.write("{from: " + edgesfrom.get(i) + ", to: " + edgesto.get(i) + "},\n");
					if(nodes.size() > 0)
						writer.write("{from: " + edgesfrom.get(i) + ", to: " + edgesto.get(i) + "}\n");
					writer.write("];" + "\n");

				} catch (IOException ex) {
					System.out.println("error file");
				} finally {
					try {writer.close();} catch (Exception ex) {/*ignore*/}
				}

			} while (/*(cursor = ids.getNextCursor()) != 0*/ false);

		} catch (TwitterException te) {

			te.printStackTrace();
			System.out.println("Failed to get friends' ids: " + te.getMessage());
			//System.exit(-1);
		}
	}
}
