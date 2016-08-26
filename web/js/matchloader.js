/**
 * Created by David on 2016-06-22.
 */
(function () {
    "use strict";

    $.material.init();

    var matchData =  [];

    var matches = new Matches(matchData);

    var editFunction = function() {
        matches.changeMode();
    };

    var reorderLadderButton = Vue.extend({
        template: "#reorderLadderButtonTemplate",
        data: function() {
            return {
                spinnerVisible: false,
                disabled: !matches.isAllDone()
            };
        },
        methods: {
            saveResults: function() {
                var header = this.$parent;
                this.spinnerVisible = true;
                var api = new API();
                var doneCallback = function() {
                    header.mode = "edit";
                    window.location.href = "/ladder";
                };
                var failCallback = function(response) {
                    header.mode = "edit";
                    alert(JSON.parse(response.responseText).responseBody);
                };
                api.reorderLadder(matches.component.activeGameSession, doneCallback.bind(this), failCallback.bind(this));
            }
        },
                                             components: {
                                                 'ClipLoader': VueSpinner.ClipLoader
        }
    });
    Vue.component('reorder-ladder-button', reorderLadderButton);

    var userRole = Cookies.get("userRole");

    var navbar = new Navbar(userRole);

    var header = new Header("Groups", "Edit Groups", "TBD", editFunction, userRole);

    var api = new API();
    api.getMatches(api.gameSession.LATEST, function (response) {
        matches.updateMatches(response, api.gameSession.LATEST);
        header.updateHeader.call(header.component, response.dateCreated);
    });

    api.getMatches(api.gameSession.PREVIOUS, function (response) {
        matches.updateMatches(response, api.gameSession.PREVIOUS);
    });
})();
