
#
#   RuleSet Arguments
#

$ ->
    extendArgumentItem = (item) ->
        item.find(".delete").click (e) ->
            item.hide("fast", () -> item.remove() )
            e.preventDefault()
    $(".container-args").each (i, item) ->
        args = $(item)
        argsList = args.find(".args-list")
        argsList.children().each (i, item) ->
            extendArgumentItem($(item))
        args.find(".args-add").click (e) ->
            newItem = $("""
            <div class="arg">
                <div class="argumentName" style="float: left; width: 50%;">
                    <input type="text" style="width: 80%;" value=""/>
                </div>
                <div class="typeName" style="float: left; width: 40%;">
                    <a href="#" class="type" data-type="string">string</a>
                </div>
                <div style="float: left; width: 10%; text-align: right;">
                    <a class="up" href="#"><span>U</span></a>
                    <a class="down" href="#"><span>D</span></a>
                    <a class="delete" href="#"><span>X</span></a>
                </div>
                <div style="clear: both;"></div>
            </div>
            """)
            extendArgumentItem(newItem)
            extendTypeSelector(newItem)
            newItem.hide().appendTo(argsList).show("fast")
            e.preventDefault()

#
#   RuleSet Results
#

$ ->
    extendResultItem = (item) ->
        item.find(".delete").click (e) ->
            item.hide("fast", () -> item.remove() )
            e.preventDefault()
    $(".container-results").each (i, item) ->
        results = $(item)
        resultsList = results.find(".results-list")
        resultsList.children().each (i, item) ->
            extendResultItem($(item))
        results.find(".results-add").click (e) ->
            newItem = $("""
            <div class="result">
                <div class="resultName" style="float: left; width: 50%;">
                    <input type="text" style="width: 80%;" value=""/>
                </div>
                <div class="typeName" style="float: left; width: 40%;">
                    <a href="#" class="type" data-type="string">string</a>
                </div>
                <div style="float: left; width: 10%; text-align: right;">
                    <a class="up" href="#"><span>U</span></a>
                    <a class="down" href="#"><span>D</span></a>
                    <a class="delete" href="#"><span>X</span></a>
                </div>
                <div style="clear: both;"></div>
            </div>
            """)
            extendResultItem(newItem)
            extendTypeSelector(newItem)
            newItem.hide().appendTo(resultsList).show("fast")
            e.preventDefault()

#
#   RuleSet Rules
#

$ ->
    extendRulesItem = (item) ->
        item.find(".delete").click (e) ->
            item.hide("fast", () -> item.remove() )
            e.preventDefault()
    $(".container-rules").each (i, item) ->
        rules = $(item)
        rulesList = rules.find(".rules-list")
        rulesList.children().each (i, item) ->
            extendRulesItem($(item))
        rules.find(".rules-add").click (e) ->
            newItem = $("""
            <div style="margin-bottom: 10px; background-color: #FFF;border: 2px solid #FFF; border-radius: 2px; /*box-shadow: 0 1px 1px 1px #A0A0A0;*/">
                <table class="item" style="width: 100%;">
                    <tr>
                        <th width="2%"><div class="fieldHeader">N</div></th>
                        <td width="98%">
                            <table width="100%">
                                <tr>
                                    <td>
                                        <div class="fieldFrame">
                                            <input class="ruleName" type="text" value="" />
                                        </div>
                                    </td>
                                    <td style="padding: 11px;">
                                        <a class="delete" href="#"><span>X</span></a>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <th><div class="fieldHeader">?</div></th>
                        <td><div class="fieldFrame">
                            <textarea  class="ruleCondition" style="width: 100%; height: 160px;"></textarea>
                        </div></td>
                    </tr>
                    <tr>
                        <th><div class="fieldHeader">=</div></th>
                        <td><div class="fieldFrame">
                            <textarea class="ruleBody" style="width: 100%; height: 160px;"></textarea>
                        </div></td>
                    </tr>
                </table>
                </div>
            """)
            extendRulesItem(newItem)
            newItem.hide().appendTo(rulesList).show("fast")
            e.preventDefault()


#
#   RuleSet Save
#

$ ->
    $(".save-button").click (e) ->
        recordId = $(".recordId").text()
        name = $("INPUT.rulesName").val()
        sequence = $("INPUT.rulesSequence").val()
        csrfToken = $("INPUT[name=csrfToken]").val()
        json = """{ "seq": "#{sequence}", "name": "#{name}", "arguments": [ """
        args = []
        $(".args-list").find(".arg").each (i, item) ->
            name = $(item).find(".argumentName INPUT").val()
            type = $(item).find(".typeName A").data("type")
            args.push(""" { "name": "#{name}", "type": "#{type}" } """)
        json += args.join(", ")
        json += """ ], "results": [ """
        results = []
        $(".results-list").find(".result").each (i, item) ->
            name = $(item).find(".resultName INPUT").val()
            type = $(item).find(".typeName A").data("type")
            results.push(""" { "name": "#{name}", "type": "#{type}" } """)
        json += results.join(", ")
        json += """ ], "rules": [ """
        rules = []
        $(".rules-list").find(".item").each (i, item) ->
            name = JSON.stringify($(item).find(".ruleName").val())
            condition = JSON.stringify($(item).find(".ruleCondition").val())
            body = JSON.stringify($(item).find(".ruleBody").val())
            rules.push(""" { "name": #{name}, "condition": #{condition}, "body": #{body} } """)
        json += rules.join(", ")
        json += " ] } "
        $.ajax("/private/service/rules/" + recordId + "/save", {
            data : json,
            contentType : 'application/json',
            headers: {
              "Csrf-Token": csrfToken
            },
            type : 'POST',
            success: () ->
                window.location = "/private";
        })
        e.preventDefault()
