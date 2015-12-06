package sna;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;

public class JenaInterface {

	private OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
	
	String NS = "http://sercan.com/semweb/";
	
	OntClass classPerson = model.createClass( NS + "Person" );
	
	OntProperty inDegree = model.createDatatypeProperty( NS + "inDegree" );
	OntProperty outDegree = model.createDatatypeProperty( NS + "outDegree" );
	OntProperty hasFollower = model.createObjectProperty( NS + "hasFollower" );
	
	private int index = 0;
	
	public JenaInterface() {
	
		model.setNsPrefix("ssna", NS);
		model.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
		model.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
		
		inDegree.addDomain(classPerson);
		outDegree.addDomain(classPerson);
		
		inDegree.addRange(RDFS.Literal);
		outDegree.addRange(RDFS.Literal);
		
		hasFollower.addDomain(classPerson);
		hasFollower.addRange(classPerson);
		
	}

	public Resource addResourceToRDF(String id) {

		OntClass node = model.getOntClass( NS + "Person" );
		Individual temp = model.createIndividual( NS + id, node );
		
		temp.addProperty(FOAF.accountName, id);
		temp.addProperty(OWL.hasValue, Integer.toString(index++));
		temp.addProperty(RDFS.label, id);
		temp.addProperty(RDF.type, "Person");

		return temp;
	}

	public void addPropertyToRDF(Resource from, Resource to) {

		from.addProperty(FOAF.knows, to);
		from.addProperty(SKOS.related, to);
		
		//from.addProperty(inDegree, "3");
		//from.addProperty(outDegree, "3");
		from.addProperty(hasFollower, to);
	}
	
	public void calculateInDegree(String id) {
		
		String queryString = 
				"PREFIX ssna: <http://sercan.com/semweb/>" + 
					"SELECT ( COUNT (?subject) AS ?howmany ) WHERE { ?subject ssna:hasFollower ssna:" +  id + " }";
		
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, model);

		try {

			ResultSet results = qexec.execSelect();
		    for (; results.hasNext();) {
		        QuerySolution s = results.nextSolution();
		        System.out.println(s.toString());
		    }
		}
		finally {

		   qexec.close();
		}
		
	}
	
	public void calculateOutDegree(String id) {
		
		String queryString = 
				"PREFIX ssna: <http://sercan.com/semweb/>" + 
					"SELECT ( COUNT (?object) AS ?howmany ) WHERE { "+ id + " ssna:hasFollower ?object }";
		
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, model);

		try {

			ResultSet results = qexec.execSelect();
		    for (; results.hasNext();) {
		        QuerySolution s = results.nextSolution();
		        System.out.println(s.toString());
		    }
		}
		finally {

		   qexec.close();
		}
		
	}

	public void save() {

		FileOutputStream out = null;

		try {
			
			out = new FileOutputStream("network.rdf");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		model.write(out);

		try {
			
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
