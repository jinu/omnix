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
            <h1 class="page-header">Realtime log <small class="hidden-xs">header small text goes here...</small></h1>
        </div>
    
        <!-- begin vertical-box-column -->
        <div class="vertical-box-column bg-white">
            <!-- begin vertical-box -->
            <div class="vertical-box">
                <!-- begin wrapper -->
                <div class="wrapper bg-silver-lighter">
                    <!-- begin btn-toolbar -->
                    <div class="btn-toolbar">
                        <div class="input-group">
                            <input type="text" id="filterText" name="filterText" class="form-control" placeholder="Filtering text input here">
                            <div class="input-group-append">
                                <button id="start" type="button" class="btn btn-primary ">
                                    <i class="fa fa-play start"></i>
                                    <i class="fa fa-pause stop" style="display:none"></i>
                                    <span class="start">Start</span>
                                    <span class="stop" style="display:none">Stop</span>
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
                                                <th style="width:100px">IP</th>
                                                <th>Detail log</th>
                                            </tr>
                                        </thead>
                                        <tbody id="listContent">
                                        </tbody>
                                        <tfoot id="template" style="display:none;">
                                            <tr id="log_[[ id ]]">
                                                <td>[[ id ]]</td>
                                                <td>[[ ip ]]</td>
                                                <td>[[ text ]]</td>
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
                    <div class="m-t-5 text-inverse f-w-600"></div>
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
var count = 1;
var runFlag = false;
var filter = '';

$(document).ready(function() {
    $('#start').on('click', function() {
        if (!runFlag) {
            play();
        } else {
            stop();
        }
    });
});


function stompClientHook(stompClient) {

    var compiled = _.template(TEMPLATE);
	stompClient.subscribe("/topic/realtimeLog/" + JOBID, function(data) {
		var lists = JSON.parse(data.body);
        var delay = 0;
        var length = lists.length;
        var tick = 1000 / length;
        
        $.each(lists, function(i, obj) {
        	
            setTimeout(function() {
                if (!runFlag) {
                    return;
                }
                obj.id = count;
                var log = obj.text;
                if (filter !== '') {
                    log = log.replace(new RegExp(filter, 'gi'), '<span class="highlight">' + filter + '</span>');
                }
                obj.detailLog = log;
                
                var html = compiled(obj);
                $('#listContent').prepend(html);
                count++;
                $('#log_' + (count - 100)).remove();
            
            }, delay);
            
            delay += tick;
        });
    });
    
}

function play() {
    $('.start').hide();
    $('.stop').show();
    
    runFlag = true;
    filter = $.trim($('#filterText').val());
    stompClient.send("/app/realtimeLog/register", {}, JSON.stringify({'jobId': JOBID, 'search': filter}));
    window.onbeforeunload = function (e) {
        e = e || window.event;
        stop();
     
        // For IE<8 and Firefox prior to version 4
        if (e) {
            e.returnValue = 'MONITOR_STOP_OUT';
        }
        return 'MONITOR_STOP_OUT';
    };
    
    
}

function stop() {
    $('.start').show();
    $('.stop').hide();
    
    runFlag = false;
    window.onbeforeunload = null;
    
    stompClient.send("/app/realtimeLog/unRegister", {}, JSON.stringify({'jobId': JOBID}));
}




</script>
<@lib.theme />
<@lib.footer /> 
