var API = (function() {
    "use strict";

    var SERVER_URL = "api";
    var LADDER_ENDPOINT = "/ladder";
    var STATUS_PARAM = "?newStatus=";
    var POSITION_PARAM = "?position=";
    var PENALTY_PARAM = "&penalty=";
    var GAMESESSION_PARAM = "?gameSession=";

    function API() {
        this.playingStatus = Object.freeze({
            PLAYING: "playing",
            NOT_PLAYING: "not playing"
        });
        this.STATUS_PLAYING = "playing";
        this.STATUS_NOT_PLAYING = "not playing";
        this.penalty = Object.freeze({
            MISS: "miss",
            LATE: "late",
            ACCIDENT: "accident"
        });
        this.PENALTY_MISS = "miss";
        this.PENALTY_LATE = "late";
        this.PENALTY_ACCIDENT = "accident";
        this.gameSession = Object.freeze({
            LATEST: "latest",
            PREVIOUS: "previous"
        });
    }

    API.prototype.getLadder = function(doneCallback, failCallback) {
        $.ajax({
            method: "GET",
            url: SERVER_URL + LADDER_ENDPOINT
        })
        .done(function(response) {
            if (doneCallback) {
                var ladderData = JSON.parse(response);
                ladderData.forEach(function(pair) {
                    pair.teamName = pair.players[0].firstName + " " + pair.players[0].lastName + " and " +
                        pair.players[1].firstName + " " + pair.players[1].lastName;
                    pair.playingStatus = pair.isPlaying ? "playing" : "notplaying";
                });
                doneCallback(ladderData);
            }
        })
        .fail(function(response) {
            if (failCallback) {
                failCallback(response);
            }
            else {
                var responseBody = JSON.parse(response.responseText);
                alert(responseBody.message);
            }
        });
    };

    // newStatus must be "playing" or "not playing"
    API.prototype.updatePairStatus = function(pairId, newStatus, doneCallback, failCallback) {
        $.ajax({
            method: "PATCH",
            url: SERVER_URL + LADDER_ENDPOINT + "/" + pairId + STATUS_PARAM + newStatus
        })
        .done(function(response) {
            if (doneCallback) {
                doneCallback(JSON.parse(response));
            }
        })
        .fail(function(response) {
            if (failCallback) {
                failCallback(response);
            }
            else {
                var responseBody = JSON.parse(response.responseText);
                alert(responseBody.message);
            }
        });
    };

    API.prototype.updatePairPosition = function(pairId, newPosition, doneCallback, failCallback) {
        $.ajax({
            method: "PATCH",
            url: SERVER_URL + LADDER_ENDPOINT + "/" + pairId + POSITION_PARAM + newPosition
        })
        .done(function(response) {
            if (doneCallback) {
                doneCallback(JSON.parse(response));
            }
        })
        .fail(function(response) {
            if (failCallback) {
                failCallback(response);
            }
            else {
                var responseBody = JSON.parse(response.responseText);
                alert(responseBody.message);
            }
        });
    };

    API.prototype.prepareNewPlayer = function(firstName, lastName, phoneNumber) {
        var onlyDigitsRegex = /\d/g;
        var sanitizedPhoneNumber = "";
        if (phoneNumber !== "") {
          sanitizedPhoneNumber = phoneNumber.match(onlyDigitsRegex).join("");
        }
        return {
            "firstName": firstName,
            "lastName": lastName,
            "phoneNumber": sanitizedPhoneNumber,
            "existingId": -1
        };
    };

    API.prototype.prepareExistingPlayer = function(playerId) {
        return {
            "existingId": playerId
        };
    };

    // player1 and player2 must be created using the prepare methods above
    API.prototype.addPair = function(players, position, doneCallback, failCallback) {
        $.ajax({
            method: "POST",
            url: SERVER_URL + "/ladder",
            data: JSON.stringify({
                players: players,
                "position": position
            })
        })
        .done(function(response) {
            if (doneCallback) {
                doneCallback(JSON.parse(response));
            }
        })
        .fail(function(response) {
            if (failCallback) {
                failCallback(response);
            }
            else {
                var responseBody = JSON.parse(response.responseText);
                alert(responseBody.message);
            }
        });
    };

    API.prototype.removePair = function (pairId, doneCallback, failCallback) {
        $.ajax({
            method: "DELETE",
            url: SERVER_URL + "/ladder/" + pairId
        })
        .done(function (response) {
            if (doneCallback) {
                doneCallback(JSON.parse(response));
            }
        })
        .fail(function(response) {
            if (failCallback) {
                failCallback(response);
            }
            else {
                var responseBody = JSON.parse(response.responseText);
                alert(responseBody.message);
            }
        });
    };

    // Valid penalties are "late", "miss" or "accident"
    API.prototype.addPenalty = function (gameSession, pairId, penalty, doneCallback, failCallback) {
        $.ajax({
            method: "POST",
            url: SERVER_URL + "/matches/" + pairId + GAMESESSION_PARAM + gameSession +
                 PENALTY_PARAM + penalty
        })
        .done(function (response) {
            if (doneCallback) {
                doneCallback(JSON.parse(response));
            }
        })
        .fail(function(response) {
            if (failCallback) {
                failCallback(response);
            }
            else {
                var responseBody = JSON.parse(response.responseText);
                alert(responseBody.message);
            }
        });
    };

    API.prototype.reorderLadder = function (gameSession, doneCallback, failCallback) {
        $.ajax({
            method: "POST",
            url: SERVER_URL + "/matches" + GAMESESSION_PARAM + gameSession
        })
        .done(function (response) {
            if (doneCallback) {
                doneCallback(response);
            }
        })
        .fail(function (response) {
            if (failCallback) {
                failCallback(response);
            }
            else {
                var responseBody = JSON.parse(response.responseText);
                alert(responseBody.message);
            }
        });
    };

    API.prototype.getMatches = function (gameSession, doneCallback, failCallback) {
        $.ajax({
            method: "GET",
            url: SERVER_URL + "/matches" + GAMESESSION_PARAM + gameSession
        })
        .done(function (response) {
            var matches = JSON.parse(response);
            matches.forEach(function(match, i) {
                match.scorecardIndex = i;
                match.mode = "read";
                match.resultsValid = false;
                match.results = [];
                match.pairs.forEach(function(pair) {
                    var resultsArr = [];
                    for (var i in match.pairs) {
                        resultsArr.push("-");
                    }
                    match.results.push(resultsArr);

                    pair.absentPenalty = {
                        'btn-raised': false
                    };
                    pair.latePenalty = {
                        'btn-raised': false
                    };
                });
            });
            if (doneCallback) {
                doneCallback(matches);
            }
        })
        .fail(function(response) {
            if (failCallback) {
                failCallback(response);
            }
            else {
                var responseBody = JSON.parse(response.responseText);
                alert(responseBody.message);
            }
        });
    };

    API.prototype.inputMatchResults = function (gameSession, matchId, results, doneCallback, failCallback) {
        $.ajax({
            method: "PATCH",
            url: SERVER_URL + "/matches/" + matchId + GAMESESSION_PARAM + gameSession,
            data: JSON.stringify({
                results: results
            })
        })
        .done(function (response) {
            if (doneCallback) {
                doneCallback(JSON.parse(response));
            }
        })
        .fail(function(response) {
            if (failCallback) {
                failCallback(response);
            }
            else {
                var responseBody = JSON.parse(response.responseText);
                alert(responseBody.message);
            }
        });
    };

    API.prototype.removePairFromMatch = function (pairId, doneCallback, failCallback) {
        $.ajax({
            method: "DELETE",
            url: SERVER_URL + "/matches/" + pairId
        })
        .done(function (response) {
            if (doneCallback) {
                doneCallback(JSON.parse(response));
            }
        })
        .fail(function(response) {
            if (failCallback) {
                failCallback(response);
            }
            else {
                var responseBody = JSON.parse(response.responseText);
                alert(responseBody.message);
            }
        });
    };

    API.prototype.userLogin = function (email, password, doneCallback, failCallback) {
        $.ajax({
            method: "POST",
            url: SERVER_URL + "/login",
            data: JSON.stringify({
                "email": email,
                "password": password
            })
        })
        .done(function (response) {
            if (doneCallback) {
                doneCallback(JSON.parse(response));
            }
        })
        .fail(function(response) {
            if (failCallback) {
                failCallback(response);
            }
            else {
                var responseBody = JSON.parse(response.responseText);
                alert(responseBody.message);
            }
        });
    };

    API.prototype.userRegistration = function (email, password, doneCallback, failCallback) {
        $.ajax({
            method: "POST",
            url: SERVER_URL + "/login/new",
            data: JSON.stringify({
                "email": email,
                "password": password
            })
        })
        .done(function (response) {
            if (doneCallback) {
                doneCallback(JSON.parse(response));
            }
        })
        .fail(function(response) {
            if (failCallback) {
                failCallback(response);
            }
            else {
                var responseBody = JSON.parse(response.responseText);
                alert(responseBody.message);
            }
        });
    };

    API.prototype.userLogout = function (doneCallback, failCallback) {
        $.ajax({
            method: "POST",
            url: SERVER_URL + "/logout",
        })
            .done(function (response) {
                if (doneCallback) {
                    doneCallback(JSON.parse(response));
                }
            })
            .fail(function(response) {
                if (failCallback) {
                    failCallback(response);
                }
            });
    };

    return API;

})();
