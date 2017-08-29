


class @Popup
    constructor: (@root, title) ->
        @content = $("""
            <div class="popup">
                <div><h2 class="header" style="margin: 0; background-color: #f8f8f8; padding: 30px 30px 24px 30px; text-align: center; border-top-right-radius: 4px; border-top-left-radius: 4px; border-bottom: 1px solid #ccc;"></h2></div>
                <div style="padding: 2em 2em 1.8em 2em;" class="popup-content"></div>
                <div style="text-align: right; padding: 20px; background-color: #f8f8f8; border-top: 2px solid #CCC; border-bottom-right-radius: 4px; border-bottom-left-radius: 4px;">
                    <input type="submit" class="popup-apply" value="Apply" style="margin-right: 10px;" />
                    <input type="submit" class="popup-cancel" value="Cancel" />
                </div>
            </div>
            """)
        @content.css({
            "border-radius": "4px",
            "background-color": "#fff",
            "box-shadow": "0 1px 4px #000",
            "width": "630px"
        })

        pop = @content

        @content.find(".header").text(title)

        @applyButton = @content.find(".popup-apply")
        @applyButton.hide()

        @content.find(".popup-cancel").click (e) ->
            pop.hideModal()
            e.preventDefault()

        @wrapper = @content.find(".popup-content")

    withApply: (callback) ->
        @applyButton.show()
        @applyButton.click (e) ->
            callback()
            e.preventDefault()

    show: () ->
        @content.showModal()

    hide: () ->
        @content.hideModal()

    update: (val) ->
        @wrapper.html(val)


$ ->
    $(".repositoryCreate").click (e) ->
        popup = new Popup(this, "Create...")
        popup.update($("""
            <div>
                <!--a class="create-folder-link" href="/private/newFolder">Create Folder</a-->
                <a class="create-type-link" href="/private/service/types/new">Create Type</a>
                <a class="create-ruleset-link" href="/private/service/rules/new">Create Rules</a>

                <div style="clear: both"></div>
            </div>
        """))
        popup.show()
        e.preventDefault()



$ ->
    $(".pageSizeSelect").change (e) ->
        link = $(this).data("link")
        window.location = link.replace("{size}", $(this).val())
        e.preventDefault()
