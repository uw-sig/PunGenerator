/**
 * 
 */
package edu.uw.sig.owl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnonymousClassExpression;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.UnloadableImportException;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

/**
 * @author detwiler
 * @date Oct 6, 2014
 *
 */
public class PunningGenerator
{
	private OWLOntologyManager baseOntMan;
	private OWLDataFactory baseOntDF;
	private OWLOntology baseOnt;
	
	private OWLOntologyManager punOntMan;
	private OWLDataFactory punOntDF;
	private OWLOntology punOnt;
	private String punOntPath;
	
	private Map<OWLAnonymousClassExpression, OWLAnonymousIndividual> anonClsExprs = new HashMap<OWLAnonymousClassExpression, OWLAnonymousIndividual>();
	
	private void init(String baseOntPath, String punOntPath) throws OWLOntologyCreationException, IOException
	{
		// this was added by ltd on 12/22/2014 to get around a bug in Java
		System.setProperty("jdk.xml.entityExpansionLimit", "0");
		
		this.punOntPath = punOntPath;
		
		// first load base ontology
		baseOntMan = OWLManager.createOWLOntologyManager();
		
		// deal with imports
		//AutoIRIMapper mapper = new AutoIRIMapper(new File("resource"), true);
		//baseOntMan.addIRIMapper(mapper);
		
		baseOntDF = baseOntMan.getOWLDataFactory();
		
		File baseFile = new File(baseOntPath);
		try
		{
			baseOntMan.setSilentMissingImportsHandling(true);
			//OWLOntologyLoaderConfiguration.setMissingImportHandlingStrategy(MissingImportHandlingStrategy)
			baseOnt = baseOntMan.loadOntologyFromOntologyDocument(baseFile);
		}
		catch (UnloadableImportException e)
		{
			// don't worry about this for now, later we could add AutoIRIMapper
			System.out.println("skipping unloadable import");
		}
		
		// now create the punning ontology (and resources)
		AutoIRIMapper punmapper = new AutoIRIMapper(new File("output"), true);
		punOntMan = OWLManager.createOWLOntologyManager();
		punOntMan.addIRIMapper(punmapper);
		punOntDF = punOntMan.getOWLDataFactory();
		File punFile = new File(punOntPath);
		punFile.createNewFile();
		
		String baseOntIRIString = baseOntMan.getOntologyFormat(baseOnt).asPrefixOWLOntologyFormat().getDefaultPrefix();
		IRI baseIRI = baseOnt.getOntologyID().getOntologyIRI();
		//String punIRIString = baseIRI.toString().replaceAll("(\\w+)\\.owl", "$1.owl");
		IRI punIRI = IRI.create(baseOntIRIString);
		punOnt = punOntMan.createOntology(punIRI);
		
		// add import statement
		// construct IRI
		//IRI importIRI = baseOnt.getOntologyID().getOntologyIRI();
		System.err.println("will gen import statement for "+baseIRI);
		
		// insert import statement
		OWLImportsDeclaration importDec = punOntDF.getOWLImportsDeclaration(baseIRI);
		punOntMan.applyChange(new AddImport(punOnt,importDec));
	}
	
	public void run(String baseOntPath, String punOntPath)
	{
		try
		{
			init(baseOntPath, punOntPath);
		}
		catch (OWLOntologyCreationException e)
		{
			e.printStackTrace();
			System.err.println("ABORTING, failed to initialize ontologies");
			return;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.err.println("ABORTING, failed to initialize ontologies");
			return;
		}
		
		OWLOntologyWalker walker = new OWLOntologyWalker(
				Collections.singleton(baseOnt));
		// Now ask our walker to walk over the ontology. We specify a visitor
		// who gets visited by the various objects as the walker encounters
		// them. We need to create out visitor. This can be any ordinary
		// visitor, but we will extend the OWLOntologyWalkerVisitor because it
		// provides a convenience method to get the current axiom being visited
		// as we go. Create an instance and override the
		// visit(OWLObjectSomeValuesFrom) method, because we are interested in
		// some values from restrictions.
		OWLOntologyWalkerVisitor<Object> visitor = new OWLOntologyWalkerVisitor<Object>(
				walker)
		{
			@Override
			public Object visit(OWLObjectSomeValuesFrom desc)
			{
				// Print out the restriction
				//System.out.println(desc);
				// Print out the axiom where the restriction is used
				//System.out.println(" " + getCurrentAxiom());
				OWLAxiom currAxiom = getCurrentAxiom();
				if(currAxiom instanceof OWLSubClassOfAxiom)
				{
					OWLSubClassOfAxiom currSubAxiom = (OWLSubClassOfAxiom)currAxiom;
					OWLClassExpression subclass = currSubAxiom.getSubClass();
					
					// something odd happens where desc is captured when it is nested in current axiom
					// this rejects cases where desc is not the direct superclass of the current axiom's subclass
					OWLClassExpression superclass = currSubAxiom.getSuperClass();
					if(superclass!=desc) {
						/*
						System.out.println("rejecting:");
						System.out.println("superclass = "+superclass);
						System.out.println("description = "+desc);
						System.out.println();
						*/
						return null;
					}
					
					// NOTE: we could have chosen to create links from reified instances here, but that is not how
					// it is presently handled.
					if(subclass.isAnonymous()) // skip this for reified instances, they are handled elsewhere
						return null;
					
					if(subclass instanceof OWLClass) // why am I checking for this, what are the alternatives?
					{
						//System.out.println(" classes in sig: "+desc.getClassesInSignature());
						//Set<OWLClass> sigClses = desc.getClassesInSignature();
						OWLClassExpression restrFiller = desc.getFiller();
						if(restrFiller.isAnonymous())
						{
							/*
							OWLAnonymousIndividual reifiedInd = null;
							if(anonClsExprs.containsKey(restrFiller))
							{
								System.err.println("found previously encountered anon class expression");
								reifiedInd = anonClsExprs.get(restrFiller);
							}
							else
							{
								reifiedInd = getReifiedIndividual((OWLAnonymousClassExpression)restrFiller);
								anonClsExprs.put((OWLAnonymousClassExpression)restrFiller,reifiedInd);
							}*/
							
							// source as individual
							OWLIndividual sourceInd = punOntDF.getOWLNamedIndividual(((OWLClass) subclass).getIRI());
							
							// get property
							OWLObjectPropertyExpression prop = desc.getProperty();
							
							// target as individual
							OWLAnonymousIndividual reifiedInd = getReifiedIndividual((OWLAnonymousClassExpression)restrFiller);
							
							// create individual level assertion
							OWLObjectPropertyAssertionAxiom axiom1 = punOntDF
									.getOWLObjectPropertyAssertionAxiom(prop, sourceInd, reifiedInd);
							AddAxiom addAxiom1 = new AddAxiom(punOnt, axiom1);
							// Now we apply the change using the manager.
							punOntMan.applyChange(addAxiom1);
							
							/*
							System.err.println("complex signature!");
							System.out.println(" " + getCurrentAxiom());
							System.out.println(reifiedInd);
							*/
						} 
						else // regular named class
						{
							assertLinkFromSome(subclass, desc);
							/*
							// source class as individual
							OWLIndividual sourceInd = punOntDF.getOWLNamedIndividual(((OWLClass) superclass).getIRI());//((OWLClass) superclass).asOWLNamedIndividual();
							OWLClassAssertionAxiom srcClsAssert =punOntDF.getOWLClassAssertionAxiom(superclass, sourceInd);
							punOntMan.addAxiom(punOnt, srcClsAssert);
							
							// target class as individual
							//OWLClass sigCls = sigClses.iterator().next();
							OWLIndividual targetInd = punOntDF.getOWLNamedIndividual(((OWLClass) restrFiller).getIRI());//sigCls.asOWLNamedIndividual();
							OWLClassAssertionAxiom targetClsAssert =punOntDF.getOWLClassAssertionAxiom(restrFiller, targetInd);
							punOntMan.addAxiom(punOnt, targetClsAssert);
							
							// get property
							OWLObjectPropertyExpression prop = desc.getProperty();
							
							// create individual level assertion
							//punOntDF.getOWLNamedIndividual(arg0) might use this to create punned individual in pun ont
							
							OWLObjectPropertyAssertionAxiom axiom1 = punOntDF
									.getOWLObjectPropertyAssertionAxiom(prop, sourceInd, targetInd);
							AddAxiom addAxiom1 = new AddAxiom(punOnt, axiom1);
							// Now we apply the change using the manager.
							punOntMan.applyChange(addAxiom1);
							*/
							
						}
					}
					//System.out.println(" "+currSubAxiom.getSubClass());
					//System.out.println();
					
					// create and return instance level assertion
					return null;
				}
				return null;
			}
			
			@Override
			public Object visit(OWLDataHasValue desc)
			{
				OWLAxiom currAxiom = getCurrentAxiom();
				if(currAxiom instanceof OWLSubClassOfAxiom)
				{
					OWLSubClassOfAxiom currSubAxiom = (OWLSubClassOfAxiom)currAxiom;
					OWLClassExpression subclass = currSubAxiom.getSubClass();
					
					// handle the case of compound axioms
					OWLClassExpression superclass = currSubAxiom.getSuperClass();
					if(!(superclass instanceof OWLDataHasValue))
						return null;
					
					// NOTE: we could have chosen to create links from reified instances here, but that is not how
					// it is presently handled.
					if(subclass.isAnonymous()) // skip this for reified instances, they are handled elsewhere
						return null;

					//OWLLiteral restrVal = desc.getValue();
					assertLinkFromHas(subclass, desc);
					
					// create and return instance level assertion
					return null;
				}
				return null;
			}
			
			@Override
			public Object visit(OWLObjectIntersectionOf desc)
			{
				//System.err.println("found an object intersection");
				//OWLAnonymousIndividual individual = getReifiedIndividual(desc);
				
				OWLAnonymousIndividual reifiedInd = null;
				if(anonClsExprs.containsKey(desc))
				{
					//System.err.println("found previously encountered anon class expression");
					reifiedInd = anonClsExprs.get(desc);
				}
				else
				{
					//punOntDF.getOWLAnonymousIndividual();
					reifiedInd = punOntDF.getOWLAnonymousIndividual();
					anonClsExprs.put(desc,reifiedInd);
				}
				
				// TODO build it up here
				buildOutIntersection(desc, reifiedInd);
				
				return null;
			}
			
			/*
			@Override
			public Object visit(OWLDataHasValue desc)
			{
				
				
				// Print out the restriction
				System.out.println(desc);
				// Print out the axiom where the restriction is used
				System.out.println(" " + getCurrentAxiom());
				System.out.println();
				// We don't need to return anything here.
				return null;
			}*/
		};
		// Now ask the walker to walk over the ontology structure using our
		// visitor instance.
		walker.walkStructure(visitor);
	}
	
	private void assertLinkFromSome(OWLClassExpression subclass, OWLObjectSomeValuesFrom restr)
	{
		OWLClassExpression restrFiller = restr.getFiller();
		
		// source class as individual
		OWLIndividual sourceInd = punOntDF.getOWLNamedIndividual(((OWLClass) subclass).getIRI());//((OWLClass) superclass).asOWLNamedIndividual();
		OWLClassAssertionAxiom srcClsAssert =punOntDF.getOWLClassAssertionAxiom(subclass, sourceInd);
		punOntMan.addAxiom(punOnt, srcClsAssert);
		
		// target class as individual
		//OWLClass sigCls = sigClses.iterator().next();
		OWLIndividual targetInd = punOntDF.getOWLNamedIndividual(((OWLClass) restrFiller).getIRI());//sigCls.asOWLNamedIndividual();
		OWLClassAssertionAxiom targetClsAssert =punOntDF.getOWLClassAssertionAxiom(restrFiller, targetInd);
		punOntMan.addAxiom(punOnt, targetClsAssert);
		
		// get property
		OWLObjectPropertyExpression prop = restr.getProperty();
		
		// create individual level assertion
		//punOntDF.getOWLNamedIndividual(arg0) might use this to create punned individual in pun ont
		
		OWLObjectPropertyAssertionAxiom axiom1 = punOntDF
				.getOWLObjectPropertyAssertionAxiom(prop, sourceInd, targetInd);
		AddAxiom addAxiom1 = new AddAxiom(punOnt, axiom1);
		// Now we apply the change using the manager.
		punOntMan.applyChange(addAxiom1);
	}
	
	private void assertIndLinkFromSome(OWLIndividual sourceInd, OWLObjectSomeValuesFrom restr)
	{
		OWLClassExpression restrFiller = restr.getFiller();
		
		// target class as individual
		//OWLClass sigCls = sigClses.iterator().next();
		OWLIndividual targetInd = punOntDF.getOWLNamedIndividual(((OWLClass) restrFiller).getIRI());//sigCls.asOWLNamedIndividual();
		OWLClassAssertionAxiom targetClsAssert =punOntDF.getOWLClassAssertionAxiom(restrFiller, targetInd);
		punOntMan.addAxiom(punOnt, targetClsAssert);
		
		// get property
		OWLObjectPropertyExpression prop = restr.getProperty();
		
		// create individual level assertion
		//punOntDF.getOWLNamedIndividual(arg0) might use this to create punned individual in pun ont
		
		OWLObjectPropertyAssertionAxiom axiom1 = punOntDF
				.getOWLObjectPropertyAssertionAxiom(prop, sourceInd, targetInd);
		AddAxiom addAxiom1 = new AddAxiom(punOnt, axiom1);
		// Now we apply the change using the manager.
		punOntMan.applyChange(addAxiom1);
	}
	
	private void assertLinkFromHas(OWLClassExpression subclass, OWLDataHasValue restr)
	{
		// source class as individual
		OWLIndividual sourceInd = punOntDF.getOWLNamedIndividual(((OWLClass) subclass).getIRI());
		OWLClassAssertionAxiom srcClsAssert =punOntDF.getOWLClassAssertionAxiom(subclass, sourceInd);
		punOntMan.addAxiom(punOnt, srcClsAssert);
				
		// get target value
		OWLLiteral targetVal = restr.getValue();
		
		// get property
		OWLDataPropertyExpression prop = restr.getProperty();
		
		// create individual level assertion
		OWLDataPropertyAssertionAxiom axiom1 = punOntDF
				.getOWLDataPropertyAssertionAxiom(prop, sourceInd, targetVal);
		AddAxiom addAxiom1 = new AddAxiom(punOnt, axiom1);
		// Now we apply the change using the manager.
		punOntMan.applyChange(addAxiom1);
	}
	
	private void assertIndLinkFromHas(OWLIndividual sourceInd, OWLDataHasValue restr)
	{
		// get target value
		OWLLiteral targetVal = restr.getValue();
		
		// get property
		OWLDataPropertyExpression prop = restr.getProperty();
		
		// create individual level assertion
		OWLDataPropertyAssertionAxiom axiom1 = punOntDF
				.getOWLDataPropertyAssertionAxiom(prop, sourceInd, targetVal);
		AddAxiom addAxiom1 = new AddAxiom(punOnt, axiom1);
		// Now we apply the change using the manager.
		punOntMan.applyChange(addAxiom1);
	}
	
	private OWLAnonymousIndividual getReifiedIndividual(OWLAnonymousClassExpression expr)
	{
		OWLAnonymousIndividual reifiedInd = null;
		if(anonClsExprs.containsKey(expr))
		{
			//System.err.println("found previously encountered anon class expression");
			reifiedInd = anonClsExprs.get(expr);
		}
		else
		{
			//punOntDF.getOWLAnonymousIndividual();
			reifiedInd = punOntDF.getOWLAnonymousIndividual();
			anonClsExprs.put(expr,reifiedInd);
			
			/*
			// must build this individual here
			if(expr instanceof OWLObjectIntersectionOf)
			{
				reifiedInd = genIndForIntersection((OWLObjectIntersectionOf)expr);
				anonClsExprs.put(expr,reifiedInd);
			}
			else if(expr instanceof OWLObjectUnionOf)
			{
				reifiedInd = genIndForUnion(expr);
				anonClsExprs.put(expr,reifiedInd);
			}
			else
			{
				//TODO: what other cases need to be handled?
			}
			*/
		}

		return reifiedInd;
	}
	
	private OWLAnonymousIndividual buildOutIntersection(OWLObjectIntersectionOf expr, OWLAnonymousIndividual intersectionInd)
	{
		Set<OWLClassExpression> conjs = expr.asConjunctSet();
		for(OWLClassExpression valExpr : conjs)
		{
			// create an individual for value expression (NOTE: presently assumes all target values are named)
			// also assumes that all valExpr are restrictions
			if(valExpr instanceof OWLObjectSomeValuesFrom)
			{
				OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) valExpr;
				OWLObjectPropertyExpression prop = some.getProperty();
				assertIndLinkFromSome(intersectionInd, some);
				//OWLClassExpression restrFiller = some.getFiller();
				
				/*
				OWLIndividual targetInd = punOntDF.getOWLNamedIndividual(((OWLClass) valExpr).getIRI());
				OWLClassAssertionAxiom targetClsAssert =punOntDF.getOWLClassAssertionAxiom(valExpr, targetInd);
				punOntMan.addAxiom(punOnt, targetClsAssert);
				*/
			}
			else if(valExpr instanceof OWLObjectHasValue)
			{
				// Not yet implemented, not presently used in OCDM?
				System.err.println("found OWLObjectHasValue restriction in reified val");
				
			}
			else if(valExpr instanceof OWLDataHasValue)
			{
				//System.err.println("found OWLDataHasValue restriction in reified val");
				OWLDataHasValue has = (OWLDataHasValue) valExpr;
				OWLDataPropertyExpression prop = has.getProperty();
				assertIndLinkFromHas(intersectionInd, has);
			}
			/*
			// target class as individual
			//OWLClass sigCls = sigClses.iterator().next();
			OWLIndividual targetInd = punOntDF.getOWLNamedIndividual(((OWLClass) valExpr).getIRI());//sigCls.asOWLNamedIndividual();
			OWLClassAssertionAxiom targetClsAssert =punOntDF.getOWLClassAssertionAxiom(valExpr, targetInd);
			punOntMan.addAxiom(punOnt, targetClsAssert);
			
			// get property
			OWLObjectPropertyExpression prop = expr.getProperty();
			
			// create individual level assertion
			//punOntDF.getOWLNamedIndividual(arg0) might use this to create punned individual in pun ont
			
			OWLObjectPropertyAssertionAxiom axiom1 = punOntDF
					.getOWLObjectPropertyAssertionAxiom(prop, sourceInd, targetInd);
			AddAxiom addAxiom1 = new AddAxiom(punOnt, axiom1);
			// Now we apply the change using the manager.
			punOntMan.applyChange(addAxiom1);
			*/
			
		}
		
//TODO: finish this
		return null;
	}
	
	private OWLAnonymousIndividual buildOutUnion(OWLAnonymousClassExpression expr)
	{
		//TODO: not yet implemented
		return null;
	}
	
	/*
	private void loadOntology(String path) throws OWLOntologyCreationException
	{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		File file = new File(path);
		inputOnt = manager.loadOntologyFromOntologyDocument(file);
	}
	*/
	
	public boolean savePunOnt()
	{
		try
		{
			//File outDir = new File(owlOutputDir);
			File output = new File(punOntPath);
			IRI documentIRI = IRI.create(output);
			
			 // Now save a copy to another location in OWL/XML format (i.e. disregard
			// the format that the ontology was loaded in).
			//File f = File.createTempFile("owlapiexample", "example1.xml");
			//IRI documentIRI2 = IRI.create(output);
			punOntMan.saveOntology(punOnt, new RDFXMLOntologyFormat(), documentIRI);
			
			// Remove the ontology from the manager
			punOntMan.removeOntology(punOnt);
			
			//output.delete(); 
		}
		catch (OWLOntologyStorageException e)
		{
			e.printStackTrace();
			return false;
		}
		/*
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		*/
		
		return true;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		PunningGenerator punGen = new PunningGenerator();
		punGen.run("resource/cho.owl", "resource/cho_pun.owl");
		punGen.savePunOnt();

	}

}
