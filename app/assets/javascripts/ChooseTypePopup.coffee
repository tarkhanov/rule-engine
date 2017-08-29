

@chooseTypePopup = (currentType, onChange) ->
    popup = new Popup(this, "Type...")
    container = $("""<div class="typeDefRoot"></div>""")
    root = $("<div></div>")
    root.append(container)
    popup.update(root)

    availableTypes = {}

    drawSelector = (currentType, container) ->
        typeSelect = $("""<select></select>""")
        for key of availableTypes
            typeOption = $("""<option value="#{availableTypes[key]}">#{key}</option> """)
            typeOption.appendTo(typeSelect)
        $("""<option value="list">list</option> """).appendTo(typeSelect)
        $("""<option value="sequence">sequence</option> """).appendTo(typeSelect)
        $("""<option value="id">id</option> """).appendTo(typeSelect)
        typeSelect.appendTo(container)
        param = $("""<span class="param"></span>""")
        param.appendTo(container)

        matchList = /^list\[(.+)\]$/.exec currentType
        matchSeq = /^sequence:(.+)$/.exec currentType
        matchId = /^id:(.+)$/.exec currentType
        if ( matchList != null )
            listType = matchList[1]
            typeSelect.val("list")
            drawList(listType, param)
        else if ( matchSeq != null )
            seqType = matchSeq[1]
            typeSelect.val("sequence")
            drawSequence(seqType, param)
        else if ( matchId != null )
            idType = matchId[1]
            typeSelect.val("id")
            drawId(idType, param)
        else
            typeSelect.val(currentType)

        typeSelect.change () ->
            t = $(this).val()
            param.empty()
            if (t == "list")
                drawList("string", param)
            else if (t == "sequence")
                drawSequence("", param)
            else if (t == "id")
                drawId("", param)

    drawSequence = (seqType, container) ->
        list = $(""" <span>:<input type="text" value="#{seqType}" style="width: 140px; margin: 0 8px;" /></span> """)
        #param = list.find("INPUT")
        list.appendTo(container)

    drawId = (idType, container) ->
        list = $(""" <span>:<input type="text" value="#{idType}" style="width: 140px; margin: 0 8px;" /></span> """)
        #param = list.find("INPUT")
        list.appendTo(container)

    drawList = (listType, container) ->
        list = $(""" <span>[<span class="listParam"></span>]</span> """)
        param = list.find(".listParam")
        drawSelector(listType, param)
        list.appendTo(container)

    $.getJSON("/private/service/types/available.ajax?list=builtin", (data) ->
          container.empty()
          $.each( data, ( i, item ) -> availableTypes[item.name] = item.type )
          drawSelector(currentType, container)
    )

    readType = (select) ->
        selectValue = select.val()
        if (selectValue == "list")
            paramSelect = select.parent().children(".param").children().children().children("select")
            "list[" + readType(paramSelect) + "]"
        else if (selectValue == "sequence")
            input = select.parent().children(".param").children().children("INPUT").val()
            "sequence:" + input
        else if (selectValue == "id")
            input = select.parent().children(".param").children().children("INPUT").val()
            "id:" + input
        else
            selectValue

    popup.withApply () ->
        select = container.children("SELECT")
        type = readType(select)
        onChange(type, type)
        popup.hide()

    popup

@extendTypeSelector = (root) ->
    root.find("A.type").click (e) ->
         row = $(this)
         popup = chooseTypePopup(row.data("type"), (type, name) ->
             row.data("type", type)
             row.text(name)
         )
         popup.show()
         e.preventDefault()

$ ->
    extendTypeSelector($("BODY"))
