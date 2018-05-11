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
</style>
        
<!-- begin #content -->
<div id="content" class="content content-full-width inbox" style="">
   
    <!-- begin vertical-box -->
    <div class="vertical-box with-grid" style="table-layout:initial; padding-top:70px;">
    
        <div class="hidden-xs" style="position:absolute; width:100%; top:0px; display: initial;">
            <ol class="breadcrumb pull-right p-t-20 hidden-md hidden-sm hidden-xs">
                <li class="breadcrumb-item"><a href="javascript:;">Home</a></li>
                <li class="breadcrumb-item"><a href="javascript:;">UI Elements</a></li>
                <li class="breadcrumb-item active">Column Information & Setting</li>
            </ol>
            <h1 class="page-header">Parser Information & Setting <small class="hidden-xs">header small text goes here...</small></h1>
        </div>
    
        <!-- begin vertical-box-column -->
        <div class="vertical-box-column bg-white">
            <!-- begin vertical-box -->
            <div class="vertical-box">
                <!-- begin wrapper -->
                <div class="wrapper bg-silver-lighter">
                    <!-- begin btn-toolbar -->
                    <div class="btn-toolbar">
                        <div class="btn-group m-r-5">
                            <a href="javascript:;" class="p-t-5 pull-left m-r-3 m-t-2" data-click="email-select-all">
                                <i class="far fa-square fa-fw text-muted f-s-16 l-minus-2"></i>
                            </a>
                        </div>
                        <span class="btn-group m-r-5">
                            <select id="tableList" class="form-control form-control-sm m-r-10" selectedValue="${tableId}">
                                <#list tableList as data>
                                <option value="${data.id}">${data.name}</option>
                                </#list>
                            </select>
                        </span>
                            
                        <!-- begin btn-group -->
                        <div class="btn-group ml-auto">
                            <a id="addBtn" class="btn btn-white btn-sm"><i class="fa fa fa-pencil-alt f-s-14 m-r-3 m-r-xs-0 t-plus-1"></i> <span class="hidden-xs">Add</span></a>
                        </div>
                        <!-- end btn-group -->
                    </div>
                    <!-- end btn-toolbar -->
                </div>
                <!-- end wrapper -->
                <!-- begin vertical-box-row -->
                <div class="vertical-box-row">
                    <!-- begin vertical-box-cell -->
                    <div class="vertical-box-cell">
                        <!-- begin vertical-box-inner-cell -->
                        <div class="vertical-box-inner-cell">
                            <!-- begin scrollbar -->
                            <div data-scrollbar="true" data-height="100%">
                                <!-- begin list-email -->
                                <div class="table-responsive">
                                    <table class="table table-hover m-b-0 text-inverse">
                                        <thead>
                                            <tr>
                                                <th style="width:40px">#</th>
                                                <th class="text-nowrap" style="">ip</th>
                                                <th style="width:200px">scriptName</th>
                                                <th class="text-nowrap" style="">encoding</th>
                                                <th class="text-nowrap">description</th>
                                                <th style="width:80px"></th>
                                            </tr>
                                        </thead>
                                        <tbody id="listContent">
                                        </tbody>
                                        <tfoot id="template" style="display:none;">
                                            <tr key="[[ id ]]">
                                                <td>[[ id ]]</td>
                                                <td><a href="/parserInfo/edit/${tableId}/[[ scriptInfo.id ]]/[[ id ]]">[[ ip ]]</a></td>
                                                <td>[[ scriptInfo.name ]]</td>
                                                <td>[[ encoding ]] [[ scriptInfo.name ]]</td>
                                                <td>[[ description ]]</td>
                                                <td class="with-btn" nowrap="">
                                                    <a href="/parserInfo/edit/${tableId}/[[ scriptInfo.id ]]/[[ id ]]" class="btn btn-sm btn-primary width-60 m-r-2">Edit</a>
                                                    <a class="btn btn-sm btn-white width-60 deleteBtn">Delete</a>
                                                </td>
                                            </tr>
                                        </tfoot>
                                    </table>
                                </div>
                                <!-- end list-email -->
                            </div>
                            <!-- end scrollbar -->
                        </div>
                        <!-- end vertical-box-inner-cell -->
                    </div>
                    <!-- end vertical-box-cell -->
                </div>
                <!-- end vertical-box-row -->
                <!-- begin wrapper -->
                <div class="wrapper bg-silver-lighter clearfix">
                    <div class="btn-group pull-right">
                        <button class="btn btn-white btn-sm">
                            <i class="fa fa-chevron-left f-s-14 t-plus-1"></i>
                        </button>
                        <button class="btn btn-white btn-sm">
                            <i class="fa fa-chevron-right f-s-14 t-plus-1"></i>
                        </button>
                    </div>
                    <div class="m-t-5 text-inverse f-w-600">1,232 messages</div>
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
var TEMPLATE = $('#template').html();
var TABLE_ID = '${tableId}';

$(document).ready(function() {
    
    getTableList();
    $('#tableList').on('change', function() {
        TABLE_ID = $(this).val();
        getTableList(TABLE_ID);
    });
    
    <#-- 추가 -->
    $('#addBtn').on('click', function() {
        var url = "/parserInfo/add/" + TABLE_ID;
        location.href=url;
    });
    
    <#-- 삭제 -->
    $('#listContent').on('click', '.deleteBtn', function() {
        var id = $(this).closest('tr').attr('key');
        var url = '/restapi/parserInfo/' + TABLE_ID + '/del/' + id;
        var returnUrl = '/parserInfo/list/' + TABLE_ID;
        deleteConfirm('Are you sure?', '삭제후에는 복원이 불가능합니다', url);
    });
});

function getTableList(tableId) {
    var tableId = tableId || ${tableId};
    var url = '/restapi/parserInfo/' + tableId + '/list';
    
    $.get(url, function(json) {
        $('#listContent').html('');
        $.each(json, function(key, obj) {
            var compiled = _.template(TEMPLATE);
            var html = compiled(obj);
            $('#listContent').append(html);
        });
    });
}
</script>
<@lib.theme />
<@lib.footer /> 
