/**
 * 수정모드일때 ajax로 값 넣기
 * @param url
 * @returns
 */
function prepareEdit(url) {
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