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

<link href="/files/plugins/parsley/src/parsley.css" rel="stylesheet" />
<script src="/files/plugins/parsley/dist/parsley.js"></script>

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
            <h1 class="page-header">Column Information & Setting <small class="hidden-xs">header small text goes here...</small></h1>
        </div>
        
        <!-- end vertical-box-column -->
        <!-- begin vertical-box-column -->
        <div class="vertical-box-column bg-white">
            <!-- begin vertical-box -->
            <div class="vertical-box">
                <!-- begin wrapper -->
                <div class="wrapper bg-silver">
                    <span class="">
                        <a href="#" onclick="location='/columnInfo/list/${table.id}'" class="btn btn-white btn-sm"><i class="fas fa-arrow-left f-s-14 m-r-3 m-r-xs-0 t-plus-1"></i> <span class="hidden-xs">List</span></a>
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
                                    <input type="hidden" id="id" name="id" value="${columnId}">
                                    </#if>
                                    <div class="form-group row m-b-15">
                                        <label class="col-form-label col-sm-3 ">Table Name</label>
                                        <div class="col-sm-9">
                                            <p>${table.name}</p>
                                        </div>
                                    </div>
                                    
                                    <div class="form-group row m-b-15">
                                        <label class="col-form-label col-sm-3 ">Column name</label>
                                        <div class="col-sm-9">
                                            <input type="text" id="name" name="name" class="form-control m-b-5" placeholder="Enter column name" required minlength="2" <#if mode == 'edit'>disabled="disabled"</#if>>
                                        </div>
                                    </div>
                                    <div class="form-group row m-b-15">
                                        <label class="col-form-label col-sm-3 ">Column alias</label>
                                        <div class="col-sm-9">
                                            <input type="text" id="alias" name="alias" class="form-control m-b-5" placeholder="Enter column alias" required minlength="2">
                                        </div>
                                    </div>
                                    <div class="form-group row m-b-15">
                                        <label class="col-form-label col-sm-3 ">Filed Type</label>
                                        <div class="col-sm-9">
                                            <select id="logFieldType" name="logFieldType" class="form-control" <#if mode == 'edit'>disabled="disabled"</#if>>
                                                <option value="TEXT">TEXT</option>
                                                <option value="STRING">STRING</option>
                                                <option value="INT">INT</option>
                                                <option value="LONG">LONG</option>
                                                <option value="DOUBLE">DOUBLE</option>
                                                <option value="FLOAT">FLOAT</option>
                                                <option value="IP">IP</option>
                                                <option value="COUNTRY">COUNTRY</option>
                                                <option value="DATETIME">DATETIME</option>
                                            </select>
                                        </div>
                                    </div>
                                    <div class="form-group row m-b-15">
                                        <label class="col-form-label col-sm-3 ">Search</label>
                                        <div class="col-sm-9">
                                            <div class="checkbox checkbox-css">
                                                <input type="checkbox" id="search" name="search" value="true" checked/>
                                                <label for="search">Enable</label>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="form-group row m-b-15">
                                        <label class="col-form-label col-sm-3 ">Statistic</label>
                                        <div class="col-sm-9">
                                            <div class="checkbox checkbox-css">
                                                <input type="checkbox" id="statistics" name="statistics" value="true" checked />
                                                <label for="statistics">Enable</label>
                                            </div>
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
var ID = '${table.id}';
var COLUMN_ID = '${columnId?default(0)}';
$(document).ready(function() {

    prepareEdit('/restapi/columnInfo/' + ID + '/edit/' + COLUMN_ID);

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
            var url = '/restapi/columnInfo/' + ID + '/del/' + COLUMN_ID;
            $.post(url, function(json) {
                if (json) {
                    location.href='/columnInfo/list/' + ID;
                }
            });
        });
    });
    
    
    <#-- 저장 -->
    $("#form1").on('submit', function( event ) {
        var url = '/restapi/columnInfo/' + ID + '/add';
        if (MODE == 'edit') {
            url = '/restapi/columnInfo/' + ID + '/edit/' + COLUMN_ID;
        }
        var data = $('#form1').serialize();
        $.post(url, data, function(data) {
            location.href='/columnInfo/list/' + ID;
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
