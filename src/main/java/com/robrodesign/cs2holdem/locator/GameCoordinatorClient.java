package com.robrodesign.cs2holdem.locator;

import in.dragonbra.javasteam.base.ClientMsgProtobuf;
import in.dragonbra.javasteam.base.gc.ClientGCMsgProtobuf;
import in.dragonbra.javasteam.enums.EMsg;
import in.dragonbra.javasteam.enums.EResult;
import in.dragonbra.javasteam.protobufs.cs.Cstrike15Gcmessages.CDataGCCStrike15_v2_MatchInfo;
import in.dragonbra.javasteam.protobufs.cs.Cstrike15Gcmessages.CMsgGCCStrike15_v2_ClientLogonFatalError;
import in.dragonbra.javasteam.protobufs.cs.Cstrike15Gcmessages.CMsgGCCStrike15_v2_MatchList;
import in.dragonbra.javasteam.protobufs.cs.Cstrike15Gcmessages.CMsgGCCStrike15_v2_MatchListRequestFullGameInfo;
import in.dragonbra.javasteam.protobufs.cs.Cstrike15Gcmessages.ECsgoGCMsg;
import in.dragonbra.javasteam.protobufs.cs.GcsdkGcmessages.CMsgClientHello;
import in.dragonbra.javasteam.protobufs.cs.Gcsystemmsgs.EGCBaseClientMsg;
import in.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver.CMsgClientGamesPlayed;
import in.dragonbra.javasteam.steam.authentication.AuthPollResult;
import in.dragonbra.javasteam.steam.authentication.AuthSession;
import in.dragonbra.javasteam.steam.authentication.AuthSessionDetails;
import in.dragonbra.javasteam.steam.authentication.AuthenticationException;
import in.dragonbra.javasteam.steam.authentication.UserConsoleAuthenticator;
import in.dragonbra.javasteam.steam.handlers.steamgamecoordinator.SteamGameCoordinator;
import in.dragonbra.javasteam.steam.handlers.steamgamecoordinator.callback.MessageCallback;
import in.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails;
import in.dragonbra.javasteam.steam.handlers.steamuser.SteamUser;
import in.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback;
import in.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback;
import in.dragonbra.javasteam.steam.steamclient.SteamClient;
import in.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager;
import in.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback;
import in.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Keeps a bot account logged in to Steam and talks to CS2's Game Coordinator (GC).
 *
 * The GC is Valve's own server for CS2. It is the only place that knows where a match's demo is
 * stored, and it only answers a logged-in Steam client that is "playing" CS2. So this class does
 * what the real Steam client does, in this order:
 *
 *   connect to Steam -> log in -> tell Steam we play CS2 (app 730) -> say hello to the GC
 *   -> the GC answers "welcome" -> ready to ask about matches
 *
 * JavaSteam is event based: Steam's answers arrive as callbacks. A dedicated thread (see
 * {@link #run()}) waits for them and also reconnects when Steam drops the connection, which it
 * does now and then. SmartLifecycle lets Spring start that thread when the app has started and
 * stop it cleanly when the app shuts down.
 */
@Component
@ConditionalOnProperty(prefix = "cs2holdem.locator", name = "enabled", havingValue = "true")
public class GameCoordinatorClient implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GameCoordinatorClient.class);

    private static final int CS2_APP_ID = 730;
    private static final Duration RECONNECT_DELAY = Duration.ofSeconds(30);
    private static final Duration HELLO_INTERVAL = Duration.ofSeconds(5);
    private static final Duration ANSWER_TIMEOUT = Duration.ofSeconds(30);

    private final LocatorProperties properties;
    private final SteamClient steamClient = new SteamClient();
    private final CallbackManager callbacks = new CallbackManager(steamClient);
    private final SteamUser steamUser = steamClient.getHandler(SteamUser.class);
    private final SteamGameCoordinator gameCoordinator = steamClient.getHandler(SteamGameCoordinator.class);

    // "volatile": these are written by the Steam thread and read by the scheduler thread.
    // Without it, one thread might never see the other's change.
    private volatile boolean running;
    private volatile boolean connecting;
    private volatile boolean loggedOn;
    private volatile boolean ready;
    private volatile Instant nextConnectAt = Instant.EPOCH;
    private volatile Instant nextHelloAt = Instant.EPOCH;
    private volatile Pending pending;
    private volatile int hellosSent;
    private Thread thread;

    /** The question we are waiting for an answer to. We ask one at a time. */
    private record Pending(long matchId, CompletableFuture<Optional<String>> answer) {
    }

    public GameCoordinatorClient(LocatorProperties properties) {
        if (isBlank(properties.username()) || isBlank(properties.password())) {
            throw new IllegalStateException(
                    "LOCATOR_ENABLED=true needs STEAM_BOT_USERNAME and STEAM_BOT_PASSWORD in .env");
        }
        this.properties = properties;

        callbacks.subscribe(ConnectedCallback.class, this::onConnected);
        callbacks.subscribe(DisconnectedCallback.class, this::onDisconnected);
        callbacks.subscribe(LoggedOnCallback.class, this::onLoggedOn);
        callbacks.subscribe(LoggedOffCallback.class, this::onLoggedOff);
        callbacks.subscribe(MessageCallback.class, this::onGcMessage);
    }

    /** True when the GC has said welcome and can answer questions. */
    public boolean isReady() {
        return ready;
    }

    /**
     * Asks the GC where the demo of a match is. The three numbers come from the match's share code.
     *
     * @return the download link, or empty when Valve no longer has the demo (they keep them a few weeks)
     * @throws TimeoutException when the GC does not answer in time; worth trying again later
     */
    public synchronized Optional<String> findDemoUrl(long matchId, long outcomeId, int token)
            throws TimeoutException {
        if (!ready) {
            throw new TimeoutException("The Game Coordinator is not ready");
        }
        Pending request = new Pending(matchId, new CompletableFuture<>());
        pending = request;

        var message = new ClientGCMsgProtobuf<CMsgGCCStrike15_v2_MatchListRequestFullGameInfo.Builder>(
                CMsgGCCStrike15_v2_MatchListRequestFullGameInfo.class,
                ECsgoGCMsg.k_EMsgGCCStrike15_v2_MatchListRequestFullGameInfo_VALUE);
        message.getBody()
                .setMatchid(matchId)
                .setOutcomeid(outcomeId)
                .setToken(token);
        gameCoordinator.send(message, CS2_APP_ID);

        try {
            return request.answer().get(ANSWER_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TimeoutException("Interrupted while waiting for the Game Coordinator");
        } catch (ExecutionException e) {
            throw new TimeoutException("Lost the connection while waiting: " + e.getCause().getMessage());
        } finally {
            pending = null;
        }
    }

    // ---- The Steam thread ----

    private void run() {
        while (running) {
            Instant now = Instant.now();
            if (!steamClient.isConnected() && !connecting && now.isAfter(nextConnectAt)) {
                connecting = true;
                log.info("Connecting to Steam");
                steamClient.connect();
            }
            // Waits up to one second for an answer from Steam and runs the matching on... method.
            callbacks.runWaitCallbacks(1000L);

            // The GC sometimes misses our first hello (it may still be starting for us), so we
            // repeat it until it says welcome. This is what the real CS2 client does too.
            if (loggedOn && !ready && Instant.now().isAfter(nextHelloAt)) {
                sayHello();
            }
        }
    }

    private void onConnected(ConnectedCallback callback) {
        connecting = false;
        Optional<String> refreshToken = readRefreshToken();
        if (refreshToken.isPresent()) {
            logOn(refreshToken.get());
        } else {
            logInWithPassword();
        }
    }

    /**
     * The first time: log in with username and password. Steam then asks for a Steam Guard code
     * (by e-mail, or in the Steam app). UserConsoleAuthenticator asks for it in the console, which
     * in IntelliJ is the Run window: type the code there and press Enter.
     *
     * Steam answers with a refresh token. We save it, so every later start logs in with the token
     * instead, without password or code.
     */
    private void logInWithPassword() {
        log.warn("First login for the Steam bot. If Steam asks for a Steam Guard code, "
                + "type it in this console and press Enter.");
        var details = new AuthSessionDetails();
        details.username = properties.username();
        details.password = properties.password();
        details.persistentSession = true;
        details.authenticator = new UserConsoleAuthenticator();

        try {
            steamClient.getAuthentication()
                    .beginAuthSessionViaCredentials(details)
                    .thenCompose(GameCoordinatorClient::waitForSteamGuard)
                    .thenAccept(this::onPasswordLoginAccepted)
                    .exceptionally(error -> {
                        // Wrong password, wrong Steam Guard code, or Steam said no
                        log.error("Steam login for the bot failed: {}", error.getMessage());
                        steamClient.disconnect();
                        return null;
                    });
        } catch (AuthenticationException e) {
            // Thrown at once, before anything is sent, for example when Steam does not know the account
            log.error("Steam login for the bot failed: {}", e.getMessage());
            steamClient.disconnect();
        }
    }

    /**
     * Waits until Steam accepts the login, after the Steam Guard code has been typed.
     *
     * pollingWaitForResult() declares a checked exception, and a lambda inside thenCompose may not
     * throw checked exceptions (Function.apply() does not declare any). So we catch it here and
     * wrap it in CompletionException, the unchecked exception CompletableFuture uses for "this
     * step failed". The .exceptionally(...) further down then receives it like any other error.
     */
    private static CompletableFuture<AuthPollResult> waitForSteamGuard(AuthSession session) {
        try {
            return session.pollingWaitForResult();
        } catch (AuthenticationException e) {
            throw new CompletionException(e);
        }
    }

    private void onPasswordLoginAccepted(AuthPollResult result) {
        saveRefreshToken(result.getRefreshToken());
        logOn(result.getRefreshToken());
    }

    private void logOn(String refreshToken) {
        var details = new LogOnDetails();
        details.setUsername(properties.username());
        details.setAccessToken(refreshToken); // JavaSteam calls it access token, but it is the refresh token
        details.setShouldRememberPassword(true);
        steamUser.logOn(details);
    }

    private void onLoggedOn(LoggedOnCallback callback) {
        if (callback.getResult() != EResult.OK) {
            log.error("Steam refused the bot's login: {}", callback.getResult());
            // The saved token may have expired or been revoked. Forget it, so the next attempt
            // logs in with the password again.
            deleteRefreshToken();
            return;
        }
        log.info("The Steam bot is logged in. Starting CS2 for the Game Coordinator");
        loggedOn = true;
        playCs2();
        sayHello();
    }

    /** Tells Steam the bot is playing CS2. Without it, the CS2 Game Coordinator ignores us. */
    private void playCs2() {
        var playing = new ClientMsgProtobuf<CMsgClientGamesPlayed.Builder>(
                CMsgClientGamesPlayed.class, EMsg.ClientGamesPlayed);
        playing.getBody().addGamesPlayed(CMsgClientGamesPlayed.GamePlayed.newBuilder().setGameId(CS2_APP_ID));
        steamClient.send(playing);
    }

    private void sayHello() {
        nextHelloAt = Instant.now().plus(HELLO_INTERVAL);
        hellosSent++;
        // Every sixth hello (about every 30 seconds): tell the log we are still waiting, so a
        // silent Game Coordinator is visible instead of looking like a frozen app.
        if (hellosSent % 6 == 0) {
            log.warn("Still no answer from the CS2 Game Coordinator after {} hellos. "
                    + "Does the bot account have CS2 in its library?", hellosSent);
        }
        var hello = new ClientGCMsgProtobuf<CMsgClientHello.Builder>(
                CMsgClientHello.class, EGCBaseClientMsg.k_EMsgGCClientHello_VALUE);
        // The CS2 GC refuses a hello without a client version (ClientLogonFatalError 9187).
        // Any CS2-era version is accepted; it is a setting so it can be raised without a rebuild.
        hello.getBody().setVersion(properties.clientVersion());
        gameCoordinator.send(hello, CS2_APP_ID);
    }

    private void onGcMessage(MessageCallback callback) {
        int type = callback.getEMsg();
        log.debug("Game Coordinator message {} from app {}", type, callback.getAppID());
        if (callback.getAppID() != CS2_APP_ID) {
            return;
        }
        if (type == EGCBaseClientMsg.k_EMsgGCClientWelcome_VALUE) {
            if (!ready) {
                log.info("The CS2 Game Coordinator is ready (after {} hellos)", hellosSent);
            }
            hellosSent = 0;
            ready = true;
        } else if (type == EGCBaseClientMsg.k_EMsgGCClientConnectionStatus_VALUE) {
            // The GC lost track of us (for example after a Valve update). Say hello again.
            log.info("The CS2 Game Coordinator dropped the session, saying hello again");
            ready = false;
        } else if (type == ECsgoGCMsg.k_EMsgGCCStrike15_v2_ClientLogonFatalError_VALUE) {
            // The GC refuses this account, and says why. Typical reasons: the account is
            // limited, banned from matchmaking, or has not accepted something in the CS2 client.
            var error = new ClientGCMsgProtobuf<CMsgGCCStrike15_v2_ClientLogonFatalError.Builder>(
                    CMsgGCCStrike15_v2_ClientLogonFatalError.class, callback.getMessage()).getBody();
            log.error("The CS2 Game Coordinator refused the bot account: error {}, \"{}\"",
                    error.getErrorcode(), error.getMessage());
        } else if (type == ECsgoGCMsg.k_EMsgGCCStrike15_v2_MatchList_VALUE) {
            var message = new ClientGCMsgProtobuf<CMsgGCCStrike15_v2_MatchList.Builder>(
                    CMsgGCCStrike15_v2_MatchList.class, callback.getMessage());
            onMatchList(message.getBody().build());
        }
    }

    private void onMatchList(CMsgGCCStrike15_v2_MatchList list) {
        Pending request = pending;
        if (request == null) {
            return; // an answer nobody is waiting for, for example after a timeout
        }
        if (list.getMatchesCount() == 0) {
            // The GC answers with an empty list when it no longer knows the match
            request.answer().complete(Optional.empty());
            return;
        }
        for (CDataGCCStrike15_v2_MatchInfo match : list.getMatchesList()) {
            if (match.getMatchid() == request.matchId()) {
                request.answer().complete(DemoUrls.from(match));
                return;
            }
        }
    }

    private void onLoggedOff(LoggedOffCallback callback) {
        log.warn("Steam logged the bot off: {}", callback.getResult());
        loggedOn = false;
        ready = false;
    }

    private void onDisconnected(DisconnectedCallback callback) {
        connecting = false;
        loggedOn = false;
        ready = false;
        Pending request = pending;
        if (request != null) {
            request.answer().completeExceptionally(new IllegalStateException("Disconnected from Steam"));
        }
        if (running) {
            log.info("Disconnected from Steam, reconnecting in {}s", RECONNECT_DELAY.toSeconds());
            nextConnectAt = Instant.now().plus(RECONNECT_DELAY);
        }
    }

    // ---- The refresh token on disk ----

    private Optional<String> readRefreshToken() {
        try {
            if (Files.exists(properties.tokenFile())) {
                String token = Files.readString(properties.tokenFile()).strip();
                return token.isEmpty() ? Optional.empty() : Optional.of(token);
            }
        } catch (IOException e) {
            log.warn("Could not read the Steam bot token: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private void saveRefreshToken(String token) {
        try {
            Files.createDirectories(properties.tokenFile().toAbsolutePath().getParent());
            Files.writeString(properties.tokenFile(), token);
            log.info("Saved the Steam bot's login token to {}", properties.tokenFile());
        } catch (IOException e) {
            log.warn("Could not save the Steam bot token, it will need the password next time: {}",
                    e.getMessage());
        }
    }

    private void deleteRefreshToken() {
        try {
            Files.deleteIfExists(properties.tokenFile());
        } catch (IOException e) {
            log.warn("Could not delete the Steam bot token: {}", e.getMessage());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // ---- SmartLifecycle: Spring starts and stops us ----

    @Override
    public void start() {
        running = true;
        thread = Thread.ofPlatform().name("steam-gc").daemon().start(this::run);
    }

    @Override
    public void stop() {
        running = false;
        ready = false;
        if (loggedOn) {
            steamUser.logOff();
        }
        steamClient.disconnect();
        if (thread != null) {
            try {
                thread.join(Duration.ofSeconds(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
