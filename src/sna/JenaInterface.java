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
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.XSD;

/*
 * Handles Jena operations
 * 
 * 
 * */

public class JenaInterface {

	private OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
	
	String NS = "http://sercan.com/semweb/";
	
	OntClass classPerson = model.createClass( NS + "Person" );
	
	OntProperty hasInDegreeValue = model.createDatatypeProperty( NS + "hasInDegreeValue" );
	OntProperty hasOutDegreeValue = model.createDatatypeProperty( NS + "hasOutDegreeValue" );
	OntProperty hasDegreeValue = model.createDatatypeProperty( NS + "hasDegreeValue" );
	OntProperty hasBetweennessCentralityValue = model.createDatatypeProperty( NS + "hasBetweennessCentralityValue" );
	OntProperty hasClosenessCentralityValue = model.createDatatypeProperty( NS + "hasClosenessCentralityValue" );
	OntProperty hasEigenvectorCentralityValue = model.createDatatypeProperty( NS + "hasEigenvectorCentralityValue" );
	OntProperty hasPageRankValue = model.createDatatypeProperty( NS + "hasPageRankValue" );
	OntProperty hasBaryCentralityValue = model.createDatatypeProperty( NS + "hasBaryCentralityValue" );
	OntProperty hasDistanceCentralityValue = model.createDatatypeProperty( NS + "hasDistanceCentralityValue" );
	OntProperty hasHITSValue = model.createDatatypeProperty( NS + "hasHITSValue" );
	
	OntProperty hasFollower = model.createObjectProperty( NS + "hasFollower" );
	
	private int index = 0;
	
	public JenaInterface() {
	
		model.setNsPrefix("ssna", NS);
		model.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
		model.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
		
		hasInDegreeValue.addDomain(classPerson);
		hasOutDegreeValue.addDomain(classPerson);
		hasDegreeValue.addDomain(classPerson);
		hasBetweennessCentralityValue.addDomain(classPerson);
		hasClosenessCentralityValue.addDomain(classPerson);
		hasEigenvectorCentralityValue.addDomain(classPerson);
		hasPageRankValue.addDomain(classPerson);
		hasBaryCentralityValue.addDomain(classPerson);
		hasDistanceCentralityValue.addDomain(classPerson);
		hasHITSValue.addDomain(classPerson);
		
		hasInDegreeValue.addRange(XSD.nonNegativeInteger);
		hasOutDegreeValue.addRange(XSD.nonNegativeInteger);
		hasDegreeValue.addRange(XSD.nonNegativeInteger);
		hasBetweennessCentralityValue.addRange(XSD.xdouble);
		hasClosenessCentralityValue.addRange(XSD.xdouble);
		hasEigenvectorCentralityValue.addRange(XSD.xdouble);
		hasPageRankValue.addRange(XSD.xdouble);
		hasBaryCentralityValue.addRange(XSD.xdouble);
		hasDistanceCentralityValue.addRange(XSD.xdouble);
		hasHITSValue.addRange(XSD.xdouble);
		
		hasFollower.addDomain(classPerson);
		hasFollower.addRange(classPerson);
		
	}

	/*
	 * creates individual given id and add properties to created individual
	 * */
	public Resource addResourceToRDF(String id) {

		OntClass node = model.getOntClass( NS + "Person" );
		Individual temp = model.createIndividual( NS + id, node );
		
		temp.addProperty(FOAF.accountName, id);
		temp.addProperty(OWL.hasValue, Integer.toString(index++));
		temp.addProperty(RDFS.label, id);
		temp.addProperty(RDF.type, "Person");

		return temp;
	}

	/*
	 *  add knows property to given resource
	 * */
	public void addPropertyToRDF(Resource from, Resource to) {

		from.addProperty(FOAF.knows, to);
		from.addProperty(SKOS.related, to);
		from.addProperty(hasFollower, to);
	}
	
	public void addLiteralBetweenness(Resource res, double value) {

		res.addLiteral(hasBetweennessCentralityValue, value);
	}
	
	public void addLiteralCloseness(Resource res, double value) {

		res.addLiteral(hasClosenessCentralityValue, value);
	}
	
	public void addLiteralEigenvector(Resource res, double value) {

		res.addLiteral(hasEigenvectorCentralityValue, value);
	}
	
	public void addLiteralDistance(Resource res, double value) {

		res.addLiteral(hasDistanceCentralityValue, value);
	}
	
	public void addLiteralPageRank(Resource res, double value) {

		res.addLiteral(hasPageRankValue, value);
	}
	
	public void addLiteralBary(Resource res, double value) {

		res.addLiteral(hasBaryCentralityValue, value);
	}
	
	public void addLiteralHits(Resource res, double value) {

		res.addLiteral(hasHITSValue, value);
	}
	
	public void addLiteralDegree(Resource res, int value) {

		res.addLiteral(hasDegreeValue, value);
	}
	
	/*
	 * calculates in and out defrees of given resource
	 * 
	 * */
	public void calculateInOutDegree(String inout, String id, Resource res) {
			
		String queryString = null;
		if(inout.startsWith("in")) {
			queryString = 
				"PREFIX ssna: <http://sercan.com/semweb/>" + 
				"SELECT ( COUNT (?subject) AS ?howmany ) " + 
				"WHERE { ?subject ssna:hasFollower ssna:" +  id + " }";
		}
		else {
			queryString = 
				"PREFIX ssna: <http://sercan.com/semweb/>" + 
				"SELECT ( COUNT (?object) AS ?howmany ) " + 
				"WHERE { ssna:"+ id + " ssna:hasFollower ?object }";
		}
		
		//System.out.println(queryString);
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, model);

		try {

			ResultSet results = qexec.execSelect();
			
		    for (; results.hasNext();) {

		    	QuerySolution s = results.nextSolution();
		        Literal howmany = ((Literal) s.get("howmany"));

		        if(inout.startsWith("in")) {
			    	res.addLiteral(hasInDegreeValue, howmany.getInt());
			    }
			    else {
			    	res.addLiteral(hasOutDegreeValue, howmany.getInt());
			    }
		    }
		    
		}
		finally {

		   qexec.close();
		}
	}
	
	/*
	 * save created model into a rdf file
	 * */
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
