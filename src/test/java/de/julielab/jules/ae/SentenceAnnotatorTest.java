/** 
 * SentenceAnnotatorTest.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 2.2
 * Since version:   1.0
 *
 * Creation date: Nov 29, 2006 
 * 
 * This is a JUnit test for the SentenceAnnotator.
 **/

package de.julielab.jules.ae;

import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jules.types.Sentence;
import de.julielab.jules.types.TestScope;

public class SentenceAnnotatorTest extends TestCase {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(SentenceAnnotatorTest.class);
	
	private static final String LOGGER_PROPERTIES = "src/test/java/log4j.properties";
	
	// uncomment to test with/without scope
	//private static final String DESCRIPTOR = "src/test/resources/SentenceAnnotatorTest.xml";
	private static final String DESCRIPTOR = "src/test/resources/SentenceAnnotator_with-scope_Test.xml";

	// last sentence has no EOS symbol to test that also this is handled correctly
	private static final String[] TEST_TEXT = {
			"First sentence. Second \t sentence! \n    Last sentence?",
			"Hallo, jemand da? Nein, niemand.", "A test. METHODS: Bad stuffi",
			"" };

	private static final String[] TEST_TEXT_OFFSETS = { "0-15;16-34;40-54", "0-17;18-32",
			"0-7;8-16;17-27", "" };
	
	private static final int[] endOffsets = {54,32,27,0};
	
	
	/**
	 * Use the model in resources, split the text in TEST_TEXT 
	 * and compare the split result against TEST_TEXT_OFFSETS
	 */
	public void testProcess() {
	
		boolean annotationsOK = true;

		XMLInputSource sentenceXML = null;
		ResourceSpecifier sentenceSpec = null;
		AnalysisEngine sentenceAnnotator = null;

		try {
			sentenceXML = new XMLInputSource(DESCRIPTOR);
			sentenceSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(
					sentenceXML);
			sentenceAnnotator = UIMAFramework
					.produceAnalysisEngine(sentenceSpec);
		} catch (Exception e) {
			LOGGER.error("testProcess()", e); 
		}

		for (int i = 0; i < TEST_TEXT.length; i++) {

			JCas jcas = null;
			try {
				jcas = sentenceAnnotator.newJCas();
			} catch (ResourceInitializationException e) {
				LOGGER.error("testProcess()", e); 
			}

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("testProcess() - testing text: " + TEST_TEXT[i]); 
			}
			jcas.setDocumentText(TEST_TEXT[i]);

			// make one test scope ranging over complete document text annotations for the processing scope
			TestScope scope1 = new TestScope(jcas,0,endOffsets[i]);
			scope1.addToIndexes();
			//TestScope scope2 = new TestScope(jcas,37,54);

			
			try {
				sentenceAnnotator.process(jcas, null);
			} catch (Exception e) {
				LOGGER.error("testProcess()", e); 
			}

			// get the offsets of the sentences
			JFSIndexRepository indexes = jcas.getJFSIndexRepository();
			Iterator sentIter = indexes.getAnnotationIndex(Sentence.type)
					.iterator();

			String predictedOffsets = getPredictedOffsets(i, sentIter);

			// compare offsets
			if (!predictedOffsets.equals(TEST_TEXT_OFFSETS[i])) {
				annotationsOK = false;
				continue;
			}
		}
		assertTrue(annotationsOK);
	}


	private String getPredictedOffsets(int i, Iterator sentIter) {
		String predictedOffsets="";
		while (sentIter.hasNext()) {
			Sentence s = (Sentence) sentIter.next();
			LOGGER.debug("sentence: " + s.getCoveredText() + ": " + s.getBegin() + " - " + s.getEnd());
			predictedOffsets += (predictedOffsets.length() > 0) ? ";" : "";
			predictedOffsets += s.getBegin() + "-" + s.getEnd();
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("testProcess() - predicted: " + predictedOffsets); 
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("testProcess() - wanted: " + TEST_TEXT_OFFSETS[i]); 
		}
		return predictedOffsets;
	}

	
}
