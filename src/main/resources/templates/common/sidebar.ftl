<!-- begin #sidebar -->
    <div id="sidebar" class="sidebar">
        <!-- begin sidebar scrollbar -->
        <div data-scrollbar="true" data-height="100%">
            <!-- begin sidebar user -->
            <ul class="nav">
                <li class="nav-profile">
                    <a href="javascript:;" data-toggle="nav-profile">
                        <div class="cover with-shadow"></div>
                        <div class="image">
                            <img src="/files/img/user/user-13.jpg" alt="" />
                        </div>
                        <div class="info">
                            <b class="caret pull-right"></b>
                            Sean Ngu
                            <small>Front end developer</small>
                        </div>
                    </a>
                </li>
                <li>
                    <ul class="nav nav-profile">
                        <li><a href="javascript:;"><i class="fa fa-cog"></i>UI Settings</a></li>
                        <li><a href="javascript:;"><i class="fa fa-pencil-alt"></i> Contact us</a></li>
                        <li><a href="javascript:;"><i class="fa fa-question-circle"></i> Helps</a></li>
                    </ul>
                </li>
            </ul>
            <!-- end sidebar user -->
            <!-- begin sidebar nav -->
            <ul class="nav">
                <li class="nav-header">Navigation</li>
                <li class="has-sub">
                    <a href="javascript:;">
                        <b class="caret"></b>
                        <i class="fa fa-th-large"></i>
                        <span>Dashboard</span>
                    </a>
                    <ul class="sub-menu">
                        <li class="active"><a href="index.html">Dashboard v1</a></li>
                        <li><a href="index_v2.html">Dashboard v2</a></li>
                    </ul>
                </li>
                <li class="<#if depth1 == "search">active</#if>">
                    <a href="/search/list">
                        <i class="fa fa-search"></i>
                        <span>Search
                    </a>
                </li>
                <li class="<#if depth1 == "realtime">active</#if>">
                    <a href="/realtime/list">
                        <i class="fa fa-search"></i>
                        <span>Realtime log
                    </a>
                </li>
                <li class="has-sub <#if depth1 == "tableInfo" || depth1 == "columnInfo" || depth1 == "scriptInfo" || depth1 == "parserInfo">active</#if>">
                    <a href="javascript:;">
                        <b class="caret"></b>
                        <i class="fa fa-list-ol"></i>
                        <span>Settings <span class="label label-theme m-l-5">NEW</span></span> 
                    </a>
                    <ul class="sub-menu">
                        <li class="<#if depth1 == "tableInfo">active</#if>"><a href="/tableInfo/list">table <i class="fa fa-paper-plane text-theme m-l-5"></i></a></li>
                        <li class="<#if depth1 == "columnInfo">active</#if>"><a href="/columnInfo/list/1">column <i class="fa fa-paper-plane text-theme m-l-5"></i></a></li>
                        <li class="<#if depth1 == "scriptInfo">active</#if>"><a href="/scriptInfo/list/1">script</a></li>
                        <li class="<#if depth1 == "parserInfo">active</#if>"><a href="/parserInfo/list/1">parser</a></li>
                    </ul>
                </li>
                <li class="has-sub">
                    <a href="javascript:;">
                        <b class="caret"></b>
                        <i class="fa fa-align-left"></i> 
                        <span>Menu Level</span>
                    </a>
                    <ul class="sub-menu">
                        <li class="has-sub">
                            <a href="javascript:;">
                                <b class="caret"></b>
                                Menu 1.1
                            </a>
                            <ul class="sub-menu">
                                <li class="has-sub">
                                    <a href="javascript:;">
                                        <b class="caret"></b>
                                        Menu 2.1
                                    </a>
                                    <ul class="sub-menu">
                                        <li><a href="javascript:;">Menu 3.1</a></li>
                                        <li><a href="javascript:;">Menu 3.2</a></li>
                                    </ul>
                                </li>
                                <li><a href="javascript:;">Menu 2.2</a></li>
                                <li><a href="javascript:;">Menu 2.3</a></li>
                            </ul>
                        </li>
                        <li><a href="javascript:;">Menu 1.2</a></li>
                        <li><a href="javascript:;">Menu 1.3</a></li>
                    </ul>
                </li>
                <!-- begin sidebar minify button -->
                <li><a href="javascript:;" class="sidebar-minify-btn" data-click="sidebar-minify"><i class="fa fa-angle-double-left"></i></a></li>
                <!-- end sidebar minify button -->
            </ul>
            <!-- end sidebar nav -->
        </div>
        <!-- end sidebar scrollbar -->
    </div>
    <div class="sidebar-bg"></div>
    <!-- end #sidebar -->