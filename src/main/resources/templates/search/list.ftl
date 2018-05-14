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
            <h1 class="page-header">Search log <small class="hidden-xs">header small text goes here...</small></h1>
        </div>
    
        <!-- begin vertical-box-column -->
        <div class="vertical-box-column bg-white">
            <!-- begin vertical-box -->
            <div class="vertical-box">
                <!-- begin wrapper -->
                <div class="wrapper bg-silver-lighter">
                    <!-- begin btn-toolbar -->
                    <div class="btn-toolbar">
                        <div class="input-group width-md">
                            <input type="text" id="searchText" name="searchText" class="form-control" placeholder="Search query input here">
                            <div class="input-group-append">
                                <button id="searchBtn" type="button" class="btn btn-primary ">
                                    <i class="fa fa-search"></i>
                                    <span>Search</span>
                                </button>
                            </div>
                        </div>
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
                                                <th style="width:200px">로그 수신 시간</th>
                                                <th>IP</th>
                                                <th>Detail log</th>
                                            </tr>
                                        </thead>
                                        <tbody id="listContent">
                                        </tbody>
                                        <tfoot id="template" style="display:none;">
                                            <tr id="log_[[ id ]]">
                                                <td>[[ id ]]</td>
                                                <td>[[ time ]]</td>
                                                <td>[[ ip ]]</td>
                                                <td>[[ log ]]</td>
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
                    <div class="m-t-5 text-inverse f-w-600">Total <span id="totalCount">-</span></div>
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
var TABLE_ID = '1';

$(document).ready(function() {
    $('#searchBtn').on('click', function() {
        getTableList();
    });
    
});

function getTableList(tableId) {
    var tableId = tableId || TABLE_ID;
    var url = '/restapi/search/' + tableId;
    var params = {
        'jobId' : JOBID,
        'query' : $('#searchText').val()
    }
    getListAjaxTemplate(TEMPLATE, $('#listContent'), url, params);
}




</script>
<@lib.theme />
<@lib.footer /> 
