/**
 * Created by David on 2016-06-22.
 */
(function () {
    "use strict";

    $.material.init();

    var matchData =  [];

    var matches = new Matches(matchData);

    var editFunction = function() {
        matches.changeMode.call(matches.component);
    };

    var header = new Header("Matches", "Edit Matches", "TBD", editFunction);

    var saveResultsButton = Vue.extend({
        template: '<a v-on:click="saveResults()" class="btn btn-raised btn-success header-button">Save Results</a>',
        methods: {
            saveResults: function() {
                var header = this.$parent;
                header.mode = "loading";
                var api = new API();
                var doneCallback = function() {
                    header.mode = "edit";
                };
                var failCallback = function(response) {
                    header.mode = "edit";
                    alert(JSON.parse(response.responseText).responseBody);
                };
                api.reorderLadder(api.gameSession.LATEST, doneCallback.bind(this), failCallback.bind(this));
            }
        },
        parent: header.component
    });
    header.addButton(saveResultsButton);

    var api = new API();
    api.getMatches(api.gameSession.LATEST, function (response) {
        matches.updateMatches.call(matches.component, response);
        header.updateHeader.call(header.component, response.dateCreated);
    });
})();
