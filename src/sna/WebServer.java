package sna;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class WebServer extends AbstractHandler {

	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) 
			throws IOException, ServletException
	{
		
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		response.getWriter().println("<table>");
		response.getWriter().println("<tr><td><img src=\"files/etu.jpg\" alt=\"TOBB ETU\" style=\"width:80px;height:64px;\"></td>");
		response.getWriter().println("<td><h1>Social Network Analysis</h1></td></tr></table>");
		response.getWriter().println("<form method=\"post\" action=\"analysis\"><br></br>");
		response.getWriter().println("<input type=\"text\" name=\"text\" value=\"Enter Twitter ID...\" size=\"40\" maxlength=\"40\" onfocus=\"this.value=''\">");
		response.getWriter().println("<input type=\"submit\" value=\"Show Result!\" align=\"center\"></form>User ID: ");

		// after clicked submit button
		if(baseRequest.toString().contains("analysis")) {

			String userid = request.getParameter("text");
			response.getWriter().println(userid + "");
			
			TwitterInterface twitterInt = new TwitterInterface(userid);
			twitterInt.getFollowers();
		
			response.getWriter().println("<script type=\"text/javascript\" src=\"files/dist/vis.js\"></script>");		//algorithm
			response.getWriter().println("<script type=\"text/javascript\" src=\"files/dist/network.js\"></script>");	//data
			response.getWriter().println("<link href=\"files/dist/vis.css\" rel=\"stylesheet\" type=\"text/css\" />");
			response.getWriter().println("<style type=\"text/css\"> #mynetwork { width: 1200px;  height: 700px;  border: 1px solid lightgray;} </style>");
			response.getWriter().println("<div id=\"mynetwork\"></div>");
			response.getWriter().println("<script type=\"text/javascript\">");
			response.getWriter().println("var container = document.getElementById('mynetwork');");
			response.getWriter().println("var data = { nodes: nodes,   edges: edges  };");
			response.getWriter().println("var options = {};");
			response.getWriter().println("var network = new vis.Network(container, data, options);</script>");
		}
	}
}
