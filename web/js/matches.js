/**
 * Created by David on 2016-06-15.
 */

var EDIT_BUTTON_HTML = '<a v-on:click="editMatch()" class="edit-button btn btn-info btn-fab btn-fab-mini"><i class="material-icons md-light">create</i></a>';

var Matches = (function () {
    function Matches(matchData) {
        Vue.component('matches', {
            template: '#matches-template',
            props: {
                active: "active",
                isActive: "isActive",
                matchlist: "matchlist",
                show: {
                    type: Boolean,
                    required: true,
                    twoWay: true
                }
            },
            methods: {
                modalActiveContent: function (i) {
                    return this.active === i;
                },
                closeModal: this.closeModal,
                applyPenalty: this.applyPenalty,
                saveChanges: this.saveModalChanges,
                refreshMatches: this.refreshMatches
            }
        });

        var editButton;
        var blankButton;
        editButton = Vue.extend({
            props: ['column','index'],
            template: EDIT_BUTTON_HTML,
            methods: {
                editMatch: function() {
                    this.$parent.openModal(this.index);
                }
             }
        });
        Vue.component('edit-button', editButton);

        blankButton = Vue.extend({
            template: "<a></a>"
        });

        this.component = new Vue({
            el: '#matches',
            data: {
                active: 0,
                showModal: false,
                matches: matchData,
                mode: 'read'
            },
            methods: {
                openModal: this.openModal,
                updateMatches: this.updateMatches,
                validateResults: this.validateResults
            },
            components: {
                edit: editButton,
                read: blankButton,
                loading: VueSpinner.ClipLoader
            }
        });
    }

    Matches.prototype.openModal = function(index) {
        this.showModal = true;
        this.active = index;
        return this.active;
    };

    Matches.prototype.closeModal = function() {
        this.show = false;
        this.active = false;
    };

    Matches.prototype.applyPenalty = function(gameSession, pair, penaltyType, event) {
        if (penaltyType === "late") {
            pair.latePenalty = {
                'btn-raised': true
            };
        }
        else if (penaltyType === "miss") {
            pair.absentPenalty = {
                'btn-raised': true
            };
        }
        var api = new API();
        api.addPenalty(gameSession, pair.id, penaltyType, function(response) {
            // $(event.srcElement).addClass("btn-raised");
        });
    };

    Matches.prototype.validateResults = function(currentMatch, newVal, oldVal) {
        var numRounds = newVal.length;
        var CORRECT_ROUNDS_PLAYED = 2;
        var CORRECT_ROUNDS_NOT_PLAYED = numRounds - CORRECT_ROUNDS_PLAYED;

        var isValid = newVal.every(function(pairRecord) {
            var roundsNotPlayed = pairRecord.filter(function(entry) {
                return entry === "-";
            }).length;
            return roundsNotPlayed === CORRECT_ROUNDS_NOT_PLAYED;
        });

        currentMatch.resultsValid = isValid;
    };

    Matches.prototype.saveModalChanges = function (gameSession, index) {
        var match = this.matchlist[index];
        var results = [];

        match.mode = "loading";

        for (var round = 0; round < match.pairs.length; round++) {
            var roundResult = [];
            for (var pair in match.pairs) {
                roundResult.push(match.results[pair][round]);
            }
            results.push(roundResult);
        }

        var api = new API();
        api.inputMatchResults(gameSession, match.id, results, function() {
            this.refreshMatches();
        }.bind(this));
        this.closeModal();
    };

    Matches.prototype.changeMode = function () {
        if (this.mode === 'read') {
            this.mode = 'edit';
            this.matches.forEach(function (match) {
                if (match.mode !== "loading") {
                    match.mode = "edit";
                }
            });
        }
        else {
            this.mode = 'read';
            this.matches.forEach(function (match) {
                if (match.mode !== "loading") {
                    match.mode = "read";
                }
            });
        }
    };

    Matches.prototype.updateMatches = function(matchData) {
        this.matches = matchData;
        this.matches.forEach(function(match, index) {
            this.$watch("matches[" + index + "].results", this.validateResults.bind(this, match));
        }.bind(this));
    };

    Matches.prototype.refreshMatches = function() {
        var api = new API();
        api.getMatches(api.gameSession.LATEST, function(matchData) {
            this.$parent.updateMatches.call(this.$parent, matchData);
        }.bind(this));
    };

    return Matches;
})();
