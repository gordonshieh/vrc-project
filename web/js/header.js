var Header = (function() {
    "use strict";

    function Header(titleText, buttonText, lastModified, editOnClick) {
        var blankButton = Vue.extend({
            template: "<a></a>"
        });

        this.component = new Vue({
            el: '#header',
            data: {
                title: titleText,
                timestamp: lastModified,
                buttonTitle: buttonText,
                mode: "read"
            },
            components: {
                read: blankButton,
                edit: blankButton,
                loading: VueSpinner.ClipLoader
            },
            methods: {
                onClick: function() {
                    if (this.mode == "read") {
                        this.mode = "edit";
                        editOnClick();
                    }
                    else {
                        this.mode = "read";
                        editOnClick();
                    }
                }
            }
        });
    }

    // Replaces the 'edit' component with buttonComponent
    // buttonComponent must define the 'parent' property as the header component
    Header.prototype.addButton = function(buttonComponent) {
        this.component.$options.components.edit = buttonComponent;
    };

    Header.prototype.updateHeader = function(lastModified) {
        this.timestamp = lastModified;
    };

    return Header;
})();
