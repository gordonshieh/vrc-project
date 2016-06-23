var API = (function () {
    "use strict";

    var SERVER_URL = "http://localhost:8000/api";

    function API() {
    }

    API.prototype.getLadder = function (callback) {
        $.ajax({
            method: "GET",
            url: SERVER_URL + "/index"
        })
            .done(function (response) {
                callback(JSON.parse(response));
            });
    };

    // newStatus must be "playing" or "not playing"
    API.prototype.updatePairStatus = function (pairId, newStatus, callback) {
        $.ajax({
            method: "PATCH",
            url: SERVER_URL + "/" + pairId + "/" + newStatus
        })
            .done(function (response) {
                if (callback) {
                    callback(JSON.parse(response));
                }
            });
    };

    API.prototype.addPair = function (pairId, position, callback) {
        $.ajax({
            method: "POST",
            url: SERVER_URL + "/index/add",
            data: {
                "id": pairId,
                "Position": position
            }
        })
            .done(function (response) {
                if (callback) {
                    callback(JSON.parse(response));
                }
            });
    };
    // beginning of Sam's attempts (halp)
    API.prototype.removePair = function (pairId, callback) {
        $.ajax({
            method: "DELETE",
            url: SERVER_URL + "/index/" + pairId,
            data: {
                "id": pairId
            }
        })
            .done(function (response) {
                if (callback) {
                    callback(JSON.parse(response));
                }
            });
    };

    API.prototype.addPenalty = function (pairId, penalty, callback) {
        $.ajax({
            method: "PATCH",
            url: SERVER_URL + "/matches/" + pairId,
            data: {
                "id": pairId,
                "Penalty": penalty
            }
        })
            .done(function (response) {
                if (callback) {
                    callback(JSON.parse(response));
                }
            });
    };

    API.prototype.getMatches = function (callback) {
        $.ajax({
            method: "GET",
            url: SERVER_URL + "/matches"
        })
            .done(function (response) {
                if (callback) {
                    callback(JSON.parse(response));
                }
            });
    };

    API.prototype.inputMatchResults = function (matchId, callback) {
        $.ajax({
            method: "PATCH",
            url: SERVER_URL + "/matches/" + matchId,
            data: {
                results: [
                    //TODO: determine how to send results to back-end (document currently says array of ints?)
                ]
            }
        })
            .done(function (response) {
                if (callback) {
                    callback(JSON.parse(response));
                }
            });
    };

    API.prototype.removePairFromMatch = function (pairId, callback) {
        $.ajax({
            method: "DELETE",
            url: SERVER_URL + "/matches/" + pairId
        })
            .done(function (response) {
                if (callback) {
                    callback(JSON.parse(response));
                }
            });
    };

    API.prototype.userLogin = function (email, password, callback) {
        $.ajax({
            method: "POST",
            url: SERVER_URL + "/login",
            data: {
                "Email": email,
                "Password": password
            }
        })
            .done(function (response) {
                if (callback) {
                    callback(JSON.parse(response));
                }
            });
    };

    API.prototype.userRegistration = function (email, password, callback) {
        $.ajax({
            method: "POST",
            url: SERVER_URL + "/login/new",
            data: {
                "Email": email,
                "Password": password
            }
        })
            .done(function (response) {
                if (callback) {
                    callback(JSON.parse(response));
                }
            });
    };

    return API;

})();
