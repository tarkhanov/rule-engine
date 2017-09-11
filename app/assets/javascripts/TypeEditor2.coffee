

#
#   RuleSet Arguments
#

$ ->
    extendArgumentItem = (item) ->
        item.find(".delete").click (e) ->
            item.hide("fast", () -> item.remove() )
            e.preventDefault()
    $(".type-fields").each (i, item) ->
        args = $(item)
        argsList = args.find(".field-list")
        argsList.find(".field").each (i, item) ->
            extendArgumentItem($(item))
        args.find(".field-add").click (e) ->
            newItem = $("""
            <div class="field">
                <div class="fieldName"><input type="text" class="name" value="" /></div>
                <div class="fieldType"><a href="#" class="type" data-type="string">string</a></div>
                <div class="fieldActions">
                    <a href="#" class="up">U</a> <a href="#" class="down">D</a> <a href="#" class="delete"><span>R</span></a>
                </div>
                <div style="clear: left;"></div>
            </div>
            """)
            extendArgumentItem(newItem)
            extendTypeSelector(newItem)
            newItem.hide().appendTo(argsList).show("fast")
            e.preventDefault()

    $(".save-type").click (e) ->
        recordId = $(".recordId").text()
        name = $("INPUT.typeName").val()
        sequence = $("INPUT.typeSequence").val()
        csrfToken = $("INPUT[name=csrfToken]").val()
        json = """{ "seq": "#{sequence}", "name": "#{name}", "fields": [ """
        fields = []
        $(".field-list").find(".field").each (i, item) ->
            name = $(item).find(".name").val()
            type = $(item).find(".type").data("type")
            fields.push(""" { "name": "#{name}", "type": "#{type}" } """)
        json += fields.join(", ")
        json += " ] } "
        $.ajax("/private/service/types/" + recordId + "/save", {
            data : json,
            contentType : 'application/json',
            headers: {
              "Csrf-Token": csrfToken
            },
            type : 'POST',
            success: () ->
                window.location = "/private"
            error: () ->
                alert("Unable to save type")
        })
        e.preventDefault()
