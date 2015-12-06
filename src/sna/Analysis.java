package sna;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.webapp.WebAppContext;

public class Analysis {

	public static void main(String[] args) throws Exception
	{
		System.out.println("started");
		Server server = new Server(8080);

		String warUrlString = "C:\\Users\\sercan\\workspace\\sna\\webroot";//warUrl.toExternalForm();
	    
	    System.out.println(warUrlString);

	    // Create a webAppContext, pointing the "webroot" folder to the "/files" url. 
	    // The files will be available at <server>:<port>/files
	    WebAppContext wac = new WebAppContext();
	    wac.setResourceBase(warUrlString);
	    wac.setContextPath("/files");

	    // Attach handlers to the server
	    HandlerList handlerList = new HandlerList();
	    handlerList.setHandlers(new Handler[]{wac, new WebServer()});
	    server.setHandler(handlerList);
        //server.setHandler(new WebServer());

        server.start();
        server.join();
	}
}
