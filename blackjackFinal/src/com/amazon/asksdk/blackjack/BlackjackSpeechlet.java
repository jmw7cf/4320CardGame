/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.amazon.asksdk.blackjack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.OutputSpeech;

public class BlackjackSpeechlet implements SpeechletV2 {
    private static final Logger log = LoggerFactory.getLogger(BlackjackSpeechlet.class);
    Game game;

    @Override
    public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
        log.info("onSessionStarted requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
        game = new Game();
    }

    @Override
    public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
        log.info("onLaunch requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
        return openingStatement();
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
        IntentRequest request = requestEnvelope.getRequest();
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                requestEnvelope.getSession().getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;

        if ("ReadRulesIntent".equals(intentName)) {
            return getRulesResponse();
        } else if ("StartNewGameIntent".equals(intentName)) {
            return dealHand();
        } else if ("HitIntent".equals(intentName)) {
            return hitHand();
        } else if ("StandIntent".equals(intentName)) {
            return standHand();
        } else if ("AMAZON.HelpIntent".equals(intentName)) {
            return getHelpResponse();
        } else {
            return getAskResponse("Blackjack", "This is unsupported.  Please try something else.");
        }
    }

    @Override
    public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
        log.info("onSessionEnded requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
    }

    /**
     * Creates and returns a {@code SpeechletResponse} with a welcome message.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse openingStatement() {
       String speech = "Would you like to start a new game or listen to the rules";
       return getAskResponse("Opening Statement", speech);
   }

   private SpeechletResponse getRulesResponse(){
        String speech =
        "This game uses a standard 52 card deck. " +
        "Each participant attempts to beat the dealer by getting a count as close to 21 as possible, without going over 21. " +
        "The dealer gives one card face up to each player in rotation clockwise, and then one card face up to himself. " +
        "Another round of cards is then dealt face up to each player, but the dealer takes his second card face down. " +
        "Thus, each player except the dealer receives two cards face up, and the dealer receives one card face up and one card face down. " +
        "The player to the left goes first and must decide whether to stand or hit. " +
        "Thus, a player may stand on the two cards originally dealt him, or he may ask the dealer for additional cards, one at a time, until he either decides to stand on the total , or go bust by exceeding a total of twenty one. " +
        "When the dealer has served every player, his face-down card is turned up. If the total is seventeen or more, he must stand. If the total is sixteen or under, he must take a card. " +
        "He must continue to take cards until the total is 17 or more, at which point the dealer must stand. ";

        return getAskResponse("Rules", speech);
    }

   private SpeechletResponse dealHand() {   //Kelly

        String speechText;
        Result result = game.deal();

        if (result.isTie()) {
            speechText = "Both you and the dealer got Blackjack! You tied! "
                + "Would you like to play again?";
        } else if (result.isPlayerBlackjack()) {
            speechText = "You got Blackjack! You win! Would you like to start a new game or hear the rules?";
        } else if (result.isDealerBlackjack()) {
            speechText = "The dealer got Blackjack! You lose! Would you like to play again?";
        } else if (result.isSoft()) {
            speechText = "You were dealt a soft " + result.getPlayerScore() + ". The dealer is showing " + result.getDealerShowing() +
                ". Would you like to hit or stand?";
        } else {
            speechText = "You were dealt a " + result.getPlayerScore() + ". The dealer is showing " + result.getDealerShowing() +
                ". Would you like to hit or stand?";
        }

        return getAskResponse("Deal", speechText);
    }

    private SpeechletResponse hitHand() {   //Kelly

        String speechText;
        Result result = game.hit();

        if (result.isPlayerBust()) {
            speechText = "You busted! You lose! Would you like to start a new game or hear the rules?";
        } else if (result.isSoft()) {
            speechText = "You were dealt a soft " + result.getPlayerScore() + ". The dealer is showing " + result.getDealerShowing() +
                ". Would you like to hit or stand?";
        } else {
            speechText = "You were dealt a " + result.getPlayerScore() + ". The dealer is showing " + result.getDealerShowing() +
                ". Would you like to hit or stand?";
        }

        return getAskResponse("Hit", speechText);
    }

    private SpeechletResponse standHand() {   //Kelly

        String speechText;
        Result result = game.stand();

        if (result.isDealerBust()) {
            speechText = "The dealer has busted! You win! Would you like to start a new game or hear the rules?";
        } else if (result.isTie()) {
            speechText = "You and the dealer tied with a score of " + result.getPlayerScore() +
                ". Would you like to start a new game or hear the rules?";
        } else if (result.isPlayerWin()) {
            speechText = "You won! Your score was " + result.getPlayerScore() + " and the dealer's score was " + result.getDealerFinalScore() +
                ". Would you like to start a new game or hear the rules?";
        } else {  //dealer wins
            speechText = "You lost! Your score was " + result.getPlayerScore() + " and the dealer's score was " + result.getDealerFinalScore() +
                ". Would you like to start a new game or hear the rules?";
        }

        return getAskResponse("Stand", speechText);
    }

    /**
     * Creates a {@code SpeechletResponse} for the help intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelpResponse() {
        String speechText = "You can ask Blackjack to deal, ask for how to play, or, you can say exit. What can I "
                        + "help you with?";
        return getAskResponse("Blackjack", speechText);
    }

    /**
     * Helper method that creates a card object.
     * @param title title of the card
     * @param content body of the card
     * @return SimpleCard the display card to be sent along with the voice response.
     */
    private SimpleCard getSimpleCard(String title, String content) {
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(content);

        return card;
    }

    /**
     * Helper method for retrieving an OutputSpeech object when given a string of TTS.
     * @param speechText the text that should be spoken out to the user.
     * @return an instance of SpeechOutput.
     */
    private PlainTextOutputSpeech getPlainTextOutputSpeech(String speechText) {
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        return speech;
    }

    /**
     * Helper method that returns a reprompt object. This is used in Ask responses where you want
     * the user to be able to respond to your speech.
     * @param outputSpeech The OutputSpeech object that will be said once and repeated if necessary.
     * @return Reprompt instance.
     */
    private Reprompt getReprompt(OutputSpeech outputSpeech) {
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(outputSpeech);

        return reprompt;
    }

    /**
     * Helper method for retrieving an Ask response with a simple card and reprompt included.
     * @param cardTitle Title of the card that you want displayed.
     * @param speechText speech text that will be spoken to the user.
     * @return the resulting card and speech text.
     */
    private SpeechletResponse getAskResponse(String cardTitle, String speechText) {
        SimpleCard card = getSimpleCard(cardTitle, speechText);
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText);
        Reprompt reprompt = getReprompt(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }
}
