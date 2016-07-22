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
                read: blankButton
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

    Matches.prototype.validateResults = function(currentMatch) {
        var ROUNDS_TO_PLAY = 3;
        var results = currentMatch.results;

        var numPlayed = 0;
        var ranksTaken = [];
        results.forEach(function(result){
            if(result.beenPlayed === true){
                var thisRanking = result.newRanking;
                var isRankTaken = false;
                for (var i = 0; i < ranksTaken.length; i++) {
                    if (ranksTaken[i] === thisRanking){
                        isRankTaken = true;
                    }
                }
                if(!isRankTaken){
                    ranksTaken.push(thisRanking);
                    numPlayed++;
                }
            }
        });

        var isValid = ROUNDS_TO_PLAY === numPlayed;
        currentMatch.resultsValid = isValid;
    };

    Matches.prototype.saveModalChanges = function (gameSession, index) {
        var match = this.matchlist[index];
        var results = match.results;
        var api = new API();
        api.inputMatchResults(gameSession, match.id, results, function() {
            this.refreshMatches();
        }.bind(this));
        this.closeModal();
    };

    Matches.prototype.changeMode = function () {
        if (this.mode === 'read') {
            this.mode = 'edit';
        }
        else {
            this.mode = 'read';
        }
    };

    Matches.prototype.updateMatches = function(matchData) {
        this.matches = matchData;
        this.matches.forEach(function(match, matchIndex) {
            var thisMatch = this.matches[matchIndex];
            var thisVue = this;
            thisMatch.pairs.forEach(function (pair, pairIndex) {
                thisVue.$watch("matches[" + matchIndex + "].results[" + pairIndex + "]", thisVue.validateResults.bind(thisVue, match));
            });
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
