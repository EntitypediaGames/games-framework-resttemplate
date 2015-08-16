package org.entitypedia.games.gameframework.client.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.entitypedia.games.common.client.GamesCommonClient;
import org.entitypedia.games.common.exceptions.GameException;
import org.entitypedia.games.common.model.ResultsPage;
import org.entitypedia.games.gameframework.client.IGamesFrameworkClient;
import org.entitypedia.games.gameframework.common.api.*;
import org.entitypedia.games.gameframework.common.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth.consumer.OAuthConsumerToken;
import org.springframework.security.oauth.consumer.OAuthSecurityContextHolder;
import org.springframework.security.oauth.consumer.OAuthSecurityContextImpl;
import org.springframework.security.oauth.consumer.ProtectedResourceDetails;
import org.springframework.security.oauth.consumer.client.OAuthRestTemplate;
import org.springframework.security.oauth.consumer.token.OAuthConsumerTokenServices;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;

/**
 * Extends OAuthRestTemplate with proper authentication use.
 *
 * @author <a href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public class GamesFrameworkRESTTemplate extends OAuthRestTemplate implements IGamesFrameworkClient {

    private static final Logger log = LoggerFactory.getLogger(GamesFrameworkRESTTemplate.class);

    // nice... type erasure leads to the necessity to reimplement super type tokens
    // by every decent library following the same Neal Gafter's solution
    private static final ParameterizedTypeReference<Player> PLAYER_TYPE_REFERENCE = new ParameterizedTypeReference<Player>() {
    };
    private static final ParameterizedTypeReference<ResultsPage<Player>> PLAYERS_RP_TYPE_REFERENCE = new ParameterizedTypeReference<ResultsPage<Player>>() {
    };
    private static final ParameterizedTypeReference<Clue> CLUE_TYPE_REFERENCE = new ParameterizedTypeReference<Clue>() {
    };
    private static final ParameterizedTypeReference<Word> WORD_TYPE_REFERENCE = new ParameterizedTypeReference<Word>() {
    };
    private static final ParameterizedTypeReference<ResultsPage<Word>> WORDS_RP_TYPE_REFERENCE = new ParameterizedTypeReference<ResultsPage<Word>>() {
    };
    private static final ParameterizedTypeReference<ResultsPage<Clue>> CLUES_RP_TYPE_REFERENCE = new ParameterizedTypeReference<ResultsPage<Clue>>() {
    };
    private static final ParameterizedTypeReference<Feedback> FEEDBACK_TYPE_REFERENCE = new ParameterizedTypeReference<Feedback>() {
    };
    private static final ParameterizedTypeReference<ClueTemplate> CLUE_TEMPLATE_TYPE_REFERENCE = new ParameterizedTypeReference<ClueTemplate>() {
    };
    private static final ParameterizedTypeReference<ResultsPage<ClueTemplate>> CLUE_TEMPLATES_RP_TYPE_REFERENCE = new ParameterizedTypeReference<ResultsPage<ClueTemplate>>() {
    };
    private static final ParameterizedTypeReference<Developer> DEVELOPER_TYPE_REFERENCE = new ParameterizedTypeReference<Developer>() {
    };
    private static final ParameterizedTypeReference<ResultsPage<Developer>> DEVELOPERS_RP_TYPE_REFERENCE = new ParameterizedTypeReference<ResultsPage<Developer>>() {
    };
    private static final ParameterizedTypeReference<Game> GAME_TYPE_REFERENCE = new ParameterizedTypeReference<Game>() {
    };
    private static final ParameterizedTypeReference<ResultsPage<Game>> GAMES_RP_TYPE_REFERENCE = new ParameterizedTypeReference<ResultsPage<Game>>() {
    };

    private OAuthConsumerTokenServices tokenServices;
    private ProtectedResourceDetails resource;

    private HttpClient httpClient;

    private String frameworkAPIRoot;
    private String frameworkSecureAPIRoot;

    private ResponseErrorHandler responseErrorHandler = new ThrowingResponseErrorHandler();

    public GamesFrameworkRESTTemplate(ProtectedResourceDetails resource) {
        super(resource);
        this.resource = resource;
    }

    private void loadOAuthSecurityContext() {
        log.debug("Loading OAuthSecurityContext for resource {}", resource.getId());
        OAuthConsumerToken token = getTokenServices().getToken(resource.getId());
        if (null == token) {
            log.debug("Token not found. Clearing OAuthSecurityContext for resource {}", resource.getId());
            OAuthSecurityContextHolder.setContext(null);
        } else {
            OAuthSecurityContextImpl context = new OAuthSecurityContextImpl();
            Map<String, OAuthConsumerToken> accessTokens = new TreeMap<>();

            accessTokens.put(resource.getId(), token);
            context.setAccessTokens(accessTokens);
            OAuthSecurityContextHolder.setContext(context);
            log.debug("Loaded OAuthSecurityContext for resource {} with token {}", resource.getId(), token.getValue());
        }
    }

    private void clearOAuthSecurityContext() {
        log.debug("Clearing OAuthSecurityContext for resource {}", resource.getId());
        OAuthSecurityContextHolder.setContext(null);
    }

    @Override
    protected <T> T doExecute(URI url, HttpMethod method, RequestCallback requestCallback, ResponseExtractor<T> responseExtractor) throws RestClientException {
        try {
            loadOAuthSecurityContext();
            return super.doExecute(url, method, requestCallback, responseExtractor);
        } finally {
            clearOAuthSecurityContext();
        }
    }

    public OAuthConsumerTokenServices getTokenServices() {
        return tokenServices;
    }

    public void setTokenServices(OAuthConsumerTokenServices tokenServices) {
        this.tokenServices = tokenServices;
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        setErrorHandler(responseErrorHandler);
        setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    private class ThrowingResponseErrorHandler implements ResponseErrorHandler {

        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException {
            return response.getStatusCode().value() != 200;
        }

        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            throw GamesCommonClient.processError(response.getBody(), mapper);
        }
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String getFrameworkAPIRoot() {
        return frameworkAPIRoot;
    }

    public void setFrameworkAPIRoot(String frameworkAPIRoot) {
        this.frameworkAPIRoot = frameworkAPIRoot;
    }

    public String getFrameworkSecureAPIRoot() {
        return frameworkSecureAPIRoot;
    }

    public void setFrameworkSecureAPIRoot(String frameworkSecureAPIRoot) {
        this.frameworkSecureAPIRoot = frameworkSecureAPIRoot;
    }

    @Override
    public Clue readClue(long clueID) {
        try {
            ResponseEntity<Clue> responseEntity = exchange(
                    new URI(frameworkAPIRoot + IClueAPI.READ_CLUE.replaceAll("\\{.*\\}", Long.toString(clueID))),
                    HttpMethod.GET, HttpEntity.EMPTY, CLUE_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public ResultsPage<Clue> listClues(Integer pageSize, Integer pageNo, String filter, String order) {
        try {
            ResponseEntity<ResultsPage<Clue>> responseEntity = exchange(
                    new URI(GamesCommonClient.addPageSizeAndNoAndFilterAndOrder(
                            frameworkAPIRoot + IClueAPI.LIST_CLUES + "?", pageSize, pageNo,
                            URLEncoder.encode(filter, "UTF-8"), order)),
                    HttpMethod.GET, HttpEntity.EMPTY, CLUES_RP_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void login() {
        try {
            getForObject(new URI(frameworkAPIRoot + IPlayerAPI.LOGIN_PLAYER), Void.class);
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public Player loginFacebook(String token) {
        try {
            ResponseEntity<Player> responseEntity = exchange(
                    new URI(frameworkSecureAPIRoot + IPlayerAPI.LOGIN_FACEBOOK_PLAYER + "?token=" +
                            URLEncoder.encode(token, "UTF-8")),
                    HttpMethod.POST, HttpEntity.EMPTY, PLAYER_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public Player loginGPlus(String code) {
        try {
            ResponseEntity<Player> responseEntity = exchange(
                    new URI(frameworkSecureAPIRoot + IPlayerAPI.LOGIN_GPLUS_PLAYER + "?code=" + URLEncoder.encode(code, "UTF-8")),
                    HttpMethod.POST, HttpEntity.EMPTY, PLAYER_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void activateEmail(String code) {
        try {
            postForObject(new URI(frameworkAPIRoot + IPlayerAPI.ACTIVATE_PLAYER_EMAIL + "?code=" +
                    URLEncoder.encode(code, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void requestEmailActivation() {
        try {
            postForObject(new URI(frameworkAPIRoot + IPlayerAPI.REQUEST_PLAYER_EMAIL_ACTIVATION), null, Void.class);
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void resetPassword(String code, String password) {
        try {
            postForObject(new URI(frameworkSecureAPIRoot + IPlayerAPI.RESET_PLAYER_PASSWORD + "?code=" +
                    URLEncoder.encode(code, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void requestPasswordReset(String email) {
        try {
            postForObject(new URI(frameworkAPIRoot + IPlayerAPI.REQUEST_PLAYER_PASSWORD_RESET + "?email=" +
                    URLEncoder.encode(email, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public Player createPlayer(Player player) {
        try {
            ResponseEntity<Player> responseEntity = exchange(
                    new URI(frameworkSecureAPIRoot + IPlayerAPI.CREATE_PLAYER),
                    HttpMethod.POST, new HttpEntity<>(player), PLAYER_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public Player readPlayer(String playerID) {
        try {
            ResponseEntity<Player> responseEntity = exchange(
                    new URI(frameworkAPIRoot + IPlayerAPI.READ_PLAYER.replaceAll("\\{.*\\}", playerID)),
                    HttpMethod.GET, HttpEntity.EMPTY, PLAYER_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public Player readPlayer(long playerID) {
        try {
            ResponseEntity<Player> responseEntity = exchange(
                    new URI(frameworkAPIRoot + IPlayerAPI.READ_PLAYER.replaceAll("\\{.*\\}", Long.toString(playerID))),
                    HttpMethod.GET, HttpEntity.EMPTY, PLAYER_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void deletePlayer(long playerID) {
        try {
            postForObject(new URI(frameworkAPIRoot + IPlayerAPI.DELETE_PLAYER + "?playerID=" + Long.toString(playerID)),
                    null, Void.class);
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updatePlayer(Player player) {
        try {
            postForObject(new URI(frameworkSecureAPIRoot + IPlayerAPI.UPDATE_PLAYER), player, Void.class);
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updatePlayerPassword(long playerID, String password) {
        try {
            postForObject(new URI(frameworkSecureAPIRoot + IPlayerAPI.UPDATE_PLAYER_PASSWORD + "?playerID=" + Long.toString(playerID)
                    + "&password=" + URLEncoder.encode(password, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updatePlayerEmail(long playerID, String email) {
        try {
            postForObject(new URI(frameworkSecureAPIRoot + IPlayerAPI.UPDATE_PLAYER_EMAIL + "?playerID=" + Long.toString(playerID)
                    + "&email=" + URLEncoder.encode(email, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updatePlayerFirstName(long playerID, String firstName) {
        try {
            postForObject(new URI(frameworkAPIRoot + IPlayerAPI.UPDATE_PLAYER_FIRST_NAME + "?playerID=" + Long.toString(playerID)
                    + "&firstName=" + URLEncoder.encode(firstName, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updatePlayerLastName(long playerID, String lastName) {
        try {
            postForObject(new URI(frameworkAPIRoot + IPlayerAPI.UPDATE_PLAYER_LAST_NAME + "?playerID=" + Long.toString(playerID)
                    + "&lastName=" + URLEncoder.encode(lastName, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updatePlayerFacebook(long playerID, String token) {
        try {
            postForObject(new URI(frameworkAPIRoot + IPlayerAPI.UPDATE_PLAYER_FACEBOOK + "?playerID=" + Long.toString(playerID)
                    + "&token=" + URLEncoder.encode(token, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updatePlayerGPlus(long playerID, String code) {
        try {
            postForObject(new URI(frameworkAPIRoot + IPlayerAPI.UPDATE_PLAYER_GPLUS + "?playerID=" + Long.toString(playerID)
                    + "&code=" + URLEncoder.encode(code, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public ResultsPage<Player> listPlayers(Integer pageSize, Integer pageNo) {
        try {
            ResponseEntity<ResultsPage<Player>> responseEntity = exchange(
                    new URI(GamesCommonClient.addPageSizeAndNo(frameworkAPIRoot + IPlayerAPI.LIST_PLAYERS + "?", pageSize, pageNo)),
                    HttpMethod.GET, HttpEntity.EMPTY, PLAYERS_RP_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public Word readWord(long wordID) {
        try {
            ResponseEntity<Word> responseEntity = exchange(
                    new URI(frameworkAPIRoot + IWordAPI.READ_WORD.replaceAll("\\{.*\\}", Long.toString(wordID))),
                    HttpMethod.GET, HttpEntity.EMPTY, WORD_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public ResultsPage<Word> listWords(Integer pageSize, Integer pageNo, String filter, String order) {
        try {
            ResponseEntity<ResultsPage<Word>> responseEntity = exchange(
                    new URI(GamesCommonClient.addPageSizeAndNoAndFilterAndOrder(frameworkAPIRoot +
                            IWordAPI.LIST_WORDS + "?", pageSize, pageNo, URLEncoder.encode(filter, "UTF-8"), order)),
                    HttpMethod.GET, HttpEntity.EMPTY, WORDS_RP_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public String getApiEndpoint() {
        return frameworkAPIRoot;
    }

    @Override
    public void setApiEndpoint(String apiEndpoint) {
        frameworkAPIRoot = apiEndpoint;
    }

    @Override
    public boolean getSignConnection() {
        return false;
    }

    @Override
    public void setSignConnection(boolean signConnection) {
        // nop
    }

    @Override
    public Feedback createFeedback(long clueID) {
        try {
            ResponseEntity<Feedback> responseEntity = exchange(
                    new URI(frameworkAPIRoot + IFeedbackAPI.CREATE_FEEDBACK + "?clueID=" + Long.toString(clueID)),
                    HttpMethod.POST, HttpEntity.EMPTY, FEEDBACK_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void postFeedback(long feedbackID, int attributePosition, String attributeValue, String comment) {
        try {
            String url = frameworkAPIRoot + IFeedbackAPI.POST_FEEDBACK + "?feedbackID=" + Long.toString(feedbackID)
                    + "&attributePosition=" + Integer.toString(attributePosition);
            if (null != attributeValue) {
                url = url + "&attributeValue=" + URLEncoder.encode(attributeValue, "UTF-8");
            }
            if (null != comment) {
                url = url + "&comment=" + URLEncoder.encode(comment, "UTF-8");
            }

            postForObject(new URI(url), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void cancelFeedback(long feedbackID) {
        try {
            postForObject(new URI(frameworkAPIRoot + IFeedbackAPI.CANCEL_FEEDBACK + "?feedbackID=" + Long.toString(feedbackID)), null, Void.class);
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void confirmClue(long clueID, double confidence) {
        try {
            postForObject(new URI(frameworkAPIRoot + IFeedbackAPI.CONFIRM_CLUE + "?clueID=" + Long.toString(clueID))
                    + "&confidence=" + Double.toString(confidence), null, Void.class);
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public ClueTemplate readClueTemplate(long clueTemplateID) {
        try {
            ResponseEntity<ClueTemplate> responseEntity = exchange(
                    new URI(frameworkAPIRoot + IClueTemplateAPI.READ_CLUE_TEMPLATE.replaceAll("\\{.*\\}", Long.toString(clueTemplateID))),
                    HttpMethod.GET, HttpEntity.EMPTY, CLUE_TEMPLATE_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public ResultsPage<ClueTemplate> listClueTemplates(Integer pageSize, Integer pageNo, String filter, String order) {
        try {
            ResponseEntity<ResultsPage<ClueTemplate>> responseEntity = exchange(
                    new URI(GamesCommonClient.addPageSizeAndNoAndFilterAndOrder(
                            frameworkAPIRoot + IClueTemplateAPI.LIST_CLUE_TEMPLATES + "?", pageSize, pageNo,
                            URLEncoder.encode(filter, "UTF-8"), order)),
                    HttpMethod.GET, HttpEntity.EMPTY, CLUE_TEMPLATES_RP_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void loginDeveloper() {
        try {
            getForObject(new URI(frameworkAPIRoot + IDeveloperAPI.LOGIN_DEVELOPER), Void.class);
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public Developer readDeveloper(long developerID) {
        try {
            ResponseEntity<Developer> responseEntity = exchange(
                    new URI(frameworkAPIRoot + IDeveloperAPI.READ_DEVELOPER.replaceAll("\\{.*\\}", Long.toString(developerID))),
                    HttpMethod.GET, HttpEntity.EMPTY, DEVELOPER_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void resetDeveloperPassword(String code, String password) {
        try {
            postForObject(new URI(frameworkSecureAPIRoot + IDeveloperAPI.RESET_DEVELOPER_PASSWORD + "?code=" +
                    URLEncoder.encode(code, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void requestDeveloperPasswordReset(String email) {
        try {
            postForObject(new URI(frameworkAPIRoot + IDeveloperAPI.REQUEST_DEVELOPER_PASSWORD_RESET + "?email=" +
                    URLEncoder.encode(email, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updateDeveloperPassword(long developerID, String password) {
        try {
            postForObject(new URI(frameworkSecureAPIRoot + IDeveloperAPI.UPDATE_DEVELOPER_PASSWORD + "?developerID=" + Long.toString(developerID)
                    + "&password=" + URLEncoder.encode(password, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updateDeveloperEmail(long developerID, String email) {
        try {
            postForObject(new URI(frameworkSecureAPIRoot + IDeveloperAPI.UPDATE_DEVELOPER_EMAIL + "?developerID=" + Long.toString(developerID)
                    + "&email=" + URLEncoder.encode(email, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updateDeveloperFirstName(long developerID, String firstName) {
        try {
            postForObject(new URI(frameworkAPIRoot + IDeveloperAPI.UPDATE_DEVELOPER_FIRST_NAME + "?developerID=" + Long.toString(developerID)
                    + "&firstName=" + URLEncoder.encode(firstName, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updateDeveloperLastName(long developerID, String lastName) {
        try {
            postForObject(new URI(frameworkAPIRoot + IDeveloperAPI.UPDATE_DEVELOPER_LAST_NAME + "?developerID=" + Long.toString(developerID)
                    + "&lastName=" + URLEncoder.encode(lastName, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public ResultsPage<Developer> listDevelopers(Integer pageSize, Integer pageNo, String filter, String order) {
        try {
            ResponseEntity<ResultsPage<Developer>> responseEntity = exchange(
                    new URI(GamesCommonClient.addPageSizeAndNoAndFilterAndOrder(frameworkAPIRoot +
                            IDeveloperAPI.LIST_DEVELOPERS + "?", pageSize, pageNo, URLEncoder.encode(filter, "UTF-8"), order)),
                    HttpMethod.GET, HttpEntity.EMPTY, DEVELOPERS_RP_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public Game createGame() {
        try {
            ResponseEntity<Game> responseEntity = exchange(
                    new URI(frameworkAPIRoot + IGameAPI.CREATE_GAME),
                    HttpMethod.POST, HttpEntity.EMPTY, GAME_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public Game readGame(long gameID) {
        try {
            ResponseEntity<Game> responseEntity = exchange(
                    new URI(frameworkAPIRoot + IGameAPI.READ_GAME.replaceAll("\\{.*\\}", Long.toString(gameID))),
                    HttpMethod.GET, HttpEntity.EMPTY, GAME_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updateGameTitle(long gameID, String title) {
        try {
            postForObject(new URI(frameworkAPIRoot + IGameAPI.UPDATE_GAME_TITLE + "?gameID=" + Long.toString(gameID)
                    + "&title=" + URLEncoder.encode(title, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updateGameDescription(long gameID, String description) {
        try {
            postForObject(new URI(frameworkAPIRoot + IGameAPI.UPDATE_GAME_DESCRIPTION + "?gameID=" + Long.toString(gameID)
                    + "&description=" + URLEncoder.encode(description, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updateGameURL(long gameID, String url) {
        try {
            postForObject(new URI(frameworkAPIRoot + IGameAPI.UPDATE_GAME_URL + "?gameID=" + Long.toString(gameID)
                    + "&url=" + URLEncoder.encode(url, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updateGameLogoURL(long gameID, String url) {
        try {
            postForObject(new URI(frameworkAPIRoot + IGameAPI.UPDATE_GAME_LOGO_URL + "?gameID=" + Long.toString(gameID)
                    + "&url=" + URLEncoder.encode(url, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updateGameSliderURL(long gameID, String url) {
        try {
            postForObject(new URI(frameworkAPIRoot + IGameAPI.UPDATE_GAME_SLIDER_URL + "?gameID=" + Long.toString(gameID)
                    + "&url=" + URLEncoder.encode(url, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public void updateGameOAuthCallbackURL(long gameID, String url) {
        try {
            postForObject(new URI(frameworkAPIRoot + IGameAPI.UPDATE_GAME_OAUTH_CALLBACK_URL + "?gameID=" + Long.toString(gameID)
                    + "&url=" + URLEncoder.encode(url, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }

    }

    @Override
    public void updateGameOAuthSecret(long gameID, String secret) {
        try {
            postForObject(new URI(frameworkAPIRoot + IGameAPI.UPDATE_GAME_OAUTH_SECRET + "?gameID=" + Long.toString(gameID)
                    + "&secret=" + URLEncoder.encode(secret, "UTF-8")), null, Void.class);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new GameException(e.getMessage(), e);
        }

    }

    @Override
    public void updateGameProduction(long gameID, boolean production) {
        try {
            postForObject(new URI(frameworkAPIRoot + IGameAPI.UPDATE_GAME_PRODUCTION + "?gameID=" + Long.toString(gameID)
                    + "&production=" + Boolean.toString(production)), null, Void.class);
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }

    }

    @Override
    public void deleteGame(long gameID) {
        try {
            postForObject(new URI(frameworkAPIRoot + IGameAPI.DELETE_GAME + "?gameID=" + Long.toString(gameID)),
                    null, Void.class);
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }

    @Override
    public ResultsPage<Game> listGames(Integer pageSize, Integer pageNo) {
        try {
            ResponseEntity<ResultsPage<Game>> responseEntity = exchange(
                    new URI(GamesCommonClient.addPageSizeAndNo(frameworkAPIRoot + IGameAPI.LIST_GAMES + "?", pageSize, pageNo)),
                    HttpMethod.GET, HttpEntity.EMPTY, GAMES_RP_TYPE_REFERENCE);
            return responseEntity.getBody();
        } catch (URISyntaxException e) {
            throw new GameException(e.getMessage(), e);
        }
    }
}