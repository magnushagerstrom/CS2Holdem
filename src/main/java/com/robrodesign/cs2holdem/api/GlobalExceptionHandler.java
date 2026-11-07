package com.robrodesign.cs2holdem.api;

import com.robrodesign.cs2holdem.account.AccountAlreadyExistsException;
import com.robrodesign.cs2holdem.account.AccountNotFoundException;
import com.robrodesign.cs2holdem.match.MatchNotFoundException;
import com.robrodesign.cs2holdem.pipeline.DemoRejectedException;
import com.robrodesign.cs2holdem.season.InvalidHandException;
import com.robrodesign.cs2holdem.season.SeasonLockedException;
import com.robrodesign.cs2holdem.season.SeasonNotFoundException;
import com.robrodesign.cs2holdem.steam.InvalidShareCodeException;
import com.robrodesign.cs2holdem.steam.InvalidSteamCredentialsException;
import com.robrodesign.cs2holdem.steam.SteamUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.TreeMap;

/**
 * Gör om våra exceptions till rätt HTTP-svar, på ETT ställe.
 *
 * Svaren följer standarden "Problem Details" (RFC 9457), t.ex.:
 * { "status": 409, "title": "Account already exists", "detail": "An account for SteamID ... already exists" }
 *
 * Genom att ärva ResponseEntityExceptionHandler får vi samma format även för Springs egna fel,
 * t.ex. när @Valid underkänner en request.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Körs när @Valid underkänner en request. Standardsvaret säger bara "Invalid request content",
     * så vi lägger till exakt vilket fält som var fel och varför:
     * "errors": { "authCode": "must look like XXXX-XXXXX-XXXX", ... }
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new TreeMap<>(); // TreeMap = sorterat på fältnamn
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed",
                "One or more fields are invalid.");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(AccountAlreadyExistsException.class)
    ProblemDetail handleAccountAlreadyExists(AccountAlreadyExistsException e) {
        return problem(HttpStatus.CONFLICT, "Account already exists", e.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail handleAccountNotFound(AccountNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Account not found", e.getMessage());
    }

    @ExceptionHandler(MatchNotFoundException.class)
    ProblemDetail handleMatchNotFound(MatchNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Match not found", e.getMessage());
    }

    @ExceptionHandler(SeasonNotFoundException.class)
    ProblemDetail handleSeasonNotFound(SeasonNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Season not found", e.getMessage());
    }

    @ExceptionHandler(SeasonLockedException.class)
    ProblemDetail handleSeasonLocked(SeasonLockedException e) {
        return problem(HttpStatus.CONFLICT, "Season locked", e.getMessage());
    }

    @ExceptionHandler(InvalidHandException.class)
    ProblemDetail handleInvalidHand(InvalidHandException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid hand", e.getMessage());
    }

    @ExceptionHandler(DemoRejectedException.class)
    ProblemDetail handleDemoRejected(DemoRejectedException e) {
        return problem(HttpStatus.BAD_REQUEST, "Demo rejected", e.getMessage());
    }

    @ExceptionHandler(NotYourAccountException.class)
    ProblemDetail handleNotYourAccount(NotYourAccountException e) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", e.getMessage());
    }

    @ExceptionHandler(NumberFormatException.class)
    ProblemDetail handleBadId(NumberFormatException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid id", "Ids must be numbers written as strings");
    }

    @ExceptionHandler(InvalidSteamCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidSteamCredentialsException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Steam credentials", e.getMessage());
    }

    @ExceptionHandler(InvalidShareCodeException.class)
    ProblemDetail handleInvalidShareCode(InvalidShareCodeException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid share code", e.getMessage());
    }

    @ExceptionHandler(SteamUnavailableException.class)
    ProblemDetail handleSteamUnavailable(SteamUnavailableException e) {
        log.warn("Steam call failed: {}", e.getMessage());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Steam unavailable",
                "Steam could not be reached right now. Please try again later.");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
