package org.fadil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.amazonaws.util.json.JSONTokener;

/**
 * This class provides the implementation of an AWS Lambda function that will handle requests
 * in the form of {@link Intent} from the user. This function basically has two types of interactions:
 * namely, a one-shot model and a dialog model.
 * 
 * The function communicates with an external web service (https://od-api.oxforddictionaries.com/api/)
 * to get lexical information for a word that the user has requested. The external web service is provided
 * by Oxford Dictionaries and requires registration to obtain an APP_ID and APP_KEY in order to call
 * the secure REST endpoints exposed.
 * 
 * The ons-shot model and the dialog model are based on the examples provided by Amazon in its
 * Java Alexa Skills Kit SDK. They can be found here: https://github.com/alexa/skill-samples-java.
 * 
 * 
 * @author Fadil
 * @version 1.0
 * @since 17/02/2018
 *
 */
public class OxfordSpeechlet implements SpeechletV2 {
	
	private static final String UNDEFINED = "undefined";
	private static final String UNCATEGORIZED = "uncategorized";
	private static final String SLOT_WORD = "Word";
	private static final Logger LOG = LoggerFactory.getLogger(OxfordSpeechlet.class);
	private static final String ENDPOINT = "https://od-api.oxforddictionaries.com/api/v1/entries/en/";
	
	/**
	 * {@inheritDoc}
	 */
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
				
			case "AMAZON.StopIntent":
				return handleExitRequest(intent);
				
			case "AMAZON.CancelIntent":
				return handleExitRequest(intent);
				
			case "AMAZON.YesIntent":
				return handleYesForExamplesRequest(intent, session);
				
			case "AMAZON.NoIntent":
				return handleExitRequest(intent);				
				
			default:
				return handleUnsupportedRequest();
		}		
	}

	/**
	 * Creates {@code SpeechletResponse} for the intent and get the examples of the word from the Session.
	 * @param intent
	 * @param session
	 * @return SpeechletResponse of each example spoken and visual response for the AMAZON.YesIntent 
	 */
	private SpeechletResponse handleYesForExamplesRequest(Intent intent, Session session) {
		StringBuilder examplesBuilder = new StringBuilder();
		
		Object sessionObj = session.getAttribute("EXAMPLES");
		if (sessionObj instanceof List) {
			@SuppressWarnings("unchecked")
			List<String> examples = (List<String>) session.getAttribute("EXAMPLES");
			
			for (int i = 0; i < examples.size(); i++) {
				examplesBuilder.append("Example " + i+1)
							   .append(": ")
							   .append(examples.get(i))
							   .append(". ");
			}
		}
		
		String examplesSpeech = examplesBuilder.toString();
		SimpleCard card = new SimpleCard();
		card.setTitle("Oxford Word Look Up");
		card.setContent(examplesSpeech);
		PlainTextOutputSpeech examplesSpeechOutput = new PlainTextOutputSpeech();
		examplesSpeechOutput.setText(examplesSpeech);
		
		return SpeechletResponse.newTellResponse(examplesSpeechOutput, card);
	}

	/**
	 * Returns a {@code SpeechletResponse} to the user when an invalid request is made.
	 * @return a SpeechletResponse that Alexa will speak to the user
	 */
	private SpeechletResponse handleUnsupportedRequest() {
		String errorSpeech = "This is unsupported. Please try something else.";
        return newAskResponse(errorSpeech, errorSpeech);
	}

	/**
	 * Returns a {@code SpeechletResponse} to the user when a Stop, Cancel or No to examples requests are made.
	 * @param intent
	 * @return a SpeechletResponse that Alexa will speak to the user
	 */
	private SpeechletResponse handleExitRequest(Intent intent) {
		String goodbyeSpeech = "Alright. Thank you for using Oxford Word Look up.";
		SimpleCard card = new SimpleCard();
		card.setTitle("Oxford Word Look Up");
		card.setContent(goodbyeSpeech);
		PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(goodbyeSpeech);
        
        return SpeechletResponse.newTellResponse(outputSpeech, card);
	}

	/**
	 * Returns a {@code SpeechletResponse} to the user when using the Dialog model.
	 * The Dialog model calls the One-shot model because only one parameter is being passed for now.
	 * @param intent
	 * @param session
	 * @return a SpeechletResponse that Alexa will use to speak to the user to get the word
	 */
	private SpeechletResponse handleDialogOxfordRequest(Intent intent, Session session) {
		Slot wordSlot = intent.getSlot(SLOT_WORD);
		String word = wordSlot.getValue();
		if (word != null) {
			return handleOneshotOxfordRequest(intent, session);
		} else {
			return handleNoSlotDialogRequest(intent, session);
		}
	}

	/**
	 * Returns a {@code SpeechletResponse} to the user when no input is provided.
	 * @param intent
	 * @param session
	 * @return a SpeechletResponse that Alexa will use to query the user again for the word
	 */
	private SpeechletResponse handleNoSlotDialogRequest(Intent intent, Session session) {
		String speechOutput = "Please try again by saying a word.";		
		return newAskResponse(speechOutput, speechOutput);
	}

	/**
	 * Returns a {@code SpeechletResponse} to the user when using the One-shot model.
	 * @param intent
	 * @param session
	 * @return a SpeechletResponse object that Alexa will use to return to the user
	 */
	private SpeechletResponse handleOneshotOxfordRequest(Intent intent, Session session) {
		Slot wordSlot = intent.getSlot(SLOT_WORD);
		String word = wordSlot.getValue();
		
		StringBuilder builder = callOxfordService(word);
		
		String speechOutput = "";
		String repromptText = "";
		WordDetails wordDetails = null;
		
		if (builder.length() == 0) {
			speechOutput = "Sorry, the Oxford service is experiencing a problem. "
								+ "Please try again later.";
		} else {
			try {
				JSONObject oxfordResponseObject = new JSONObject(new JSONTokener(builder.toString()));
				wordDetails = retrieveWordDetails(oxfordResponseObject);
				speechOutput = buildSpeechOutput(speechOutput, word, wordDetails);
				
			} catch (JSONException e) {
				LOG.error("Exception occured while parsing service response.", e);
			}
		}
		
		List<String> examples = wordDetails.getExamples();
		boolean hasExamples = hasExamples(examples);
		if (hasExamples) {
			setExamplesInSession(intent, session, examples);
			speechOutput = speechOutput 
					+ " I've found some examples for "
					+ word
					+ "."
					+ " Would you like to hear them?";
		}
		
		SimpleCard card = new SimpleCard();
		card.setTitle("Oxford Word Look Up");
		card.setContent(speechOutput);
		PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
		outputSpeech.setText(speechOutput);
		
		repromptText = "I'm sorry, I didn't understand what you said. "
				+ "Would you like to hear some examples?";
		
		return hasExamples ? newAskResponse(speechOutput, repromptText) : SpeechletResponse.newTellResponse(outputSpeech, card);
	}
	
	/**
	 * Stores the extracted examples in the Session.
	 * @param intent
	 * @param session
	 * @param examples
	 */
	private void setExamplesInSession(final Intent intent, final Session session, List<String> examples) {		
		session.setAttribute("EXAMPLES", examples);		
	}

	/**
	 * Method to check if the word has some examples with it.
	 * @param examples
	 * @return true if the word has some examples
	 * 				false if not examples found
	 */
	private boolean hasExamples(List<String> examples) {
		return examples != null && !examples.isEmpty();
	}

	/**
	 * Method to build the speech for Alexa to speak.
	 * @param wordDetails
	 * @return the speech built from the information gathered in the {@link WordDetails} object
	 */
	private String buildSpeechOutput(String speechOutput, String word, WordDetails wordDetails) {
		Optional<String> optLexicalCategory = wordDetails.getLexicalCategory();
		Optional<String> optdefinition = wordDetails.getDefinition();
		
		String lexicalCategory = optLexicalCategory.orElse(UNCATEGORIZED);
		String definition = optdefinition.orElse(UNDEFINED);
		
		String lexicalCategorySpeechOuput = UNCATEGORIZED.equals(lexicalCategory) ?
				new StringBuilder().append(word)
									.append(" has not been classified in any lexical category")
									.append(". ")
									.toString()									
				: new StringBuilder().append(word)
									  .append(" is ")
									  .append(startsWithAVowel(lexicalCategory) ? "an " : "a ")
									  .append(lexicalCategory)
									  .append(". ")
									  .toString();
		
		String definitionSpeechOuput = UNDEFINED.equals(definition) ?
				new StringBuilder().append("Sorry. ")
									.append("I could not find any definition for the word ")
									.append(word)
									.append(".")
									.toString()
				: new StringBuilder().append(word)
									  .append(" means ")
									  .append(definition)
									  .append(".")
									  .toString();
									
		speechOutput = lexicalCategorySpeechOuput 
				+ " " 
				+ definitionSpeechOuput;										
		
		return speechOutput;
	}
	
	/**
	 * @param text
	 * @return true if a word begins with a vowel
	 * 				false if word begins with a consonant
	 */
	private boolean startsWithAVowel(String text) {
		return text.startsWith("A") || text.startsWith("a")
					|| text.startsWith("E") || text.startsWith("e")
					|| text.startsWith("I") || text.startsWith("i")
					|| text.startsWith("O") || text.startsWith("o")
					|| text.startsWith("U") || text.startsWith("U");
	}

	/**
	 * Calls the Oxford service by passing the APP_ID and APP_KEY as header parameters in the request.
	 * APP_ID and APP_KEY are obtained when registering for an account on "https://developer.oxforddictionaries.com/".
	 * @param word
	 * @return StringBuilder object containing the JSONObject response
	 */
	private StringBuilder callOxfordService(String word) {
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
		return builder;
	}

	/**
	 * Retrieves lexical category, definition and examples of a word from the JSONObject
	 * response obtained from the call to "https://od-api.oxforddictionaries.com/api/v1/entries/en/{Word}" 
	 * @param oxfordResponseObject
	 * @return WordDetails object containing the information retrieved from the JSONObject response
	 * @throws JSONException
	 */
	private WordDetails retrieveWordDetails(JSONObject oxfordResponseObject) throws JSONException {
		Optional<String> lexicalCategory = null;
		Optional<String> definition = null;
		List<String> examples = new ArrayList<>();
		
		if (oxfordResponseObject != null) {
			JSONArray results = oxfordResponseObject.has("results") ?
					(JSONArray) oxfordResponseObject.get("results") : null;
			
			JSONArray lexicalEntries = results != null && results.getJSONObject(0).has("lexicalEntries") ?
					(JSONArray) results.getJSONObject(0).get("lexicalEntries") : null;
			
			if (lexicalEntries != null) {
				lexicalCategory = lexicalEntries.getJSONObject(0).has("lexicalCategory") ?
						Optional.of((String) lexicalEntries.getJSONObject(0).get("lexicalCategory")) : null;
						
				JSONArray entries = lexicalEntries.getJSONObject(0).has("entries") ?
						(JSONArray) lexicalEntries.getJSONObject(0).get("entries") : null;
						
				JSONArray senses = entries != null && entries.getJSONObject(0).has("senses") ? 
									(JSONArray) entries.getJSONObject(0).get("senses") : null;
				
				if (senses != null) {
					JSONArray definitionsArray = senses.getJSONObject(0).has("definitions") ?
							(JSONArray) senses.getJSONObject(0).get("definitions") : null;
					definition = !definitionsArray.isNull(0) ? Optional.of(definitionsArray.getString(0)) : null;
					
					JSONArray examplesArray = senses.getJSONObject(0).has("examples") ?
							(JSONArray) senses.getJSONObject(0).get("examples") : null;
					if (examplesArray != null && examplesArray.length() > 0)
					for (int i = 0; i < examplesArray.length(); i++) {
						examples.add((String) examplesArray.getJSONObject(i).get("text"));
					}
				}
			}			
		}
		
		return new WordDetails(lexicalCategory, definition, examples);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
		LOG.info("onLaunch requestId={}, sessionId={}",
					requestEnvelope.getRequest().getRequestId(),
					requestEnvelope.getSession().getSessionId());
		
		return getWelcomeResponse();
	}
	
	/**
	 * Creates a {@code SpeechletResponse} object for the Dialog model.
	 * @return SpeechletResponse spoken at the beginning of Dialog model interaction with the user
	 */
	private SpeechletResponse getWelcomeResponse() {
		String whatWordPrompt = "What word would you like information for?";
		String speechOutput = "<speak>"
								+ "Welcome. This word look up service is provided by Oxford University Press. "
								+ whatWordPrompt
								+ "</speak>";
		String repromptText = "I can provide you information for any specific word. "
								+ "You can simply open Oxford Word Look up and ask a question like, "
								+ "what is the meaning of, and say the word you are looking for. "
								+ whatWordPrompt;
		
		return newAskResponse(speechOutput, true, repromptText, false);
	}
	
	/**
     * Wrapper for creating the Ask response from the input strings with
     * plain text output and reprompt speeches.
     *
     * @param stringOutput
     *            the output to be spoken
     * @param repromptText
     *            the reprompt for if the user doesn't reply or is misunderstood.
     * @return SpeechletResponse the speechlet response
     */
	private SpeechletResponse newAskResponse(String stringOutput, String repromptText) {
        return newAskResponse(stringOutput, false, repromptText, false);
    }
	
	/**
     * Wrapper for creating the Ask response from the input strings.
     *
     * @param stringOutput
     *            the output to be spoken
     * @param isOutputSsml
     *            whether the output text is of type SSML
     * @param repromptText
     *            the reprompt for if the user doesn't reply or is misunderstood.
     * @param isRepromptSsml
     *            whether the reprompt text is of type SSML
     * @return SpeechletResponse the speechlet response
     */
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
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
		LOG.info("onSessionEnded requestId={}, sessionId={}",
					requestEnvelope.getRequest().getRequestId(),
					requestEnvelope.getSession().getSessionId());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
		LOG.info("onSessionStarted requestId={}, sessionId={}",
					requestEnvelope.getRequest().getRequestId(),
					requestEnvelope.getSession().getSessionId());
	}
	
}
