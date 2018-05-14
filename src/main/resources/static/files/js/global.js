/**
 * ajax로 목록 그리기
 * @param template
 * @param targetObj
 * @param url
 * @returns
 */
function getListAjaxTemplate(template, targetObj, url) {
    if (!template) {
        alert('no template');
        return false;
    }
    var compiled = _.template(template);
    targetObj.html('');
    $.get(url, function(json) {
    	 if (json.length > 0) {
	    	$('tfoot.noList').hide();
	        $.each(json, function(key, obj) {
	            var html = compiled(obj);
	            targetObj.append(html);
	        });
    	 } else {
	    	$('tfoot.noList').show();
    	 }
    });
   
}

/**
 * 수정모드일때 ajax로 값 넣기
 * @param url
 * @returns
 */
function initSettingData(url) {
    if (MODE == 'edit' && url != '') {
        $.get(url, function(json) {
            $.each(json, function(key, value) {
                var obj = $('#' + key);
                if (obj.hasClass('codeEditor')) {
                    editor.setValue(value);
                } else if (obj.attr('type') == 'checkbox') {
                    if (!value) {
                        obj.prop('checked', false);
                    }
                } else {
                    obj.val(value);
                }
            });
        });
    }
}

function initSettingFormBind() {
	$('#save').on('click', function() {
        $('#form1').submit();
    });
    $('#cancel').on('click', function() {
        location.reload();
    });
}

function deleteConfirm(text, text2, deleteUrl, returnUrl) {
	var returnUrl = returnUrl || location.href;
    sweetConfirm(text, text2, 'warning', function() {
        $.post(deleteUrl, function(json) {
            if (json) {
                location.href=returnUrl;
            }
        });
    });
}