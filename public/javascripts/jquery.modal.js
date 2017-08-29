(function($){

    function close_modal(overlay, modal){

    	overlay.fadeOut(200);
        modal.css({ 'display' : 'none' });
 	}

    $.fn.extend({ 
         
        showModal: function(options) {

            var defaults = {
                top: "10%",
                overlay: 0.5
            }

            options =  $.extend(defaults, options);

            var overlay = $("#modal_overlay");
            if (overlay.size() == 0) {
                overlay = $("<div id='modal_overlay'></div>");
                overlay.css({
                    'position': 'fixed',
                    'z-index': '100',
                    'top': '0px',
                    'left': '0px',
                    'height':'100%',
                    'width':'100%',
                    'background': '#000',
                    'display': 'none',
                });
                $("body").append(overlay);
            }

            this.each(function() {

                var modal = $(this);
                var modal_height = modal.outerHeight();
                var modal_width = modal.outerWidth();

                overlay.css({ 'display' : 'block', opacity : 0 });
                overlay.fadeTo(200, options.overlay);
                overlay.click(function() {
                     close_modal($(this), modal);
                });

                modal.css({
                    'display' : 'block',
                    'position' : 'fixed',
                    'opacity' : 0,
                    'z-index': 11000,
                    'left' : 50 + '%',
                    'margin-left' : - (modal_width / 2) + "px",
                    'top' : options.top
                });

                $("body").append(modal);

                modal.fadeTo(200,1);
            });

        },

        hideModal: function() {

            close_modal($("#modal_overlay"), $(this))
        }

    });
     
})(jQuery);