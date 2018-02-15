package org.fadil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SsmlOutputSpeech;

public class OxfordSpeechlet implements SpeechletV2 {
	
	private static final Logger LOG = LoggerFactory.getLogger(OxfordSpeechlet.class);
	private static final String ENDPOINT = "https://od-api.oxforddictionaries.com/api/v1/entries/en/";

	@Override
	public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
		IntentRequest request = requestEnvelope.getRequest(); 
		Session session = requestEnvelope.getSession();
		LOG.info("onIntent requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());
		
		Intent intent = request.getIntent();
		String intentName = intent.getName();
		
		switch (intentName) {
		
			case "OneshotOxfordIntent":
				return handleOneshotOxfordRequest(intent, session);
				
			case "DialogOxfordIntent":
				return handleDialogOxfordRequest(intent, session);
				
			case "SupportedLanguagesIntent":
				return handleSupportedLanguagesRequest(intent, session);
				
			case "AMAZON.StopIntent":
				return handleStopRequest(intent);
				
			case "AMAZON.CancelIntent":
				return handleCancelRequest(intent);
				
			default:
				return handleUnsupportedRequest();
		}
		
	}

	private SpeechletResponse handleUnsupportedRequest() {
		// TODO Auto-generated method stub
		return null;
	}

	private SpeechletResponse handleCancelRequest(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	private SpeechletResponse handleStopRequest(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	private SpeechletResponse handleSupportedLanguagesRequest(Intent intent, Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	private SpeechletResponse handleDialogOxfordRequest(Intent intent, Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	private SpeechletResponse handleOneshotOxfordRequest(Intent intent, Session session) {
		Slot wordSlot = intent.getSlot("Word");
		String word = wordSlot.getValue();
		String queryString = ENDPOINT + word;
		
		InputStreamReader inputStream = null;
		BufferedReader bufferedReader = null;
		StringBuilder builder = new StringBuilder();
		
		try {
			URL url = new URL(queryString);
			URLConnection urlConnection = url.openConnection();
			urlConnection.setRequestProperty("app_id", "85c67723");
			urlConnection.setRequestProperty("app_key", "b805130051964d05b144b7bd63b6a15a");
			
			inputStream = new InputStreamReader(urlConnection.getInputStream());
			bufferedReader = new BufferedReader(inputStream);
			
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				builder.append(line);
			}
		} catch (Exception e) {
			builder.setLength(0);
		} finally {
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(bufferedReader);
		}
		
		return null;
	}

	@Override
	public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
		LOG.info("onLaunch requestId={}, sessionId={}",
					requestEnvelope.getRequest().getRequestId(),
					requestEnvelope.getSession().getSessionId());
		
		return getWelcomeResponse();
	}
	
	private SpeechletResponse getWelcomeResponse() {
		String whatWordPrompt = "What word would you like information for?";
		String speechOutput = "<speak>"
								+ "Welcome to Oxford Word Look up. "
								+ whatWordPrompt
								+ "</speak>";
		String repromptText = "I can provide you information for any specific word. "
								+ "You can simply open Oxford Word Look up and ask a question like, "
								+ "what is the meaning of and say the word you are looking for. "
								+ "For a list of supported languages, ask what languages are supported. "
								+ whatWordPrompt;
		
		return newAskResponse(speechOutput, true, repromptText, false);
	}
	
	private SpeechletResponse newAskResponse(String stringOutput, String repromptText) {
        return newAskResponse(stringOutput, false, repromptText, false);
    }
	
	private SpeechletResponse newAskResponse(String stringOutput, boolean isOutputSsml,
            String repromptText, boolean isRepromptSsml) {
        OutputSpeech outputSpeech, repromptOutputSpeech;
        if (isOutputSsml) {
            outputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) outputSpeech).setSsml(stringOutput);
        } else {
            outputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) outputSpeech).setText(stringOutput);
        }

        if (isRepromptSsml) {
            repromptOutputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) repromptOutputSpeech).setSsml(stringOutput);
        } else {
            repromptOutputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) repromptOutputSpeech).setText(repromptText);
        }

        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptOutputSpeech);
        return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
    }

	@Override
	public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
		LOG.info("onSessionEnded requestId={}, sessionId={}",
					requestEnvelope.getRequest().getRequestId(),
					requestEnvelope.getSession().getSessionId());
	}

	@Override
	public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
		LOG.info("onSessionStarted requestId={}, sessionId={}",
					requestEnvelope.getRequest().getRequestId(),
					requestEnvelope.getSession().getSessionId());
	}

}
