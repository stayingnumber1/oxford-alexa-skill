package org.fadil;

import java.util.HashSet;
import java.util.Set;

import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

public class OxfordSpeechletRequestStreamHandler extends SpeechletRequestStreamHandler {
	
	private static final Set<String> SUPPORTED_APPLICATION_IDS;
	
	static {
		
		SUPPORTED_APPLICATION_IDS = new HashSet<>();
		String appId = System.getenv("APP_ID");
		SUPPORTED_APPLICATION_IDS.add(appId);
		
	}
	
	public OxfordSpeechletRequestStreamHandler() {
		super(new OxfordSpeechlet(), SUPPORTED_APPLICATION_IDS);
	}

	public OxfordSpeechletRequestStreamHandler(Speechlet speechlet, Set<String> supportedApplicationIds) {
		super(speechlet, supportedApplicationIds);
	}

}
