$(document).ready(function() {
	
	$('select[selectedValue]').each(function() {
	    var value = $(this).attr('selectedValue');
	    var delimiter = ';';
	    if ($(this).attr('multiple')) {
	        if (!_.isUndefined($(this).attr('multipleDelimiter'))) {
	            delimiter = $(this).attr('multipleDelimiter');
	        }
	        var valueArray = value.split(delimiter);
	        var selectObj = $(this);
	
	        $(valueArray).each(function(i, text) {
	            if (!text) {
	                return false;
	            }
	            selectObj.find('option[value="' + text + '"]').prop('selected', true);
	        });
	    } else {
	        if (value !== '') {
	            $(this).find('option[value="' + value + '"]').prop('selected', true);
	        }
	    }
	});
	//check box check
	$('input[type="checkbox"]').each(function() {
	    var value = $(this).attr('checkedValue');
	
	    if (!value || value === '' || value === '0' || value === 'false') {
	    } else {
	        // multi loop 
	        if ($(this).attr('mode') === 'multi') {
	            var tempArray = value.split(';');
	            var targetObj = $(this);
	            $.each(tempArray, function(i, value2) {
	                if (targetObj.val() === value2) {
	                    targetObj.prop('checked', true);
	                    return false;
	                }
	            });
	        } else if ($(this).attr('mode') === 'onOff') {
	            if (value === 'ON' || value === 'on') {
	                $(this).prop('checked', true);
	            }
	        } else if ($(this).attr('mode') === 'include') {
	            if (value.indexOf($(this).val()) >= 0) {
	                $(this).prop('checked', true);
	            }
	        } else if ($(this).attr('mode') === 'multiPlus') {
	            var tempArray = value.split('+');
	            var targetObj = $(this);
	            $.each(tempArray, function(i, value2) {
	                if (targetObj.val() === value2) {
	                    targetObj.prop('checked', true);
	                    return false;
	                }
	            });
	        } else if ($(this).attr('mode') === 'bit') {
	            if (value & $(this).val()) {
	                $(this).prop('checked', true);
	            }
	        } else {
	            $(this).prop('checked', true);
	        }
	
	
	    }
	});
	//radio selected
	$('input[type="radio"]').each(function() {
	    var value = $(this).attr('selectedValue');
	
	    if (!value || value === '') {
	    } else {
	        if (value === $(this).val()) {
	            $(this).prop('checked', true);
	        }
	    }
	});
});
	    
    
if (typeof String.prototype.startsWith != 'function') {
    String.prototype.startsWith = function (str) {
        return this.substring(0, str.length) === str;
    }
};
if (typeof String.prototype.endsWith != 'function') {
    String.prototype.endsWith = function (str) {
        return this.substring(this.length - str.length, this.length) === str;
    }
};
/**
 * url 주소의 GET변수를 가져온다.
 * 
 * @param {string} url의 get parameter 키값
 * @returns {array|string} parameter 값이 존재하면 value 를 반환하고 아니면 전체 array 를 반환한다. 
 */
location.map = function(parameter) {
    var search = this.search.replace(/^\?/, ''),
            pair, key, value,
            rtn = {};
    search = search.split('&');
    for (var i = 0; i < search.length; i += 1) {
        pair = search[i].split('=');
        key = pair[0];
        value = decodeURIComponent(pair[1]);
        rtn[key] = value;
    }
    ;

    return parameter ? rtn[parameter] : rtn;
};

/**
 * url 주소의 GET변수를 치환한다.
 * 
 * @param {array} {[key:value, key2:value2]}
 * @returns {string} 치환한 GET ?뒤에꺼
 */
location.replaceMap = function(parameter) {
    var search = this.search.replace(/^\?/, ''),
            pair, key, value,
            rtn = {};
    search = search.split('&');

    for (key in parameter) {
        rtn[key] = parameter[key];
    }

    for (var i = 0; i < search.length; i += 1) {
        pair = search[i].split('=');
        if (pair.length < 2) {
            continue;
        }

        key = pair[0];
        value = pair[1];

        if (!_.isUndefined(parameter[key])) {
            value = parameter[key];
        }

        rtn[key] = value;
    }
    ;

    var resultString = '?';
    for (key in rtn) {
        resultString = resultString + key + "=" + rtn[key] + '&';
    }
    resultString = resultString.slice(0, -1);
    return resultString;
};

/**
 * jQuery selector 금지 문자열을 확인해 escape문자열을 붙여준다.
 * 
 * @returns {string} escape 처리된 string
 */
String.prototype.selectorEscape = function() {
    return this.replace(/[!"#$%&'()*+,.\/:;<=>?@[\\\]^`{|}~]/g, "\\$&");
};

/** 템플릿 기본값 세팅 */
_.templateSettings = {
    interpolate: /\[\[(.+?)\]\]/gi
};




function sweetAlert(title, text, iconType) {
	var title = title || 'Are you OK?';
	var text = text || '';
    var iconType = iconType || 'info';
    
	swal({
		title: title,
        text: text,
        icon: iconType
	});
}
/**
 * @param iconType  info, success, warning, error
 */
function sweetConfirm(title, text, iconType, okCallback, cancelCallback) {
	var title = title || 'Are you sure?';
	var text = text || '';
    var iconType = iconType || 'info';
    
    if (typeof(okCallback) != 'function') {
    	okCallback = false;
    }
    if (typeof(cancelCallback) != 'function') {
    	cancelCallback = false;
    }
    
	swal({
		title: title,
        text: text,
        icon: iconType,
        buttons: {
            cancel: {
                text: "Cancel",
                value: null,
                visible: !0,
                className: "btn btn-default",
                closeModal: !0
            },
            confirm: {
                text: "Confirm",
                value: !0,
                visible: !0,
                className: "btn btn-primary",
                closeModal: !0
            }
        }
    }).then((result) => {
	     if (result) {
    	 	okCallback();
     	 } else {
			cancelCallback();
		 }
	});
}

