package org.fadil;

import java.util.HashSet;
import java.util.Set;

import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

/**
 * This class is the handler for an AWS Lambda function that acts as a fulfillment service
 * for an Alexa skill. It is also the entry point of the lambda function and has to be
 * defined in the handler field as "OxfordSpeechletRequestStreamHandler" when creating the
 * Lambda function on AWS Cloud.
 * 
 * @author Fadil
 * @version 1.0
 * @since 17/02/2018
 * 
 */
public class OxfordSpeechletRequestStreamHandler extends SpeechletRequestStreamHandler {
	
	private static final Set<String> SUPPORTED_APPLICATION_IDS;
	
	static {
		
		SUPPORTED_APPLICATION_IDS = new HashSet<>(); 
		SUPPORTED_APPLICATION_IDS.add("APP_ID"); // Application Id of the Alexa skill
		
	}
	
	public OxfordSpeechletRequestStreamHandler() {
		super(new OxfordSpeechlet(), SUPPORTED_APPLICATION_IDS);
	}

	public OxfordSpeechletRequestStreamHandler(Speechlet speechlet, Set<String> supportedApplicationIds) {
		super(speechlet, supportedApplicationIds);
	}

}
