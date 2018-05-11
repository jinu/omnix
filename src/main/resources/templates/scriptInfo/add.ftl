<#import "/common/lib.ftl" as lib> 
<@lib.header />
<@lib.top />
<@lib.sidebar />

<style>
@media (max-width: 767px) {
    .vertical-box.with-grid {
        padding-top: 0px !important;
    }
}

.cm-s-rubyblue.CodeMirror { background: #112435; color: white; }
.cm-s-rubyblue div.CodeMirror-selected { background: #38566F; }
.cm-s-rubyblue .CodeMirror-line::selection, .cm-s-rubyblue .CodeMirror-line > span::selection, .cm-s-rubyblue .CodeMirror-line > span > span::selection { background: rgba(56, 86, 111, 0.99); }
.cm-s-rubyblue .CodeMirror-line::-moz-selection, .cm-s-rubyblue .CodeMirror-line > span::-moz-selection, .cm-s-rubyblue .CodeMirror-line > span > span::-moz-selection { background: rgba(56, 86, 111, 0.99); }
.cm-s-rubyblue .CodeMirror-gutters { background: #1F4661; border-right: 7px solid #3E7087; }
.cm-s-rubyblue .CodeMirror-guttermarker { color: white; }
.cm-s-rubyblue .CodeMirror-guttermarker-subtle { color: #3E7087; }
.cm-s-rubyblue .CodeMirror-linenumber { color: white; }
.cm-s-rubyblue .CodeMirror-cursor { border-left: 1px solid white; }
.cm-s-rubyblue span.cm-comment { color: #999; font-style:italic; line-height: 1em; }
.cm-s-rubyblue span.cm-atom { color: #F4C20B; }
.cm-s-rubyblue span.cm-number, .cm-s-rubyblue span.cm-attribute { color: #82C6E0; }
.cm-s-rubyblue span.cm-keyword { color: #F0F; }
.cm-s-rubyblue span.cm-string { color: #F08047; }
.cm-s-rubyblue span.cm-meta { color: #F0F; }
.cm-s-rubyblue span.cm-variable-2, .cm-s-rubyblue span.cm-tag { color: #7BD827; }
.cm-s-rubyblue span.cm-variable-3, .cm-s-rubyblue span.cm-def { color: white; }
.cm-s-rubyblue span.cm-bracket { color: #F0F; }
.cm-s-rubyblue span.cm-link { color: #F4C20B; }
.cm-s-rubyblue span.CodeMirror-matchingbracket { color:#F0F !important; }
.cm-s-rubyblue span.cm-builtin, .cm-s-rubyblue span.cm-special { color: #FF9D00; }
.cm-s-rubyblue span.cm-error { color: #AF2018; }
.cm-s-rubyblue .CodeMirror-activeline-background { background: #173047; }

</style>

<link href="/files/plugins/parsley/src/parsley.css" rel="stylesheet" />
<script src="/files/plugins/parsley/dist/parsley.js"></script>

<link rel="stylesheet" type="text/css" href="/files/plugins/codemirror/codemirror.css" >
<script type="text/javascript" src="/files/plugins/codemirror/codemirror.js"></script>
<script type="text/javascript" src="/files/plugins/codemirror/mode/javascript/javascript.js"></script>
<script type="text/javascript" src="/files/plugins/codemirror/addon/edit/matchbrackets.js"></script>
    

<!-- begin #content -->
<div id="content" class="content content-full-width inbox">
   
    <!-- begin vertical-box -->
    <div class="vertical-box with-grid" style="table-layout:initial; padding-top:70px;">
    
        <div class="hidden-xs" style="position:absolute; width:100%; top:0px; display: initial;">
            <ol class="breadcrumb pull-right p-t-20 hidden-md hidden-sm hidden-xs">
                <li class="breadcrumb-item"><a href="javascript:;">Home</a></li>
                <li class="breadcrumb-item"><a href="javascript:;">UI Elements</a></li>
                <li class="breadcrumb-item active">Column Information & Setting</li>
            </ol>
            <h1 class="page-header">Script Information & Setting <small class="hidden-xs">header small text goes here...</small></h1>
        </div>
        
        <!-- end vertical-box-column -->
        <!-- begin vertical-box-column -->
        <div class="vertical-box-column bg-white">
            <!-- begin vertical-box -->
            <div class="vertical-box">
                <!-- begin wrapper -->
                <div class="wrapper bg-silver">
                    <span class="">
                        <a href="#" onclick="location='/scriptInfo/list/${tableId?default(0)}'" class="btn btn-white btn-sm"><i class="fas fa-arrow-left f-s-14 m-r-3 m-r-xs-0 t-plus-1"></i> <span class="hidden-xs">List</span></a>
                    </span>
                    
                    <span class="pull-right">
                        <a id="deleteBtn" class="btn btn-white btn-sm"><i class="fa fa-times f-s-14 m-r-3 m-r-xs-0 t-plus-1"></i> <span class="hidden-xs">Delete</span></a>
                    </span>
                </div>
                <!-- end wrapper -->
                <!-- begin vertical-box-row -->
                <div class="vertical-box-row bg-white">
                    <!-- begin vertical-box-cell -->
                    <div class="vertical-box-cell">
                        <!-- begin vertical-box-inner-cell -->
                        <div class="vertical-box-inner-cell">
                            <!-- begin scrollbar -->
                            <div data-scrollbar="true" data-height="100%" class="p-15">
                                <form id="form1" class="form form-horizontal col-md-8" data-parsley-validate="true">
                                    <#if mode == 'edit'>
                                    <input type="hidden" id="id" name="id">
                                    </#if>
                                    <div class="form-group row m-b-15">
                                        <label class="col-form-label col-sm-3 ">Table ID</label>
                                        <div class="col-sm-9">
                                            ${tableId}
                                        </div>
                                    </div>
                                    
                                    <div class="form-group row m-b-15">
                                        <label class="col-form-label col-sm-3 ">Script name</label>
                                        <div class="col-sm-9">
                                            <input type="text" id="name" name="name" class="form-control m-b-5" placeholder="Enter column name" required minlength="2">
                                        </div>
                                    </div>
                                    <div class="form-group row m-b-15">
                                        <label class="col-form-label col-sm-3">Script text</label>
                                        <div class="col-sm-9">
                                            <input id="script" name="script" type="hidden" class="codeEditor"/>
                                            <textarea id="scriptText" name="scriptText"></textarea>
                                        </div>
                                    </div>
                                    
                                    <div class="form-group row m-b-15">
                                        <label class="col-form-label col-sm-3">Description</label>
                                        <div class="col-sm-9">
                                            <textarea id="description" name="description" class="form-control" rows="3" maxlength="50"></textarea>
                                        </div>
                                    </div>
                                    
                                    
                                </form>
                            </div>
                            <!-- end scrollbar -->
                        </div>
                        <!-- end vertical-box-inner-cell -->
                    </div>
                    <!-- end vertical-box-cell -->
                </div>
                <!-- end vertical-box-row -->
                <!-- begin wrapper -->
                <div class="wrapper bg-silver text-left">
                    <button type="submit" id="cancel" class="btn btn-white p-l-40 p-r-40 m-r-5">Cancel</button>
                    <button type="submit" id="save" class="btn btn-primary p-l-40 p-r-40">Send</button>
                </div>
                <!-- end wrapper -->
            </div>
            <!-- end vertical-box -->
        </div>
        <!-- end vertical-box-column -->
    </div>
    <!-- end vertical-box -->
</div>
<!-- end #content -->


<!-- begin scroll to top btn -->
<a href="javascript:;" class="btn btn-icon btn-circle btn-success btn-scroll-to-top fade" data-click="scroll-top"><i class="fa fa-angle-up"></i></a>
<!-- end scroll to top btn -->
    
<script>

var MODE = '${mode}';
var ID = '${tableId?default(0)}';
var SCRIPT_ID = '${scriptId?default(0)}';
var editor;

$(document).ready(function() {
    
    editor = CodeMirror.fromTextArea(document.getElementById("scriptText"), {
        lineNumbers: true,
        mode: "javascript",
        styleActiveLine: true,
        matchBrackets: true,
        theme: "rubyblue"
    });
    
    <#-- 수정모드일때 값 넣기 -->
    prepareEdit('/restapi/scriptInfo/' + ID + '/edit/' + SCRIPT_ID);

    $('#save').on('click', function() {
        $('#form1').submit();
    });
    
    <#-- 취소 -->
    $('#cancel').on('click', function() {
        location.reload();
    });
    
    <#-- 삭제 -->
    $('#deleteBtn').on('click', function() {
        sweetConfirm('Are you sure?', '삭제후에는 복원이 불가능합니다', 'warning', function() {
            var url = '/restapi/scriptInfo/' + ID + '/del/' + SCRIPT_ID;
            $.post(url, function(json) {
                if (json) {
                    location.href='/scriptInfo/list/' + ID;
                }
            });
        });
    });
    
    <#-- 저장 -->
    $("#form1").on('submit', function( event ) {
        var url = '/restapi/scriptInfo/' + ID + '/add';
        if (MODE == 'edit') {
            url = '/restapi/scriptInfo/' + ID + '/edit/' + SCRIPT_ID;
        }
        
        $('#script').val(editor.getValue());
        var data = $('#form1').serialize();
        
        $.post(url, data, function(data) {
            location.href='/scriptInfo/list/' + ID;
        }).fail(function() {
            sweetAlert('Error');
            return false;
        })
        return false;
    });

});

</script>

<@lib.theme />
<@lib.footer /> 
